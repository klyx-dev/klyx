/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kwasm

import kwasm.api.HostFunction
import kwasm.api.MemoryProvider
import kwasm.ast.module.Index
import kwasm.ast.module.WasmModule
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.module.readModule
import kwasm.format.text.Tokenizer
import kwasm.format.text.module.parseModule
import kwasm.runtime.Address
import kwasm.runtime.ExecutionContext
import kwasm.runtime.FunctionInstance
import kwasm.runtime.Global
import kwasm.runtime.ImportExtern
import kwasm.runtime.Memory
import kwasm.runtime.ModuleInstance
import kwasm.runtime.Store
import kwasm.runtime.Table
import kwasm.runtime.allocate
import kwasm.runtime.collectImportExterns
import kwasm.runtime.instruction.execute
import kwasm.runtime.isType
import kwasm.runtime.stack.OperandStack
import kwasm.runtime.stack.RuntimeStacks
import kwasm.runtime.toFunctionInstance
import kwasm.runtime.toValue
import kwasm.validation.module.validate
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader

/**
 * A WebAssembly program.
 *
 * Consisting of one or more [WasmModule]s loaded either via [String]s or from
 * [File]s/[InputStream]s, a KWasmProgram may expose to the caller any [memory], functions, globals
 * exported by the loaded modules. Additionally, the caller may provide [HostFunction]s to those
 * modules to allow them to communicate with the external environment (e.g. i/o).
 */
