/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.material.utils

/** Utility methods for string representations of colors.  */
internal object StringUtils {
    /**
     * Hex string representing color, ex. #ff0000 for red.
     *
     * @param argb ARGB representation of a color.
     */
    fun hexFromArgb(argb: Int): String {
        val red = ColorUtils.redFromArgb(argb)
        val blue = ColorUtils.blueFromArgb(argb)
        val green = ColorUtils.greenFromArgb(argb)
        return buildString {
            append('#')
            append(red.toString(16).padStart(2, '0'))
            append(green.toString(16).padStart(2, '0'))
            append(blue.toString(16).padStart(2, '0'))
        }
    }
}
