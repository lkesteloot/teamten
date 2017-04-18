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

package com.teamten.markdown;

/**
 * Describes the type of the block (body, header).
 */
public enum BlockType {
    BODY,
    BLOCK_QUOTE,
    PART_HEADER,
    CHAPTER_HEADER,
    MINOR_SECTION_HEADER,
    MINOR_HEADER,
    NUMBERED_LIST,
    BULLET_LIST,
    CODE,
    OUTPUT,
    INPUT,
    POETRY,
    SEPARATOR,
    VERTICAL_SPACE,
    CAPTION,
    NEW_PAGE,
    ODD_PAGE,

    // Specific pages:
    HALF_TITLE_PAGE,
    TITLE_PAGE,
    COPYRIGHT_PAGE,
    TABLE_OF_CONTENTS,
    INDEX;

    /**
     * Whether this represents what you might see on a console.
     */
    public boolean isConsole() {
        return this == OUTPUT || this == INPUT;
    }
}
