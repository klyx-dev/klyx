/*
 * Copyright 2021 Google LLC
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

package kwasm.format.binary.module

import kwasm.ast.module.ElementSegment
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.value.readVector

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#element-section):
 *
 * The element section has the id 9. It decodes into a vector of element segments that represent
 * the `elem` component of a module.
 *
 * ```
 *      elemsec ::= seg*:section_9(vec(elem))           => seg
 *      elem    ::= x:tableidx e:expr y*:vec(funcidx)   => {table x, offset e, init y*}
 * ```
 */
fun BinaryParser.readElementSection(): ElementSection =
    ElementSection(readVector { readElementSegment() })

/** Reads an [ElementSegment] from a binary-encoded WebAssembly module's [ElementSection]. */
fun BinaryParser.readElementSegment(): ElementSegment =
    ElementSegment(readIndex(), readOffset(), readVector { readIndex() })

/** Represents an element section within a binary-encoded WebAssembly module. */
data class ElementSection(val segments: List<ElementSegment>) : Section
