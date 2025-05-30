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

package kwasm.ast.util

import kwasm.ast.Identifier
import kwasm.ast.module.Index

/** Converts the [String] into an [Identifier.Type]-based [Index]. */
fun String.toTypeIndex() = Index.ByIdentifier(Identifier.Type(this))

/** Converts the [String] into an [Identifier.Function]-based [Index]. */
fun String.toFunctionIndex() = Index.ByIdentifier(Identifier.Function(this))

/** Converts the [String] into an [Identifier.Table]-based [Index]. */
fun String.toTableIndex() = Index.ByIdentifier(Identifier.Table(this))

/** Converts the [String] into an [Identifier.Memory]-based [Index]. */
fun String.toMemoryIndex() = Index.ByIdentifier(Identifier.Memory(this))

/** Converts the [String] into an [Identifier.Global]-based [Index]. */
fun String.toGlobalIndex() = Index.ByIdentifier(Identifier.Global(this))

/** Converts the [String] into an [Identifier.Local]-based [Index]. */
fun String.toLocalIndex() = Index.ByIdentifier(Identifier.Local(this))

/** Converts the [String] into an [Identifier.Label]-based [Index]. */
fun String.toLabelIndex() = Index.ByIdentifier(Identifier.Label(this))
