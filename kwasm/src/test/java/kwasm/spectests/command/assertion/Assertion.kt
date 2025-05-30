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

package kwasm.spectests.command.assertion

import kwasm.format.text.ParseResult
import kwasm.format.text.token.Token
import kwasm.spectests.command.Command

fun List<Token>.parseAssertion(fromIndex: Int): ParseResult<out Command<*>>? {
    return parseAssertReturn(fromIndex)
        ?: parseAssertExhaustion(fromIndex)
        ?: parseAssertActionTrap(fromIndex)
        ?: parseAssertMalformed(fromIndex)
        ?: parseAssertUnlinkable(fromIndex)
        ?: parseAssertInvalid(fromIndex)
        ?: parseAssertInstantiationTrap(fromIndex)
}