class KWasmProgram internal constructor(
    moduleExports: Map<String, Map<String, Address>>,
    private val allocatedModules: MutableMap<String, ModuleInstance>,
    internal val store: Store
) {
    private val exportedMemoryAddresses =
        moduleExports.flatMap { it.value.values }.filterIsInstance<Address.Memory>().toSet()
    private val exportedFunctionAddresses =
        moduleExports.mapValues { (_, moduleExps) ->
            moduleExps.filter { (_, expAddress) -> expAddress is Address.Function }
        }
    private val exportedGlobalAddresses =
        moduleExports.mapValues { (_, moduleExps) ->
            moduleExps.filter { (_, expAddress) -> expAddress is Address.Global }
        }

    /**
     * Gets the [KWasmProgram]'s shared/exported [Memory].
     */
    val memory: Memory
        get() {
            val address = exportedMemoryAddresses.firstOrNull()
                ?: throw ExportNotFoundException("No exported memories found.")
            return store.memories[address.value]
        }

    /**
     * Gets the current value of the i32-typed global called [globalName] exported from the module
     * with name [moduleName].
     */
    fun getGlobalInt(moduleName: String, globalName: String): Int {
        val globalAddress = exportedGlobalAddresses[moduleName]?.get(globalName)
            ?: throw ExportNotFoundException(
                "No global with name \"$globalName\" was found to be " +
                    "exported from module: $moduleName"
            )
        val global = getGlobal(moduleName, globalName) as? Global.Int
            ?: throw ExportNotFoundException(
                "Global with name \"$globalName\" in module \"$moduleName\" was not of type" +
                    " Int, instead: ${store.globals[globalAddress.value]::class.simpleName}"
            )
        return global.value
    }

    /**
     * Sets the current value of the i32-typed global called [globalName] exported from the module
     * with name [moduleName] to the provided [newValue].
     */
    fun setGlobalInt(moduleName: String, globalName: String, newValue: Int) {
        val globalAddress = exportedGlobalAddresses[moduleName]?.get(globalName)
            ?: throw ExportNotFoundException(
                "No global with name \"$globalName\" was found to be " +
                    "exported from module: $moduleName"
            )
        val global = store.globals[globalAddress.value] as? Global.Int
            ?: throw ExportNotFoundException(
                "Global with name \"$globalName\" in module \"$moduleName\" was not of type" +
                    " Int, instead: ${store.globals[globalAddress.value]::class.simpleName}"
            )
        if (!global.mutable) throw ImmutableGlobalException(moduleName, globalName)
        global.value = newValue
    }

    /**
     * Gets the current value of the i64-typed global called [globalName] exported from the module
     * with name [moduleName].
     */
    fun getGlobalLong(moduleName: String, globalName: String): Long {
        val globalAddress = exportedGlobalAddresses[moduleName]?.get(globalName)
            ?: throw ExportNotFoundException(
                "No global with name \"$globalName\" was found to be " +
                    "exported from module: $moduleName"
            )
        val global = store.globals[globalAddress.value] as? Global.Long
            ?: throw ExportNotFoundException(
                "Global with name \"$globalName\" in module \"$moduleName\" was not of type" +
                    " Long, instead: ${store.globals[globalAddress.value]::class.simpleName}"
            )
        return global.value
    }

    /**
     * Sets the current value of the i64-typed global called [globalName] exported from the module
     * with name [moduleName] to the provided [newValue].
     */
    fun setGlobalLong(moduleName: String, globalName: String, newValue: Long) {
        val globalAddress = exportedGlobalAddresses[moduleName]?.get(globalName)
            ?: throw ExportNotFoundException(
                "No global with name \"$globalName\" was found to be " +
                    "exported from module: $moduleName"
            )
        val global = store.globals[globalAddress.value] as? Global.Long
            ?: throw ExportNotFoundException(
                "Global with name \"$globalName\" in module \"$moduleName\" was not of type" +
                    " Long, instead: ${store.globals[globalAddress.value]::class.simpleName}"
            )
        if (!global.mutable) throw ImmutableGlobalException(moduleName, globalName)
        global.value = newValue
    }

    /**
     * Gets the current value of the f32-typed global called [globalName] exported from the module
     * with name [moduleName].
     */
    fun getGlobalFloat(moduleName: String, globalName: String): Float {
        val globalAddress = exportedGlobalAddresses[moduleName]?.get(globalName)
            ?: throw ExportNotFoundException(
                "No global with name \"$globalName\" was found to be " +
                    "exported from module: $moduleName"
            )
        val global = store.globals[globalAddress.value] as? Global.Float
            ?: throw ExportNotFoundException(
                "Global with name \"$globalName\" in module \"$moduleName\" was not of type" +
                    " Float, instead: ${store.globals[globalAddress.value]::class.simpleName}"
            )
        return global.value
    }

    /**
     * Sets the current value of the f32-typed global called [globalName] exported from the module
     * with name [moduleName] to the provided [newValue].
     */
    fun setGlobalFloat(moduleName: String, globalName: String, newValue: Float) {
        val globalAddress = exportedGlobalAddresses[moduleName]?.get(globalName)
            ?: throw ExportNotFoundException(
                "No global with name \"$globalName\" was found to be " +
                    "exported from module: $moduleName"
            )
        val global = store.globals[globalAddress.value] as? Global.Float
            ?: throw ExportNotFoundException(
                "Global with name \"$globalName\" in module \"$moduleName\" was not of type" +
                    " Float, instead: ${store.globals[globalAddress.value]::class.simpleName}"
            )
        if (!global.mutable) throw ImmutableGlobalException(moduleName, globalName)
        global.value = newValue
    }

    /**
     * Gets the current value of the f64-typed global called [globalName] exported from the module
     * with name [moduleName].
     */
    fun getGlobalDouble(moduleName: String, globalName: String): Double {
        val globalAddress = exportedGlobalAddresses[moduleName]?.get(globalName)
            ?: throw ExportNotFoundException(
                "No global with name \"$globalName\" was found to be " +
                    "exported from module: $moduleName"
            )
        val global = store.globals[globalAddress.value] as? Global.Double
            ?: throw ExportNotFoundException(
                "Global with name \"$globalName\" in module \"$moduleName\" was not of type" +
                    " Double, instead: ${store.globals[globalAddress.value]::class.simpleName}"
            )
        return global.value
    }

    /**
     * Sets the current value of the f64-typed global called [globalName] exported from the module
     * with name [moduleName] to the provided [newValue].
     */
    fun setGlobalDouble(moduleName: String, globalName: String, newValue: Double) {
        val globalAddress = exportedGlobalAddresses[moduleName]?.get(globalName)
            ?: throw ExportNotFoundException(
                "No global with name \"$globalName\" was found to be " +
                    "exported from module: $moduleName"
            )
        val global = store.globals[globalAddress.value] as? Global.Double
            ?: throw ExportNotFoundException(
                "Global with name \"$globalName\" in module \"$moduleName\" was not of type" +
                    " Double, instead: ${store.globals[globalAddress.value]::class.simpleName}"
            )
        if (!global.mutable) throw ImmutableGlobalException(moduleName, globalName)
        global.value = newValue
    }

    /**
     * Gets an [ExportedFunction] by the given [functionName] in the [moduleName]-named module.
     */
    fun getFunction(moduleName: String, functionName: String): ExportedFunction {
        val functionAddress = exportedFunctionAddresses[moduleName]?.get(functionName)
            ?: throw ExportNotFoundException(
                "No function with name \"$functionName\" was found to be exported from " +
                    "module: $moduleName"
            )
        val function = store.functions[functionAddress.value] as FunctionInstance.Module

        return object : ExportedFunction {
            override val signature: String = function.type.toSignature(moduleName, functionName)

            override val argCount: Int = function.type.parameters.size

            override fun invoke(vararg args: Number): Number? {
                val parameters = function.type.parameters
                val argList = args.toList().map(Number::toValue)

                val argException = ExportedFunction.IllegalArgumentException.create(
                    moduleName,
                    functionName,
                    function.type,
                    argList
                )

                if (args.size != parameters.size) throw argException
                if (parameters.zip(argList).any { !it.second.isType(it.first.valType) })
                    throw argException

                val stacks = RuntimeStacks(operands = OperandStack(argList))
                val context = ExecutionContext(store, function.moduleInstance, stacks)
                return function.execute(context).stacks.operands.peek()?.value
            }
        }
    }

    internal fun getGlobal(moduleName: String, globalName: String): Global<*> {
        val globalAddress = exportedGlobalAddresses[moduleName]?.get(globalName)
            ?: throw ExportNotFoundException(
                "No global with name \"$globalName\" was found to be " +
                    "exported from module: $moduleName"
            )
        return store.globals[globalAddress.value]
    }

    class Builder internal constructor(
        private var memoryProvider: MemoryProvider
    ) {
        private val tokenizer = Tokenizer()
        private val hostFunctions = mutableMapOf<Pair<String, String>, HostFunction<*>>()
        private val hostMemories = mutableMapOf<Pair<String, String>, Memory>()
        private val hostTables = mutableMapOf<Pair<String, String>, Table>()
        private val hostGlobals = mutableMapOf<Pair<String, String>, Global<*>>()
        internal val parsedModules = mutableMapOf<String, WasmModule>()
        internal val modulesInOrder = mutableListOf<Pair<String, WasmModule>>()

        /**
         * Returns a [KWasmProgram] ready-to-use.
         *
         * Allocates and Instantiates all provided modules before returning.
         */
        fun build(): KWasmProgram {
            val allocatedModules = mutableMapOf<String, ModuleInstance>()
            val moduleImports = mutableMapOf<String, List<ImportExtern<out Address>>>()
            val moduleExports = mutableMapOf<String, MutableMap<String, Address>>()
            var store = Store()

            val hostExports = mutableMapOf<String, MutableMap<String, Address>>()
            fun updateHostExports(namespace: String, name: String, address: Address) {
                hostExports.compute(namespace) { _, map ->
                    val result = map ?: mutableMapOf()
                    result[name] = address
                    result
                }
            }
            hostFunctions.forEach { (namespace, name), fn ->
                val allocation = store.allocateFunction(fn.toFunctionInstance())
                updateHostExports(namespace, name, allocation.allocatedAddress)
                store = allocation.updatedStore
            }
            hostMemories.forEach { (namespace, name), mem ->
                val allocation = store.allocateMemory(mem)
                updateHostExports(namespace, name, allocation.allocatedAddress)
                store = allocation.updatedStore
            }
            hostTables.forEach { (namespace, name), table ->
                val allocation = store.allocateTable(table)
                updateHostExports(namespace, name, allocation.allocatedAddress)
                store = allocation.updatedStore
            }
            hostGlobals.forEach { (namespace, name), global ->
                val allocation = store.allocateGlobal(global)
                updateHostExports(namespace, name, allocation.allocatedAddress)
                store = allocation.updatedStore
            }

            // Allocate the modules, and gather their exports.
            // Currently this is done strictly in the order in which the modules were added to the
            // builder. It would be nice to support automatically figuring out the correct order so
            // that dependencies don't need to be added in the correct order by the caller at all
            // times.
            modulesInOrder.forEach { (name, module) ->
                val myImports = module.collectImportExterns()
                val myExports = mutableMapOf<String, Address>()

                val validationContext = module.validate()
                val (newStore, allocation) = module.allocate(
                    validationContext,
                    memoryProvider,
                    myImports,
                    store
                )

                allocation.exports.forEach { export ->
                    myExports[export.name] = export.address
                }

                store = newStore
                moduleImports[name] = myImports
                moduleExports[name] = myExports
                allocatedModules[name] = allocation
            }

            // Link up the exports to the imports by updating ImportExtern addresses with their
            // proper values.
            moduleImports.forEach { (moduleName, imports) ->
                imports.forEach { import ->
                    val exportCandidate = moduleExports[import.moduleName]?.get(import.name)
                        ?: hostExports[import.moduleName]?.get(import.name)
                        ?: throw ImportNotFoundException(moduleName, import)

                    import.addressPlaceholder.value = when (import.addressPlaceholder) {
                        is Address.Function -> exportCandidate as? Address.Function
                        is Address.Table -> exportCandidate as? Address.Table
                        is Address.Memory -> exportCandidate as? Address.Memory
                        is Address.Global -> exportCandidate as? Address.Global
                    }?.value ?: throw ImportMismatchException(moduleName, exportCandidate)

                    import.addressPlaceholder.value = exportCandidate.value
                }
            }

            // Initialize globals for modules.
            modulesInOrder.forEach { (name, moduleNode) ->
                val importedGlobals =
                    moduleImports[name]?.mapNotNull {
                        it.addressPlaceholder as? Address.Global
                    } ?: emptyList()
                val allocatedModule = allocatedModules[name] ?: return@forEach
                val auxiliaryInstance = allocatedModule.toGlobalInitInstance(importedGlobals)
                val context = ExecutionContext(store, auxiliaryInstance, RuntimeStacks())

                moduleNode.globals.forEach { global ->
                    val updatedContext = global.initExpression.execute(context)
                    val globalId = Index.ByIdentifier(global.id)
                    val address = requireNotNull(allocatedModule.globalAddresses[globalId])
                    val globalValue = store.globals[address.value]
                    globalValue.update(updatedContext.stacks.operands.pop().value)
                }
            }

            // Initialize the Element Segments and Data Segments.
            modulesInOrder.forEach { (name, moduleNode) ->
                val allocatedModule = checkNotNull(allocatedModules[name])
                val executionContext = ExecutionContext(store, allocatedModule, RuntimeStacks())

                // Check the element segments will fit in the tables, then place them there.
                moduleNode.elements.forEach { elementSegment ->
                    val offsetValue = elementSegment.offset.expression.execute(executionContext)
                        .stacks.operands.pop()
                    val offsetInt = checkNotNull(offsetValue.value as? Int)
                    val tableAddress =
                        checkNotNull(allocatedModule.tableAddresses[elementSegment.tableIndex])
                    val table = store.tables[tableAddress.value]

                    if (
                        offsetInt < 0 || offsetInt + elementSegment.init.size > table.elements.size
                    ) {
                        throw KWasmRuntimeException(
                            "Can't fit element segment with size ${elementSegment.init.size} " +
                                "into table with current size = ${table.elements.size} at " +
                                " offset = $offsetInt. (elements segment does not fit)"
                        )
                    }

                    elementSegment.init.forEachIndexed { loc, fnIndex ->
                        val addr = checkNotNull(allocatedModule.functionAddresses[fnIndex])
                        table.addFunction(offsetInt + loc, addr)
                    }
                }

                // Check the data segments will fit in the memories, then update the memory.
                moduleNode.data.forEach { dataSegment ->
                    val offsetValue = dataSegment.offset.expression.execute(executionContext)
                        .stacks.operands.pop()
                    val offsetInt = checkNotNull(offsetValue.value as? Int)
                    val memoryAddress =
                        checkNotNull(allocatedModule.memoryAddresses[dataSegment.memoryIndex])
                    val memory = store.memories[memoryAddress.value]

                    try {
                        memory.writeBytes(dataSegment.init, offsetInt)
                    } catch (e: IllegalArgumentException) {
                        if (
                            e.message?.contains("cannot fit") == true ||
                            e.message?.contains("newPosition < 0") == true
                        ) {
                            throw KWasmRuntimeException(
                                message = "${e.message}: (data segment does not fit)",
                                cause = e
                            )
                        }
                        throw e
                    }
                }
            }

            // Invoke the start functions.
            modulesInOrder.forEach { (name, module) ->
                val startFunction = module.start ?: return@forEach
                val allocatedModule = allocatedModules[name] ?: return@forEach
                val index = startFunction.funcIndex
                val addr = checkNotNull(allocatedModule.functionAddresses[index])
                val startContext = ExecutionContext(store, allocatedModule, RuntimeStacks())
                store.functions[addr.value].execute(startContext)
            }

            return KWasmProgram(moduleExports, allocatedModules, store)
        }

        /**
         * Provides a [HostFunction] for importing from the [WasmModule]s in the [KWasmProgram]
         * returned from [build].
         */
        fun withHostFunction(
            namespace: String,
            name: String,
            hostFunction: HostFunction<*>
        ) = apply { hostFunctions[namespace to name] = hostFunction }

        /**
         * Provides a [Memory] for access by [WasmModule]s in the [KWasmProgram].
         */
        fun withHostMemory(namespace: String, name: String, memory: Memory) = apply {
            hostMemories[namespace to name] = memory
        }

        /**
         * Provides an immutable [Global] for access by [WasmModule]s in the [KWasmProgram].
         */
        fun withHostGlobal(namespace: String, name: String, value: Number) = apply {
            val global = when (value) {
                is Int -> Global.Int(value, false)
                is Long -> Global.Long(value, false)
                is Float -> Global.Float(value, false)
                is Double -> Global.Double(value, false)
                else -> {
                    throw IllegalArgumentException("Unsupported global type: ${value::class}")
                }
            }
            hostGlobals[namespace to name] = global
        }

        /**
         * Provides a function [Table] for use by [WasmModule]s in the [KWasmProgram].
         */
        fun withHostTable(namespace: String, name: String, table: Table) = apply {
            hostTables[namespace to name] = table
        }

        /**
         * Parses a [WasmModule] from the provided [source] and associates it with the
         * given [name] for the [KWasmProgram] returned from [build].
         */
        fun withModule(name: String, source: String) = apply {
            val moduleTree =
                tokenizer.tokenize(source, ParseContext(name)).parseModule(0)?.astNode
                    ?: throw ParseException("No module found in source for $name")

            withModule(name, moduleTree)
        }

        /**
         * Parses a text-format [WasmModule] from the provided [file] and associates
         * it with the given [name] for the [KWasmProgram] returned from [build].
         */
        fun withTextFormatModule(name: String, file: File) = apply {
            val moduleTree = parseTextModule(
                name,
                BufferedInputStream(FileInputStream(file)),
                ParseContext(file.path)
            )

            withModule(name, moduleTree)
        }

        /**
         * Parses a text-format [WasmModule] from the provided [sourceStream] and
         * associates it with the given [name] for the [KWasmProgram] returned from [build].
         *
         * The provided [InputStream] will be closed after being parsed-from.
         */
        fun withTextFormatModule(name: String, sourceStream: InputStream) = apply {
            val moduleTree = parseTextModule(name, sourceStream, ParseContext(name))
            withModule(name, moduleTree)
        }

        /**
         * Parses a binary-format [WasmModule] from the provided [file] and
         * associates it with the given [name] for the [KWasmProgram] returned from [build].
         *
         * The provided [InputStream] will be closed after being parsed-from.
         */
        fun withBinaryModule(name: String, file: File) = apply {
            val moduleTree = parseBinaryModule(
                BufferedInputStream(FileInputStream(file)),
                file.path
            )
            withModule(name, moduleTree)
        }

        /**
         * Parses a binary-format [WasmModule] from the provided [binaryStream] and
         * associates it with the given [name] for the [KWasmProgram] returned from [build].
         *
         * The provided [InputStream] will be closed after being parsed-from.
         */
        fun withBinaryModule(name: String, binaryStream: InputStream) = apply {
            val moduleTree = parseBinaryModule(binaryStream, name)
            withModule(name, moduleTree)
        }

        internal fun withModule(name: String, module: WasmModule) {
            parsedModules[name] = module
            modulesInOrder.removeIf { it.first == name }
            modulesInOrder.add(name to module)
        }

        private fun parseTextModule(
            name: String,
            stream: InputStream,
            parseContext: ParseContext
        ): WasmModule = stream.use {
            tokenizer.tokenize(InputStreamReader(it), parseContext)
                .parseModule(0)?.astNode
                ?: throw ParseException("No module found in sourceStream for $name")
        }

        private fun parseBinaryModule(
            stream: InputStream,
            fileName: String
        ): WasmModule = stream.use { BinaryParser(it, fileName).readModule() }
    }

    /**
     * Thrown at linkage-time, when one of a module's imports couldn't be found.
     */
    class ImportNotFoundException(
        importingModule: String,
        extern: ImportExtern<*>
    ) : IllegalStateException("Module $importingModule's import not found: $extern")

    /**
     * Thrown at linkage-time, when an import was found, but the value was a mismatch for what was
     * required by the import.
     */
    class ImportMismatchException(
        importingModule: String,
        foundAddress: Address
    ) : IllegalStateException(
        "Import for $importingModule found, but was wrong type: $foundAddress"
    )

    /** Thrown when an exported value does not exist for a given module. */
    class ExportNotFoundException(message: String) : IllegalStateException(message)

    /** Thrown when an attempt to mutate an immutable global was made. */
    class ImmutableGlobalException(moduleName: String, globalName: String) : IllegalStateException(
        "Cannot mutate global: \"$globalName\" exported from module: $moduleName"
    )

    companion object {
        fun builder(memoryProvider: MemoryProvider): Builder = Builder(memoryProvider)
    }
}
