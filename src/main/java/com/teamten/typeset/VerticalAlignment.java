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

package com.teamten.typeset;

/**
 * Various ways to vertically align a list of elements.
 */
public enum VerticalAlignment {
    /**
     * The baseline is at the top of the first element. All elements are in the depth.
     */
    TOP,
    /**
     * The baseline matches that of the first (top-most) box.
     */
    FIRST_BOX,
    /**
     * The baseline matches that of the last (bottom-most) box.
     */
    LAST_BOX,
    /**
     * The baseline is at the bottom of the last element. All elements are in the height.
     */
    BOTTOM
}
