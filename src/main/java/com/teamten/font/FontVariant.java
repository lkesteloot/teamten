/*
 *
 *    Copyright 2016 Lawrence Kesteloot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.teamten.font;

/**
 * Describes the various variants of a typeface (regular, bold, etc.).
 */
public enum FontVariant {
    REGULAR, BOLD, ITALIC, BOLD_ITALIC, SMALL_CAPS;

    /**
     * Parses a font variant by converting it to upper case and transforming spaces and hyphens to
     * underscores, then looking it up in this enum.
     *
     * @throws IllegalArgumentException if the font variant is not found.
     */
    public static FontVariant parse(String s) {
        return valueOf(s.toUpperCase().replace(' ', '_').replace('-', '_'));
    }
}
