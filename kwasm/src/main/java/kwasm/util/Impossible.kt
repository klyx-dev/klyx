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

package kwasm.util

/**
 * Shorthand for marking a branch of code as impossible to reach.
 *
 * Useful mainly in `when` blocks on `reified` type-parameters when the `else` should never be hit.
 */
@Suppress("FunctionName")
fun Impossible(message: String = "Reached an impossible branch somehow"): Nothing = throw IllegalStateException(message)
