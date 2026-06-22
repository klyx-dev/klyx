package com.klyx.editor.treesitter

import android.os.Bundle
import android.util.Log
import androidx.annotation.WorkerThread
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.StyleReceiver
import io.github.rosemoe.sora.lang.styling.CodeBlock
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.SpanFactory
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.treesitter.ktreesitter.InputEdit
import io.github.treesitter.ktreesitter.InputEncoding
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Parser
import io.github.treesitter.ktreesitter.Point
import io.github.treesitter.ktreesitter.Query
import io.github.treesitter.ktreesitter.QueryCapture
import io.github.treesitter.ktreesitter.Tree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds

class TsAnalyzeManager(
    private val language: Language,
    private val queries: LanguageQueries,
    @Volatile private var theme: TsTheme,
    private val languageProvider: LanguageProvider
) : AnalyzeManager, OffsetMapper {

    private var receiver: StyleReceiver? = null
    private var content: ContentReference? = null

    private val tsDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val analyzeScope = CoroutineScope(tsDispatcher + SupervisorJob())
    private var parseJob: Job? = null

    private val textBuffer = StringBuilder()
    private val parser = Parser(language)

    @Volatile
    var activeTree: Tree? = null
        private set

    private val injectionParsers = HashMap<String, Parser>()
    var styles = Styles()

    fun updateTheme(theme: TsTheme) {
        this.theme = theme
        (styles.spans as? TsSpans)?.updateTheme(theme)
    }

    override fun setReceiver(receiver: StyleReceiver?) {
        this.receiver = receiver
    }

    override fun reset(content: ContentReference, extraArguments: Bundle) {
        this.content = content
        synchronized(textBuffer) {
            textBuffer.clear()
            textBuffer.append(content.reference.toString())
        }
        runAnalysis(debounceMs = 0L)
    }

    override fun charToByte(charIndex: Int): Int {
        return synchronized(textBuffer) {
            if (charIndex <= 0) return 0
            val end = charIndex.coerceAtMost(textBuffer.length)
            textBuffer.substring(0, end).encodeToByteArray().size
        }
    }

    override fun byteToChar(byteOffset: Int): Int {
        return synchronized(textBuffer) {
            if (byteOffset <= 0) return 0
            val fullString = textBuffer.toString()
            val bytes = fullString.encodeToByteArray()
            if (byteOffset >= bytes.size) return fullString.length
            String(bytes, 0, byteOffset, Charsets.UTF_8).length
        }
    }

    override fun insert(start: CharPosition, end: CharPosition, insertedContent: CharSequence) {
        val edit = createInputEdit(start, start, end)
        synchronized(textBuffer) {
            textBuffer.insert(start.index, insertedContent)
        }
        applyIncrementalEdit(edit)
    }

    override fun delete(start: CharPosition, end: CharPosition, deletedContent: CharSequence) {
        val edit = createInputEdit(start, end, start)
        synchronized(textBuffer) {
            textBuffer.delete(start.index, end.index)
        }
        applyIncrementalEdit(edit)
    }

    override fun rerun() {
        runAnalysis(debounceMs = 0L)
    }

    override fun destroy() {
        receiver = null
        content = null
        parseJob?.cancel()
        analyzeScope.cancel()

        analyzeScope.launch(tsDispatcher) {
            parser.closeSafely()
            injectionParsers.values.forEach { it.closeSafely() }
            injectionParsers.clear()
            activeTree?.closeSafely()
            activeTree = null
        }
    }

    private fun applyIncrementalEdit(edit: InputEdit) {
        activeTree?.edit(edit)
        runAnalysis()
    }

    private fun runAnalysis(debounceMs: Long = 120L) {
        parseJob?.cancel()

        parseJob = analyzeScope.launch {
            if (debounceMs > 0L) delay(debounceMs.milliseconds)
            ensureActive()

            val text = synchronized(textBuffer) { textBuffer.toString() }
            val currentLineCount = content?.lineCount ?: 0

            val documentLines = text.lines()
            val snapshotMapper = SnapshotOffsetMapper(text)

            val oldTree = activeTree?.copy()
            val newTree = parser.parse(
                source = text,
                encoding = InputEncoding.UTF_8,
                oldTree = oldTree
            )

            activeTree = newTree
            oldTree?.closeSafely()
            ensureActive()

            val scopedVariables = try {
                TsScopedVariables(newTree, StringBuilder(text), queries, snapshotMapper) {
                    !isActive
                }
            } catch (_: TsScopedVariables.AnalysisCanceledException) {
                return@launch
            }
            ensureActive()

            val activeInjections = mutableListOf<ActiveInjection>()
            val precomputedLineSpans = runCatching {
                compileDocumentSpans(
                    tree = newTree,
                    text = text,
                    documentLines = documentLines,
                    lineCount = currentLineCount,
                    scopedVariables = scopedVariables,
                    snapshotMapper = snapshotMapper,
                    outInjections = activeInjections
                )
            }.getOrNull() ?: emptyArray()

            val computedBlocks = computeCodeBlocks(
                tree = newTree,
                documentLines = documentLines,
                snapshotMapper = snapshotMapper,
                injections = activeInjections
            )
            ensureActive()

            val treeCopy = newTree.copy()
            val finalSpans = TsSpans(precomputedLineSpans, currentLineCount, theme)

            withContext(Dispatchers.Main) {
                styles.finishBuilding()
                receiver?.setStyles(this@TsAnalyzeManager, styles) {
                    styles.spans = finalSpans
                    styles.blocks = computedBlocks
                }

                receiver?.updateBracketProvider(
                    this@TsAnalyzeManager,
                    TsBracketsProvider(
                        hostTree = treeCopy,
                        mapper = snapshotMapper,
                        hostProfile = BracketProfile.forLanguage(queries.languageName),
                        injections = activeInjections
                    )
                )
            }
        }
    }

    @WorkerThread
    private fun compileDocumentSpans(
        tree: Tree,
        text: String,
        documentLines: List<String>,
        lineCount: Int,
        scopedVariables: TsScopedVariables,
        snapshotMapper: SnapshotOffsetMapper,
        outInjections: MutableList<ActiveInjection>
    ): Array<List<Span>> {
        val totalLines = documentLines.size.coerceAtMost(lineCount)
        val compiledStructure = Array<List<Span>>(totalLines) { emptyList() }
        val lineSegments = Array(totalLines) { mutableListOf<CaptureSegment>() }

        val hostCaptures = runCatching {
            queries.highlights(tree.rootNode)
                .captures()
                .map { (idx, match) -> match.captures[idx.toInt()] }
        }.getOrNull() ?: emptySequence()

        for (capture in hostCaptures) {
            try {
                processCapture(
                    capture,
                    text,
                    documentLines,
                    totalLines,
                    scopedVariables,
                    snapshotMapper,
                    lineSegments,
                    isInjection = false
                )
            } catch (e: Exception) {
                Log.w("TsAnalyzeManager", "Failed to process capture: ${e.message}")
            }
        }

        queries.injections?.let { injectionQuery ->
            try {
                val injectionCursor = injectionQuery(tree.rootNode)
                for (match in injectionCursor.matches()) {
                    var contentNode: Node? = null
                    var languageName: String? = null

                    for (capture in match.captures) {
                        when (capture.name) {
                            "injection.content" -> contentNode = capture.node
                            "injection.language" -> {
                                val start =
                                    snapshotMapper.byteToChar(capture.node.startByte.toInt())
                                val end = snapshotMapper.byteToChar(capture.node.endByte.toInt())
                                languageName = text.substring(start, end).lowercase()
                            }
                        }
                    }

                    if (languageName == null && contentNode != null) {
                        languageName = when (contentNode.parent?.type) {
                            "script_element" -> "javascript"
                            "style_element" -> "css"
                            else -> null
                        }
                    }

                    if (contentNode != null && languageName != null) {
                        val subLanguage = languageProvider.getLanguage(languageName)
                        val subQueries = languageProvider.getQueries(languageName)

                        if (subLanguage != null && subQueries != null) {
                            val contentStartChar =
                                snapshotMapper.byteToChar(contentNode.startByte.toInt())
                            val contentEndChar =
                                snapshotMapper.byteToChar(contentNode.endByte.toInt())
                            val injectionRawText = text.substring(contentStartChar, contentEndChar)

                            val subParser =
                                injectionParsers.getOrPut(languageName) { Parser(subLanguage) }
                            val subTree = subParser.parse(injectionRawText, InputEncoding.UTF_8)
                            val subTreeCopy = subTree.copy()

                            outInjections.add(
                                ActiveInjection(
                                    languageName = languageName,
                                    startChar = contentStartChar,
                                    endChar = contentEndChar,
                                    startByte = contentNode.startByte.toInt(),
                                    tree = subTreeCopy,
                                    queries = subQueries,
                                    profile = BracketProfile.forLanguage(languageName)
                                )
                            )

                            val injectionMapper = object : OffsetMapper {
                                override fun charToByte(charIndex: Int): Int = 0
                                override fun byteToChar(byteOffset: Int): Int {
                                    val globalByte = contentNode.startByte.toInt() + byteOffset
                                    return snapshotMapper.byteToChar(globalByte) - contentStartChar
                                }
                            }

                            val subScopedVars = TsScopedVariables(
                                subTree,
                                StringBuilder(injectionRawText),
                                subQueries,
                                injectionMapper
                            ) { false }

                            // captures() already in document order — no sortedBy needed
                            val subCaptures = subQueries.highlights(subTree.rootNode).captures()
                                .map { (idx, m) -> m.captures[idx.toInt()] }

                            for (subCapture in subCaptures) {
                                processInjectedCapture(
                                    subCapture,
                                    injectionRawText,
                                    documentLines,
                                    totalLines,
                                    subQueries,
                                    subScopedVars,
                                    snapshotMapper,
                                    lineSegments,
                                    contentNode.startByte.toInt(),
                                    contentNode.startPoint
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        for (row in 0 until totalLines) {
            val rowSegments = lineSegments[row]
            val rowText = documentLines[row]
            val completedRowSpans = mutableListOf<Span>()

            rowSegments.sortWith(
                compareBy<CaptureSegment> { it.startColumn }
                    .thenByDescending { it.isInjection }
                    .thenBy { it.endColumn - it.startColumn }
            )

            var scanCursor = 0
            for (segment in rowSegments) {
                if (segment.startColumn < scanCursor) continue
                if (segment.startColumn > scanCursor) {
                    completedRowSpans.add(SpanFactory.obtain(scanCursor, theme.normalTextStyle))
                }
                completedRowSpans.add(SpanFactory.obtain(segment.startColumn, segment.style))
                scanCursor = segment.endColumn
            }

            if (scanCursor < rowText.length || completedRowSpans.isEmpty()) {
                completedRowSpans.add(SpanFactory.obtain(scanCursor, theme.normalTextStyle))
            }
            compiledStructure[row] = completedRowSpans
        }

        return compiledStructure
    }

    @WorkerThread
    private fun computeCodeBlocks(
        tree: Tree,
        documentLines: List<String>,
        snapshotMapper: SnapshotOffsetMapper,
        injections: List<ActiveInjection>
    ): List<CodeBlock> {
        val structuralBlocks = mutableListOf<CodeBlock>()
        queries.folds?.let { foldsQuery ->
            processTreeFolds(
                tree,
                foldsQuery,
                documentLines,
                snapshotMapper,
                globalStartByte = 0,
                structuralBlocks
            )
        }
        for (injection in injections) {
            val subFoldsQuery = injection.queries.folds ?: continue
            processTreeFolds(
                injection.tree,
                subFoldsQuery,
                documentLines,
                snapshotMapper,
                globalStartByte = injection.startByte,
                structuralBlocks
            )
        }
        return structuralBlocks.distinct()
    }

    private fun processTreeFolds(
        targetTree: Tree,
        foldsQuery: Query,
        documentLines: List<String>,
        snapshotMapper: SnapshotOffsetMapper,
        globalStartByte: Int,
        output: MutableList<CodeBlock>
    ) {
        try {
            val cursor = foldsQuery(targetTree.rootNode)
            for ((captureIdx, match) in cursor.captures()) {
                val capture = match.captures[captureIdx.toInt()]
                if (capture.name == "fold") {
                    val node = capture.node
                    val absoluteStartByte = globalStartByte + node.startByte.toInt()
                    val absoluteEndByte = globalStartByte + node.endByte.toInt()

                    val startChar = snapshotMapper.byteToChar(absoluteStartByte)
                    val endChar = snapshotMapper.byteToChar(absoluteEndByte)

                    val (startLineRow, _) = snapshotMapper.charToLineCol(startChar)
                    val (endLineRow, endLineCol) = snapshotMapper.charToLineCol(endChar)

                    if (endLineRow - startLineRow > 1) {
                        val safeStartRow = startLineRow.coerceAtMost(documentLines.size - 1)
                        val lineText = documentLines[safeStartRow]
                        val structuralIndentColumn = lineText.takeWhile { it.isWhitespace() }.length

                        val block = CodeBlock().apply {
                            startLine = startLineRow
                            startColumn = structuralIndentColumn
                            endLine = endLineRow
                            endColumn = endLineCol
                        }
                        output.add(block)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun translateByteToCharColumn(lineText: String, targetByteColumn: Int): Int {
        if (targetByteColumn <= 0) return 0
        var byteAccumulator = 0
        var charAccumulator = 0
        while (charAccumulator < lineText.length && byteAccumulator < targetByteColumn) {
            val codePoint = lineText.codePointAt(charAccumulator)
            val stepChar = Character.charCount(codePoint)
            val stepBytes = lineText.substring(charAccumulator, charAccumulator + stepChar)
                .encodeToByteArray().size
            if (byteAccumulator + stepBytes > targetByteColumn) break
            byteAccumulator += stepBytes
            charAccumulator += stepChar
        }
        return charAccumulator
    }

    private fun processCapture(
        capture: QueryCapture,
        text: String,
        documentLines: List<String>,
        totalLines: Int,
        scopedVariables: TsScopedVariables,
        snapshotMapper: SnapshotOffsetMapper,
        lineSegments: Array<MutableList<CaptureSegment>>,
        isInjection: Boolean
    ) {
        val name = capture.name
        if (name in queries.localsScopeNames ||
            name in queries.localsDefinitionNames ||
            name in queries.localsDefinitionValueNames ||
            name in queries.localsMembersScopeNames
        ) return

        val startPoint = capture.node.startPoint
        val endPoint = capture.node.endPoint
        val startRow = startPoint.row.toInt().coerceAtMost(totalLines - 1)
        val endRow = endPoint.row.toInt().coerceAtMost(totalLines - 1)

        var activeStyle = 0L
        if (name in queries.localsReferenceNames) {
            val startChar = snapshotMapper.byteToChar(capture.node.startByte.toInt())
            val endChar = snapshotMapper.byteToChar(capture.node.endByte.toInt())
            val definition = scopedVariables.findDefinition(
                startChar.toUInt(),
                endChar.toUInt(),
                text.substring(startChar, endChar)
            )
            if (definition != null) {
                activeStyle =
                    theme.resolveStyleForCaptureName(definition.highlightCaptureName ?: name)
            }
        }
        if (activeStyle == 0L) activeStyle = theme.resolveStyleForCaptureName(name)
        if (activeStyle == 0L) activeStyle = theme.normalTextStyle

        for (row in startRow..endRow) {
            val currentLineText = documentLines[row]
            val relativeStartChar = if (row == startRow) {
                translateByteToCharColumn(currentLineText, startPoint.column.toInt())
            } else 0
            val relativeEndChar = if (row == endRow) {
                translateByteToCharColumn(currentLineText, endPoint.column.toInt())
            } else currentLineText.length

            if (relativeStartChar >= relativeEndChar) continue
            lineSegments[row].add(
                CaptureSegment(relativeStartChar, relativeEndChar, activeStyle, isInjection)
            )
        }
    }

    private fun processInjectedCapture(
        subCapture: QueryCapture,
        subText: String,
        documentLines: List<String>,
        totalLines: Int,
        subQueries: LanguageQueries,
        subScopedVars: TsScopedVariables,
        snapshotMapper: SnapshotOffsetMapper,
        lineSegments: Array<MutableList<CaptureSegment>>,
        globalStartByte: Int,
        globalStartPoint: Point
    ) {
        val name = subCapture.name
        val subStartPoint = subCapture.node.startPoint
        val subEndPoint = subCapture.node.endPoint

        val globalRowStart =
            (globalStartPoint.row.toInt() + subStartPoint.row.toInt()).coerceAtMost(totalLines - 1)
        val globalRowEnd =
            (globalStartPoint.row.toInt() + subEndPoint.row.toInt()).coerceAtMost(totalLines - 1)

        val absStartChar =
            snapshotMapper.byteToChar(globalStartByte + subCapture.node.startByte.toInt())
        val absEndChar =
            snapshotMapper.byteToChar(globalStartByte + subCapture.node.endByte.toInt())
        val (_, startColChar) = snapshotMapper.charToLineCol(absStartChar)
        val (_, endColChar) = snapshotMapper.charToLineCol(absEndChar)

        var activeStyle = 0L
        if (name in subQueries.localsReferenceNames) {
            val contentNodeStartChar = snapshotMapper.byteToChar(globalStartByte)
            val localStartChar = (absStartChar - contentNodeStartChar).toUInt()
            val localEndChar = (absEndChar - contentNodeStartChar).toUInt()
            val variableName = subText.substring(localStartChar.toInt(), localEndChar.toInt())
            val definition =
                subScopedVars.findDefinition(localStartChar, localEndChar, variableName)
            if (definition != null) {
                activeStyle =
                    theme.resolveStyleForCaptureName(definition.highlightCaptureName ?: name)
            }
        }

        if (activeStyle == 0L) activeStyle = theme.resolveStyleForCaptureName(name)
        if (activeStyle == 0L) activeStyle = theme.normalTextStyle

        for (row in globalRowStart..globalRowEnd) {
            val currentLineText = documentLines[row]
            val relativeStartChar = if (row == globalRowStart) startColChar else 0
            val relativeEndChar = if (row == globalRowEnd) endColChar else currentLineText.length

            if (relativeStartChar >= relativeEndChar) continue
            lineSegments[row].add(
                CaptureSegment(relativeStartChar, relativeEndChar, activeStyle, isInjection = true)
            )
        }
    }

    private fun createInputEdit(
        start: CharPosition,
        oldEnd: CharPosition,
        newEnd: CharPosition
    ): InputEdit {
        return newInputEdit(start = start, oldEnd = oldEnd, newEnd = newEnd, mapper = this)
    }

    private data class CaptureSegment(
        val startColumn: Int,
        val endColumn: Int,
        val style: Long,
        val isInjection: Boolean = false
    )
}

class SnapshotOffsetMapper(text: String) : OffsetMapper {
    private val lines = text.lines()
    private val lineStartChar = IntArray(lines.size)
    private val lineStartByte = IntArray(lines.size)

    init {
        var currentCharOffset = 0
        var currentByteOffset = 0
        for (i in lines.indices) {
            lineStartChar[i] = currentCharOffset
            lineStartByte[i] = currentByteOffset
            val lineStr = lines[i]

            var lineByteLen = 0
            var j = 0
            while (j < lineStr.length) {
                val cp = lineStr.codePointAt(j)
                lineByteLen += when {
                    cp <= 0x7F -> 1
                    cp <= 0x7FF -> 2
                    cp <= 0xFFFF -> 3
                    else -> 4
                }
                j += Character.charCount(cp)
            }
            currentCharOffset += lineStr.length + 1
            currentByteOffset += lineByteLen + 1
        }
    }

    fun charToLineCol(charIndex: Int): Pair<Int, Int> {
        if (charIndex <= 0) return Pair(0, 0)
        var low = 0
        var high = lineStartChar.size - 1
        var lineIdx = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lineStartChar[mid] <= charIndex) {
                lineIdx = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return Pair(lineIdx, charIndex - lineStartChar[lineIdx])
    }

    override fun charToByte(charIndex: Int): Int {
        if (charIndex <= 0) return 0
        var low = 0
        var high = lineStartChar.size - 1
        var lineIdx = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lineStartChar[mid] <= charIndex) {
                lineIdx = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        var currentByte = lineStartByte[lineIdx]
        var currentChar = lineStartChar[lineIdx]
        val lineStr = lines[lineIdx]

        var i = 0
        while (i < lineStr.length && currentChar < charIndex) {
            val cp = lineStr.codePointAt(i)
            val cLen = Character.charCount(cp)
            currentByte += when {
                cp <= 0x7F -> 1
                cp <= 0x7FF -> 2
                cp <= 0xFFFF -> 3
                else -> 4
            }
            currentChar += cLen
            i += cLen
        }
        return currentByte
    }

    override fun byteToChar(byteOffset: Int): Int {
        if (byteOffset <= 0) return 0
        var low = 0
        var high = lineStartByte.size - 1
        var lineIdx = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lineStartByte[mid] <= byteOffset) {
                lineIdx = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        var currentByte = lineStartByte[lineIdx]
        var currentChar = lineStartChar[lineIdx]
        val lineStr = lines[lineIdx]

        var i = 0
        while (i < lineStr.length && currentByte < byteOffset) {
            val cp = lineStr.codePointAt(i)
            val cLen = Character.charCount(cp)
            val bLen = when {
                cp <= 0x7F -> 1
                cp <= 0x7FF -> 2
                cp <= 0xFFFF -> 3
                else -> 4
            }
            if (currentByte + bLen > byteOffset) break
            currentByte += bLen
            currentChar += cLen
            i += cLen
        }
        return currentChar
    }
}
