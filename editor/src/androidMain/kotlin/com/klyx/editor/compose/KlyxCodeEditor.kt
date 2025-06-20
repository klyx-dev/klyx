package com.klyx.editor.compose

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.core.LocalAppSettings
import com.klyx.core.Notifier
import com.klyx.core.event.EventBus
import com.klyx.core.theme.ThemeManager
import com.klyx.editor.EditorState
import com.klyx.editor.compose.input.textInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.abs

@Composable
fun KlyxCodeEditor(
    editorState: EditorState,
    modifier: Modifier = Modifier,
    wrapText: Boolean = false,
    scrollbarThickness: Dp = 10.dp,
    fontSize: TextUnit = 18.sp,
    horizontalPadding: Dp = 4.dp,
    bottomPaddingLines: Int = 5,
    scrollbarColor: Color = Color(0x88FFFFFF),
    typeface: Typeface? = null,
    gutterWidth: Dp = 48.dp,
    showGutter: Boolean = true,
    pinnedLineNumbers: Boolean = false,
    gutterBackgroundColor: Color = Color(0xFF2B2B2B),
    gutterTextColor: Color = Color(0xFF888888),
    gutterDividerColor: Color = Color(0xFF444444),
    cursorColor: Color = Color.White,
    cursorWidth: Dp = 2.dp,
    editable: Boolean = true,
    cursorBlinkRate: Long = 500,
    cursorFocusPadding: Dp = 100.dp,
    onTextChanged: (String) -> Unit = {},
    language: String = "text"
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val settings = LocalAppSettings.current
    val notifier: Notifier = koinInject()

    val themeStyle by remember(settings) { derivedStateOf { ThemeManager.getThemeByName(settings.theme)?.style } }

    val effectiveGutterBackground = themeStyle?.gutterBackground ?: gutterBackgroundColor
    val effectiveGutterText = themeStyle?.gutterText ?: gutterTextColor
    val effectiveGutterDivider = themeStyle?.gutterDivider ?: gutterDividerColor

    val coroutineScope = rememberCoroutineScope()
    val syntaxHighlights = remember { mutableStateListOf<SyntaxHighlight>() }
    val lastHighlightedText = remember { mutableStateOf("") }
    val lastHighlightedLanguage = remember { mutableStateOf("") }

    val scrollY = remember { mutableFloatStateOf(0f) }
    val scrollX = remember { mutableFloatStateOf(0f) }
    var showCursor by remember { mutableStateOf(true) }
    var isCursorActive by remember { mutableStateOf(false) }
    var lastCursorActivityTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isCursorMoving by remember { mutableStateOf(false) }
    val lineHeightPx = with(density) { fontSize.toPx() }
    val lineSpacingPx = with(density) { 4.dp.toPx() }
    val fullLineHeightPx = lineHeightPx + lineSpacingPx
    val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }
    val gutterWidthPx = if (showGutter) with(density) { gutterWidth.toPx() } else 0f
    val scrollbarThicknessPx = with(density) { scrollbarThickness.toPx() }
    val endHorizontalPaddingPx = if (wrapText) 0f else with(density) { 50.dp.toPx() }
    val cursorFocusPaddingPx = with(density) { cursorFocusPadding.toPx() }

    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }

    val visibleLines = remember { mutableStateListOf<String>() }
    val lineOffsets = remember { mutableStateListOf<Int>() }
    val totalLines = remember { mutableStateOf(0) }

    var lastScrollUpdateTime by remember { mutableLongStateOf(0L) }
    val scrollDebounceTime = 16L // ~60fps

    var lastTextUpdateTime by remember { mutableLongStateOf(0L) }
    val textUpdateDebounceTime = 16L // ~60fps

    val allHighlights = remember { mutableStateListOf<SyntaxHighlight>() }

    fun updateVisibleLines(allLines: List<String>) {
        val startIndex = (scrollY.floatValue / fullLineHeightPx).toInt().coerceAtLeast(0)
        val visibleCount = (canvasHeight / fullLineHeightPx).toInt() + 2
        val endIndex = (startIndex + visibleCount).coerceAtMost(allLines.size)

        visibleLines.clear()
        visibleLines.addAll(allLines.subList(startIndex, endIndex))
    }

    fun updateVisibleHighlights() {
        val lines = editorState.text.lines()
        val startIndex = (scrollY.floatValue / fullLineHeightPx).toInt().coerceAtLeast(0)
        val visibleCount = (canvasHeight / fullLineHeightPx).toInt() + 2
        val bufferSize = 20 // Number of lines to process above and below visible area
        val processStart = (startIndex - bufferSize).coerceAtLeast(0)
        val processEnd = (startIndex + visibleCount + bufferSize).coerceAtMost(lines.size)

        var startCharOffset = 0
        for (i in 0 until processStart) {
            startCharOffset += lines[i].length + 1 // +1 for newline
        }

        var endCharOffset = startCharOffset
        for (i in processStart until processEnd) {
            endCharOffset += lines[i].length + 1 // +1 for newline
        }

        val filteredHighlights = allHighlights.filter { highlight ->
            highlight.startOffset < endCharOffset && highlight.endOffset > startCharOffset
        }

        syntaxHighlights.clear()
        syntaxHighlights.addAll(filteredHighlights)
    }

    val highlighter = remember { TreeSitterHighlighter(context) }

    LaunchedEffect(editorState.text, language) {
        highlighter.reparse(editorState.text)

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTextUpdateTime >= textUpdateDebounceTime) {
            if (editorState.text != lastHighlightedText.value || language != lastHighlightedLanguage.value) {
                lastTextUpdateTime = currentTime
                highlighter.setLanguage(language)

                val newHighlights = highlighter.getSyntaxHighlights(editorState.text)

                lastHighlightedText.value = editorState.text
                lastHighlightedLanguage.value = language

                allHighlights.clear()
                allHighlights.addAll(newHighlights)

                updateVisibleHighlights()
            }
        }
    }

    LaunchedEffect(scrollY.floatValue) {
        updateVisibleHighlights()
    }

    fun handleScroll(newScrollY: Float) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScrollUpdateTime >= scrollDebounceTime) {
            scrollY.floatValue = newScrollY
            lastScrollUpdateTime = currentTime

            // Only update visible lines if we've scrolled enough
            val lineDelta = (newScrollY - scrollY.floatValue) / fullLineHeightPx
            if (abs(lineDelta) >= 1) {
                updateVisibleLines(editorState.text.lines())
                updateVisibleHighlights()
            }
        }
    }

    LaunchedEffect(editorState.text) {
        val lines = editorState.text.lines()
        totalLines.value = lines.size

        lineOffsets.clear()
        var offset = 0
        lines.forEach { line ->
            lineOffsets.add(offset)
            offset += line.length + 1 // +1 for newline
        }

        updateVisibleLines(lines)
    }

    LaunchedEffect(scrollY.floatValue) {
        handleScroll(scrollY.floatValue)
    }

    // track dirty regions for partial redraws
    val dirtyRegion = remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var lastScrollY by remember { mutableFloatStateOf(0f) }
    var lastScrollX by remember { mutableFloatStateOf(0f) }

    // update dirty region when scrolling
    LaunchedEffect(scrollY.floatValue, scrollX.floatValue) {
        if (scrollY.floatValue != lastScrollY || scrollX.floatValue != lastScrollX) {
            dirtyRegion.value = Pair(scrollY.floatValue - lastScrollY, scrollX.floatValue - lastScrollX)
            lastScrollY = scrollY.floatValue
            lastScrollX = scrollX.floatValue
        }
    }

    var draggingVerticalScrollbar by remember { mutableStateOf(false) }
    var draggingHorizontalScrollbar by remember { mutableStateOf(false) }
    var verticalDragOffset by remember { mutableFloatStateOf(0f) }
    var horizontalDragOffset by remember { mutableFloatStateOf(0f) }

    val textPaint = remember(typeface, lineHeightPx) {
        Paint().apply {
            textSize = lineHeightPx
            color = Color.White.toArgb()
            isAntiAlias = true
            this.typeface = typeface
        }
    }

    val gutterPaint = remember(typeface, lineHeightPx, effectiveGutterText) {
        Paint().apply {
            textSize = lineHeightPx * 0.85f
            color = effectiveGutterText.toArgb()
            isAntiAlias = true
            this.typeface = typeface
            textAlign = Paint.Align.RIGHT
        }
    }

    // cache for wrapped lines with size limit
    val wrappedLinesCache = remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    val maxCacheSize = 1000 // maximum number of cached lines

    fun getWrappedLines(text: String, paint: Paint, wrapWidth: Float): List<String> {
        // Check cache first
        val cached = wrappedLinesCache.value
        if (cached != null && cached.first == text) {
            return cached.second
        }

        // For large files, only wrap visible lines
        val lines = if (text.lines().size > maxCacheSize) {
            val startIndex = (scrollY.floatValue / fullLineHeightPx).toInt().coerceAtLeast(0)
            val visibleCount = (canvasHeight / fullLineHeightPx).toInt() + 2
            val endIndex = (startIndex + visibleCount).coerceAtMost(text.lines().size)

            val visibleLines = text.lines().subList(startIndex, endIndex)
            buildList {
                visibleLines.forEachIndexed { _, originalLine ->
                    if (!wrapText) {
                        add(originalLine)
                    } else {
                        if (originalLine.isEmpty()) {
                            add("")
                        } else {
                            var remaining = originalLine
                            while (remaining.isNotEmpty()) {
                                val count = paint.breakText(remaining, true, wrapWidth, null)
                                if (count == 0) {
                                    add(remaining)
                                    break
                                }
                                add(remaining.substring(0, count))
                                remaining = remaining.substring(count)
                            }
                        }
                    }
                }
            }
        } else {
            buildList {
                text.lines().forEachIndexed { _, originalLine ->
                    if (!wrapText) {
                        add(originalLine)
                    } else {
                        if (originalLine.isEmpty()) {
                            add("")
                        } else {
                            var remaining = originalLine
                            while (remaining.isNotEmpty()) {
                                val count = paint.breakText(remaining, true, wrapWidth, null)
                                if (count == 0) {
                                    add(remaining)
                                    break
                                }
                                add(remaining.substring(0, count))
                                remaining = remaining.substring(count)
                            }
                        }
                    }
                }
            }
        }

        // Update cache only for smaller files
        if (text.lines().size <= maxCacheSize) {
            wrappedLinesCache.value = text to lines
        }
        return lines
    }

    // Optimize text measurement cache for large files
    val textMeasurementCache = remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    val maxMeasurementCacheSize = 5000 // Maximum number of cached measurements

    fun measureText(text: String): Float {
        return textMeasurementCache.value[text] ?: run {
            val measurement = textPaint.measureText(text)
            if (textMeasurementCache.value.size < maxMeasurementCacheSize) {
                textMeasurementCache.value += (text to measurement)
            }
            measurement
        }
    }

    // Clear measurement cache when it gets too large
    LaunchedEffect(editorState.text) {
        if (textMeasurementCache.value.size > maxMeasurementCacheSize) {
            textMeasurementCache.value = emptyMap()
        }
    }

    fun calculateScrollLimits(
        allLines: List<String>,
        paint: Paint
    ): Pair<Float, Float> {
        val totalVisualLines = allLines.size
        val contentHeight = totalVisualLines * fullLineHeightPx + bottomPaddingLines * fullLineHeightPx
        val verticalLimit = (contentHeight - canvasHeight).coerceAtLeast(0f)
        val maxLineWidth = allLines.maxOfOrNull { paint.measureText(it) } ?: 0f
        val horizontalLimit = if (!wrapText) {
            (maxLineWidth - canvasWidth + horizontalPaddingPx + endHorizontalPaddingPx + gutterWidthPx).coerceAtLeast(0f)
        } else 0f
        return Pair(verticalLimit, horizontalLimit)
    }

    // calculate cursor position
    fun calculateCursorPosition(cursorPosition: Int, lines: List<String>, paint: Paint): Pair<Int, Float> {
        var currentPosition = 0
        var cursorLine = 0
        var cursorX = 0f

        for (i in lines.indices) {
            val line = lines[i]
            val lineLength = line.length + 1 // +1 for newline

            if (currentPosition + lineLength > cursorPosition) {
                cursorLine = i
                val charPosition = cursorPosition - currentPosition

                // measure text up to cursor position
                if (charPosition > 0) {
                    cursorX = measureText(line.substring(0, charPosition))
                }
                break
            }
            currentPosition += lineLength
        }

        return Pair(cursorLine, cursorX)
    }

    fun DrawScope.drawScrollbars(
        verticalLimit: Float,
        horizontalLimit: Float,
        maxLineWidth: Float,
        totalVisualLines: Int
    ) {
        if (verticalLimit > 0f) {
            val verticalThumbHeight =
                (canvasHeight / (totalVisualLines * fullLineHeightPx + bottomPaddingLines * fullLineHeightPx) * canvasHeight).coerceAtLeast(20f)
            val verticalThumbTop = (scrollY.floatValue / verticalLimit * (canvasHeight - verticalThumbHeight))
                .coerceIn(0f, canvasHeight - verticalThumbHeight)

            drawRect(
                color = scrollbarColor.copy(alpha = 0.3f),
                topLeft = Offset(canvasWidth - scrollbarThicknessPx, 0f),
                size = Size(scrollbarThicknessPx, canvasHeight)
            )
            drawRect(
                color = scrollbarColor,
                topLeft = Offset(canvasWidth - scrollbarThicknessPx, verticalThumbTop),
                size = Size(scrollbarThicknessPx, verticalThumbHeight)
            )
        }

        if (!wrapText && horizontalLimit > 0f) {
            val horizontalThumbWidth = (canvasWidth / (maxLineWidth + horizontalPaddingPx + endHorizontalPaddingPx + gutterWidthPx) * canvasWidth)
                .coerceAtLeast(20f)
            val horizontalThumbLeft = (scrollX.floatValue / horizontalLimit * (canvasWidth - horizontalThumbWidth))
                .coerceIn(0f, canvasWidth - horizontalThumbWidth)

            drawRect(
                color = scrollbarColor.copy(alpha = 0.3f),
                topLeft = Offset(0f, canvasHeight - scrollbarThicknessPx),
                size = Size(canvasWidth, scrollbarThicknessPx)
            )
            drawRect(
                color = scrollbarColor,
                topLeft = Offset(horizontalThumbLeft, canvasHeight - scrollbarThicknessPx),
                size = Size(horizontalThumbWidth, scrollbarThicknessPx)
            )
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    fun ensureCursorInView() {
        val cursorPosition = editorState.cursorPosition
        val lines = editorState.text.lines()
        var currentPosition = 0
        var cursorLine = 0
        var cursorX = 0f

        // Find cursor line and X position
        for (i in lines.indices) {
            val lineLength = lines[i].length + 1
            if (currentPosition + lineLength > cursorPosition) {
                cursorLine = i
                val line = lines[i]
                val charPosition = cursorPosition - currentPosition
                cursorX = measureText(line.substring(0, charPosition))
                break
            }
            currentPosition += lineLength
        }

        val cursorY = cursorLine * fullLineHeightPx
        val cursorBottom = cursorY + fullLineHeightPx

        if (cursorY < scrollY.floatValue + cursorFocusPaddingPx) {
            scrollY.floatValue = (cursorY - cursorFocusPaddingPx).coerceAtLeast(0f)
        } else if (cursorBottom > scrollY.floatValue + canvasHeight - cursorFocusPaddingPx) {
            scrollY.floatValue = (cursorBottom - canvasHeight + cursorFocusPaddingPx).coerceAtLeast(0f)
        }

        if (!wrapText) {
            val cursorRight = gutterWidthPx + horizontalPaddingPx + cursorX
            if (cursorX < scrollX.floatValue + cursorFocusPaddingPx) {
                scrollX.floatValue = (cursorX - cursorFocusPaddingPx).coerceAtLeast(0f)
            } else if (cursorRight > scrollX.floatValue + canvasWidth - cursorFocusPaddingPx) {
                scrollX.floatValue = (cursorRight - canvasWidth + cursorFocusPaddingPx).coerceAtLeast(0f)
            }
        }
    }

    // Cursor blink effect
    LaunchedEffect(Unit) {
        while (true) {
            delay(cursorBlinkRate)
            val currentTime = System.currentTimeMillis()
            if (!isCursorActive || currentTime - lastCursorActivityTime > cursorBlinkRate) {
                if (!isCursorMoving) {
                    showCursor = !showCursor
                }
            }
        }
    }

    fun updateCursorActivity() {
        isCursorActive = true
        lastCursorActivityTime = System.currentTimeMillis()
        showCursor = true
        isCursorMoving = true
        coroutineScope.launch {
            delay(100)
            isCursorMoving = false
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(themeStyle?.editorBackground ?: Color.Transparent)
            .focusRequester(focusRequester)
            .then(
                if (editable) {
                    Modifier.textInput(
                        keyboardController = keyboardController,
                        editorState = editorState,
                        onKeyEvent = { event: KeyEvent ->
                            dirtyRegion.value = Pair(Float.MAX_VALUE, Float.MAX_VALUE)
                            EventBus.getInstance().postSync(event)

                            if (event.type == KeyEventType.KeyDown) {
                                updateCursorActivity()
                                when (event.key) {
                                    Key.Backspace -> {
                                        if (editorState.text.isNotEmpty() && editorState.cursorPosition > 0) {
                                            val currentText = editorState.text
                                            val cursorPos = editorState.cursorPosition
                                            val beforeCursor = currentText.substring(0, cursorPos - 1)
                                            val afterCursor = currentText.substring(cursorPos)
                                            val newText = beforeCursor + afterCursor
                                            editorState.updateText(newText)
                                            onTextChanged(newText)
                                            editorState.moveCursor(cursorPos - 1)
                                            ensureCursorInView()
                                        }
                                        true
                                    }

                                    Key.DirectionLeft -> {
                                        if (editorState.cursorPosition > 0) {
                                            editorState.moveCursor(editorState.cursorPosition - 1)
                                            ensureCursorInView()
                                        }
                                        true
                                    }

                                    Key.DirectionRight -> {
                                        if (editorState.cursorPosition < editorState.text.length) {
                                            editorState.moveCursor(editorState.cursorPosition + 1)
                                            ensureCursorInView()
                                        }
                                        true
                                    }

                                    Key.DirectionUp -> {
                                        val lines = editorState.text.lines()
                                        var currentPosition = 0
                                        var currentLine = 0
                                        var currentColumn = 0

                                        // Find current line and column
                                        for (i in lines.indices) {
                                            val lineLength = lines[i].length + 1
                                            if (currentPosition + lineLength > editorState.cursorPosition) {
                                                currentLine = i
                                                currentColumn = editorState.cursorPosition - currentPosition
                                                break
                                            }
                                            currentPosition += lineLength
                                        }

                                        if (currentLine > 0) {
                                            val targetLine = currentLine - 1
                                            val targetColumn = currentColumn.coerceAtMost(lines[targetLine].length)
                                            var newPosition = 0
                                            for (i in 0 until targetLine) {
                                                newPosition += lines[i].length + 1
                                            }
                                            newPosition += targetColumn
                                            editorState.moveCursor(newPosition)
                                            ensureCursorInView()
                                        }
                                        true
                                    }

                                    Key.DirectionDown -> {
                                        val lines = editorState.text.lines()
                                        var currentPosition = 0
                                        var currentLine = 0
                                        var currentColumn = 0

                                        // Find current line and column
                                        for (i in lines.indices) {
                                            val lineLength = lines[i].length + 1
                                            if (currentPosition + lineLength > editorState.cursorPosition) {
                                                currentLine = i
                                                currentColumn = editorState.cursorPosition - currentPosition
                                                break
                                            }
                                            currentPosition += lineLength
                                        }

                                        if (currentLine < lines.size - 1) {
                                            val targetLine = currentLine + 1
                                            val targetColumn = currentColumn.coerceAtMost(lines[targetLine].length)
                                            var newPosition = 0
                                            for (i in 0 until targetLine) {
                                                newPosition += lines[i].length + 1
                                            }
                                            newPosition += targetColumn
                                            editorState.moveCursor(newPosition)
                                            ensureCursorInView()
                                        }
                                        true
                                    }

                                    Key.Enter -> {
                                        val currentText = editorState.text
                                        val cursorPos = editorState.cursorPosition

                                        // normalize line endings to \n for consistent handling
                                        val normalizedText = currentText.replace("\r\n", "\n").replace("\r", "\n")

                                        // get text before and after cursor
                                        val beforeCursor = normalizedText.substring(0, cursorPos)
                                        val afterCursor = normalizedText.substring(cursorPos)

                                        // create new text with normalized line ending
                                        val newText = beforeCursor + "\n" + afterCursor

                                        editorState.updateText(newText)
                                        // move cursor exactly one position forward for newline
                                        editorState.moveCursor(cursorPos + 1)
                                        onTextChanged(newText)
                                        ensureCursorInView()
                                        true
                                    }

                                    else -> {
                                        if (event.utf16CodePoint != 0) {
                                            val char = event.utf16CodePoint.toChar()
                                            if (char.isDefined() && !char.isISOControl()) {
                                                val currentText = editorState.text
                                                val cursorPos = editorState.cursorPosition
                                                val beforeCursor = currentText.substring(0, cursorPos)
                                                val afterCursor = currentText.substring(cursorPos)
                                                val newText = beforeCursor + char + afterCursor
                                                editorState.updateText(newText)
                                                onTextChanged(newText)
                                                editorState.moveCursor(cursorPos + 1)
                                                ensureCursorInView()
                                                true
                                            } else {
                                                false
                                            }
                                        } else {
                                            false
                                        }
                                    }
                                }
                            } else {
                                false
                            }
                        },
                        onCursorMoved = {
                            updateCursorActivity()
                            ensureCursorInView()
                        }
                    )
                } else {
                    Modifier
                }
            )
            .focusable(interactionSource = remember { MutableInteractionSource() })
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (!editable) {
                        notifier.notify("read only")
                    }

                    focusRequester.requestFocus()
                    updateCursorActivity()

                    val tapX = offset.x - gutterWidthPx - horizontalPaddingPx + scrollX.floatValue
                    val tapY = offset.y + scrollY.floatValue

                    val lineNumber = (tapY / fullLineHeightPx).toInt().coerceAtLeast(0)
                    val lines = editorState.text.lines()
                    if (lineNumber < lines.size) {
                        val line = lines[lineNumber]
                        var charPosition = 0
                        var currentX = 0f
                        for (i in line.indices) {
                            val charWidth = measureText(line[i].toString())
                            if (currentX + charWidth / 2 > tapX) {
                                break
                            }
                            currentX += charWidth
                            charPosition++
                        }

                        var absolutePosition = 0
                        for (i in 0 until lineNumber) {
                            absolutePosition += lines[i].length + 1
                        }
                        absolutePosition += charPosition

                        editorState.moveCursor(absolutePosition)
                    }
                }
            }
            .pointerInput(editorState.text, wrapText, showGutter, pinnedLineNumbers) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val wrapWidth = canvasWidth - horizontalPaddingPx * 2 - gutterWidthPx
                        val allLines = getWrappedLines(editorState.text, textPaint, wrapWidth)
                        val (verticalLimit, horizontalLimit) = calculateScrollLimits(allLines, textPaint)

                        val verticalThumbHeight = if (verticalLimit > 0f) {
                            (canvasHeight / (allLines.size * fullLineHeightPx + bottomPaddingLines * fullLineHeightPx) * canvasHeight).coerceAtLeast(
                                20f
                            )
                        } else canvasHeight

                        val verticalThumbTop = if (verticalLimit > 0f) {
                            (scrollY.floatValue / verticalLimit * (canvasHeight - verticalThumbHeight)).coerceIn(
                                0f,
                                canvasHeight - verticalThumbHeight
                            )
                        } else 0f

                        val maxLineWidth = allLines.maxOfOrNull { measureText(it) } ?: 0f
                        val horizontalThumbWidth = if (!wrapText && horizontalLimit > 0f) {
                            (canvasWidth / (maxLineWidth + horizontalPaddingPx + endHorizontalPaddingPx + gutterWidthPx) * canvasWidth).coerceAtLeast(
                                20f
                            )
                        } else 0f

                        val horizontalThumbLeft = if (!wrapText && horizontalLimit > 0f) {
                            (scrollX.floatValue / horizontalLimit * (canvasHeight - horizontalThumbWidth))
                                .coerceIn(0f, canvasWidth - horizontalThumbWidth)
                        } else 0f

                        val inVerticalScrollbarX = offset.x >= canvasWidth - scrollbarThicknessPx && offset.x <= canvasWidth
                        val inVerticalScrollbarY = offset.y >= verticalThumbTop && offset.y <= verticalThumbTop + verticalThumbHeight

                        val inHorizontalScrollbarY = offset.y >= canvasHeight - scrollbarThicknessPx && offset.y <= canvasHeight
                        val inHorizontalScrollbarX = offset.x >= horizontalThumbLeft && offset.x <= horizontalThumbLeft + horizontalThumbWidth

                        val inGutterArea = showGutter && offset.x <= gutterWidthPx

                        draggingVerticalScrollbar = !inGutterArea && verticalLimit > 0f && inVerticalScrollbarX && inVerticalScrollbarY
                        draggingHorizontalScrollbar =
                            !inGutterArea && !wrapText && horizontalLimit > 0f && inHorizontalScrollbarY && inHorizontalScrollbarX

                        if (draggingVerticalScrollbar) {
                            verticalDragOffset = offset.y - verticalThumbTop
                        }
                        if (draggingHorizontalScrollbar) {
                            horizontalDragOffset = offset.x - horizontalThumbLeft
                        }
                    },
                    onDragEnd = {
                        draggingVerticalScrollbar = false
                        draggingHorizontalScrollbar = false
                    },
                    onDragCancel = {
                        draggingVerticalScrollbar = false
                        draggingHorizontalScrollbar = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val wrapWidth = canvasWidth - horizontalPaddingPx * 2 - gutterWidthPx
                        val allLines = getWrappedLines(editorState.text, textPaint, wrapWidth)
                        val (verticalLimit, horizontalLimit) = calculateScrollLimits(allLines, textPaint)

                        if (draggingVerticalScrollbar && verticalLimit > 0f) {
                            val verticalThumbHeight =
                                (canvasHeight / (allLines.size * fullLineHeightPx + bottomPaddingLines * fullLineHeightPx) * canvasHeight).coerceAtLeast(
                                    20f
                                )
                            val newThumbTop = (change.position.y - verticalDragOffset).coerceIn(0f, canvasHeight - verticalThumbHeight)
                            scrollY.floatValue = (newThumbTop / (canvasHeight - verticalThumbHeight)) * verticalLimit
                        } else if (draggingHorizontalScrollbar && !wrapText && horizontalLimit > 0f) {
                            val maxLineWidth = allLines.maxOfOrNull { measureText(it) } ?: 0f
                            val horizontalThumbWidth =
                                (canvasWidth / (maxLineWidth + horizontalPaddingPx + endHorizontalPaddingPx + gutterWidthPx) * canvasWidth)
                                    .coerceAtLeast(20f)
                            val newThumbLeft = (change.position.x - horizontalDragOffset).coerceIn(0f, canvasWidth - horizontalThumbWidth)
                            scrollX.floatValue = (newThumbLeft / (canvasWidth - horizontalThumbWidth)) * horizontalLimit
                        } else {
                            if (!draggingHorizontalScrollbar && !draggingVerticalScrollbar) {
                                scrollY.floatValue = (scrollY.floatValue - dragAmount.y).coerceIn(0f, verticalLimit)
                                if (!wrapText) {
                                    scrollX.floatValue = (scrollX.floatValue - dragAmount.x).coerceIn(0f, horizontalLimit)
                                }
                            }
                        }
                    }
                )
            }
    ) {
        // update canvas size
        canvasWidth = size.width
        canvasHeight = size.height

        val wrapWidth = canvasWidth - horizontalPaddingPx * 2 - gutterWidthPx
        val allVisualLines = getWrappedLines(editorState.text, textPaint, wrapWidth)
        val (verticalLimit, horizontalLimit) = calculateScrollLimits(allVisualLines, textPaint)

        scrollY.floatValue = scrollY.floatValue.coerceIn(0f, verticalLimit)
        if (!wrapText) {
            scrollX.floatValue = scrollX.floatValue.coerceIn(0f, horizontalLimit)
        } else {
            scrollX.floatValue = 0f
        }

        val firstVisibleIndex = (scrollY.floatValue / fullLineHeightPx).toInt().coerceAtLeast(0)
        val visibleCount = (canvasHeight / fullLineHeightPx).toInt() + 2
        val lastVisibleIndex = (firstVisibleIndex + visibleCount).coerceAtMost(allVisualLines.size)

        // Draw only visible lines
        for (i in firstVisibleIndex until lastVisibleIndex) {
            val line = allVisualLines[i]
            val actualLineIndex = i
            val y = i * fullLineHeightPx - scrollY.floatValue + lineHeightPx
            val codeStartX = gutterWidthPx + horizontalPaddingPx - scrollX.floatValue

            var lineStartOffset = lineOffsets.getOrNull(actualLineIndex) ?: 0

            // get highlights for this line
            val lineHighlights = syntaxHighlights
                .map { highlight ->
                    val startChar = highlight.startOffset - lineStartOffset
                    val endChar = highlight.endOffset - lineStartOffset
                    Triple(
                        startChar.coerceIn(0, line.length),
                        endChar.coerceIn(0, line.length),
                        highlight.color
                    )
                }
                .filter { (start, end) -> start < end }
                .sortedBy { it.first }

            // draw the line character by character
            var currentX = codeStartX
            var currentPos = 0
            while (currentPos < line.length) {
                // find the next highlight that starts at or after current position
                val nextHighlight = lineHighlights.find { it.first >= currentPos }

                if (nextHighlight != null) {
                    // draw text before the highlight
                    if (nextHighlight.first > currentPos) {
                        val beforeText = line.substring(currentPos, nextHighlight.first)
                        drawContext.canvas.nativeCanvas.drawText(
                            beforeText,
                            currentX,
                            y,
                            textPaint
                        )
                        currentX += measureText(beforeText)
                    }

                    // draw the highlighted text
                    val highlightText = line.substring(nextHighlight.first, nextHighlight.second)
                    textPaint.color = nextHighlight.third
                    drawContext.canvas.nativeCanvas.drawText(
                        highlightText,
                        currentX,
                        y,
                        textPaint
                    )
                    currentX += measureText(highlightText)
                    textPaint.color = (themeStyle?.editorForeground ?: Color.White).toArgb()
                    currentPos = nextHighlight.second
                } else {
                    // no more highlights, draw the rest of the text
                    val remainingText = line.substring(currentPos)
                    drawContext.canvas.nativeCanvas.drawText(
                        remainingText,
                        currentX,
                        y,
                        textPaint
                    )
                    break
                }
            }
        }

        if (showGutter) {
            val gutterScrollX = if (pinnedLineNumbers) 0f else -scrollX.floatValue

            drawRect(
                color = effectiveGutterBackground,
                topLeft = Offset(gutterScrollX, 0f),
                size = Size(gutterWidthPx, canvasHeight)
            )

            drawRect(
                color = effectiveGutterDivider,
                topLeft = Offset(gutterWidthPx - 1f + gutterScrollX, 0f),
                size = Size(1f, canvasHeight)
            )

            for (i in firstVisibleIndex until lastVisibleIndex) {
                if (i >= allVisualLines.size) continue

                val y = i * fullLineHeightPx - scrollY.floatValue + lineHeightPx

                val shouldShowLineNumber = if (!wrapText) {
                    true
                } else {
                    if (i == 0) {
                        true
                    } else {
                        i - 1 < allVisualLines.size && allVisualLines[i] != allVisualLines[i - 1]
                    }
                }

                if (shouldShowLineNumber) {
                    val lineNumber = i + 1
                    drawContext.canvas.nativeCanvas.drawText(
                        lineNumber.toString(),
                        gutterWidthPx - horizontalPaddingPx / 2 + gutterScrollX,
                        y,
                        gutterPaint
                    )
                }
            }
        }

        drawScrollbars(
            verticalLimit,
            horizontalLimit,
            allVisualLines.maxOfOrNull { measureText(it) } ?: 0f,
            allVisualLines.size
        )

        if (showCursor) {
            val (cursorLine, cursorX) = calculateCursorPosition(editorState.cursorPosition, editorState.text.lines(), textPaint)
            val cursorY = cursorLine * fullLineHeightPx - scrollY.floatValue

            drawRect(
                color = themeStyle?.editorForeground ?: cursorColor,
                topLeft = Offset(
                    gutterWidthPx + horizontalPaddingPx + cursorX - scrollX.floatValue,
                    cursorY
                ),
                size = Size(
                    with(density) { cursorWidth.toPx() },
                    fullLineHeightPx
                )
            )
        }

        // Draw selection
        if (editorState.hasSelection) {
            val selectionStart = editorState.selectionStart
            val selectionEnd = editorState.selectionEnd
            val lines = editorState.text.lines()
            var currentPosition = 0

            for (i in lines.indices) {
                val line = lines[i]
                val lineLength = line.length + 1
                val lineStart = currentPosition
                val lineEnd = currentPosition + lineLength

                if (lineEnd > selectionStart && lineStart < selectionEnd) {
                    val startInLine = (selectionStart - lineStart).coerceAtLeast(0)
                    val endInLine = (selectionEnd - lineStart).coerceAtMost(line.length)

                    val startX = measureText(line.substring(0, startInLine))
                    val endX = measureText(line.substring(0, endInLine))

                    drawRect(
                        color = Color(0x33FFFFFF),
                        topLeft = Offset(
                            gutterWidthPx + horizontalPaddingPx + startX - scrollX.floatValue,
                            i * fullLineHeightPx - scrollY.floatValue
                        ),
                        size = Size(
                            endX - startX,
                            lineHeightPx
                        )
                    )
                }

                currentPosition += lineLength
            }
        }

        // clear dirty region after drawing
        dirtyRegion.value = null
    }
}
