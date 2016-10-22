
package com.teamten.markdown;

/**
 * Describes the type of the block (body, header).
 */
public enum BlockType {
    BODY,
    PART_HEADER,
    CHAPTER_HEADER,
    MINOR_SECTION_HEADER,

    // Specific pages:
    HALF_TITLE_PAGE,
    TITLE_PAGE,
    COPYRIGHT_PAGE,
    TABLE_OF_CONTENTS,
}
