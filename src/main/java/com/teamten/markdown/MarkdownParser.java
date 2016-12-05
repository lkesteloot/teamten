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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Markdown file into a DOM.
 */
public class MarkdownParser {
    private static final Map<String,BlockType> TAG_BLOCK_TYPE_MAP = new HashMap<>();
    private static final Pattern mMetadataPattern = Pattern.compile("([A-Za-z-]+): (.*)");

    private enum ParserState {
        START_OF_LINE,
        ONE_SPACE,
        TWO_SPACES,
        THREE_SPACES,
        IN_LINE,
        SKIP_WHITESPACE,
        COMMENT,
        IN_TAG,
        NUMBERED_LIST,
        LINE_OF_CODE,
    }

    static {
        // Map tags like "[TOC]" to the block they should create.
        TAG_BLOCK_TYPE_MAP.put("HALF-TITLE", BlockType.HALF_TITLE_PAGE);
        TAG_BLOCK_TYPE_MAP.put("TITLE", BlockType.TITLE_PAGE);
        TAG_BLOCK_TYPE_MAP.put("COPYRIGHT", BlockType.COPYRIGHT_PAGE);
        TAG_BLOCK_TYPE_MAP.put("TOC", BlockType.TABLE_OF_CONTENTS);
        TAG_BLOCK_TYPE_MAP.put("INDEX", BlockType.INDEX);
    }

    public static void main(String[] args) throws IOException {
        InputStream inputStream = new FileInputStream(args[0]);
        MarkdownParser parser = new MarkdownParser();
        Doc doc = parser.parse(inputStream);

        for (Block block : doc.getBlocks()) {
            System.out.println("Block: " + block);
        }
    }

    public Doc parse(InputStream inputStream) throws IOException {
        Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        // Our document.
        Doc doc = new Doc();

        ParserState state = ParserState.START_OF_LINE;
        BlockType blockType = BlockType.BODY;
        Block.Builder builder = null;
        boolean isItalic = false;
        boolean isSmallCaps = false;
        // Whether we're owed a space from the end of the previous line.
        boolean newlineSpace = false;
        boolean newlineSpaceIsItalic = false;
        boolean newlineSpaceIsSmallCaps = false;
        // Accumulated tag.
        StringBuilder tagBuilder = new StringBuilder();
        ParserState preTagState = null;
        boolean processSameCharacter = false;
        int chOrEof = 0;
        while (processSameCharacter || (chOrEof = reader.read()) != -1) {
            char ch = (char) chOrEof;
            processSameCharacter = false;

            if (Character.isWhitespace(ch) && ch != '\n' && ch != ' ') {
                System.out.printf("Warning: Skipped whitespace character 0x%02x\n", (int) ch);
                continue;
            }

            /// System.out.printf("%d (%c) %s %s%n", chOrEof, ch, state, builder);
            switch (state) {
                case START_OF_LINE:
                    if (ch == ' ') {
                        if (blockType == BlockType.BODY) {
                            state = ParserState.ONE_SPACE;
                        } else {
                            // Skip initial whitespace, probably after "#" or similar.
                        }
                    } else if (ch == '%') {
                        // Comment, skip rest of line.
                        state = ParserState.COMMENT;
                    } else if (builder == null && Character.isDigit(ch)) {
                        state = ParserState.NUMBERED_LIST;
                        tagBuilder.setLength(0);
                        tagBuilder.append(ch);
                    } else if (builder == null && ch == '#' && blockType == BlockType.BODY) {
                        blockType = BlockType.PART_HEADER;
                    } else if (builder == null && ch == '#' && blockType == BlockType.PART_HEADER) {
                        blockType = BlockType.CHAPTER_HEADER;
                    } else if (builder == null && ch == '#' && blockType == BlockType.CHAPTER_HEADER) {
                        blockType = BlockType.MINOR_SECTION_HEADER;
                    } else if (builder == null && ch == '#' && blockType == BlockType.MINOR_SECTION_HEADER) {
                        blockType = BlockType.MINOR_HEADER;
                    } else if (ch == '*') {
                        isItalic = true;
                    } else if (ch == '\n') {
                        // Blank line, end of chunk.
                        newlineSpace = false;
                        if (builder != null && !builder.isEmpty()) {
                            doc.addBlock(builder.build());
                            builder = null;
                            blockType = BlockType.BODY;
                        }
                    } else if (ch == '[') {
                        tagBuilder.setLength(0);
                        preTagState = state;
                        state = ParserState.IN_TAG;
                    } else {
                        if (builder == null) {
                            builder = new Block.Builder(blockType);
                        }
                        if (newlineSpace) {
                            builder.addText(' ', newlineSpaceIsItalic, newlineSpaceIsSmallCaps);
                            newlineSpace = false;
                        }
                        builder.addText(translateCharacter(ch), isItalic, isSmallCaps);
                        state = ParserState.IN_LINE;
                    }
                    break;

                case ONE_SPACE:
                    if (ch == ' ') {
                        state = ParserState.TWO_SPACES;
                    } else {
                        state = ParserState.START_OF_LINE;
                        processSameCharacter = true;
                    }
                    break;

                case TWO_SPACES:
                    if (ch == ' ') {
                        state = ParserState.THREE_SPACES;
                    } else {
                        state = ParserState.START_OF_LINE;
                        processSameCharacter = true;
                    }
                    break;

                case THREE_SPACES:
                    if (ch == ' ' || ch == '>') {
                        state = ParserState.LINE_OF_CODE;
                        builder = new Block.Builder(ch == '>' ? BlockType.OUTPUT : BlockType.CODE);
                    } else {
                        state = ParserState.START_OF_LINE;
                        processSameCharacter = true;
                    }
                    break;

                case IN_LINE:
                    if (ch == '\n') {
                        state = ParserState.START_OF_LINE;
                        if (blockType == BlockType.CODE) {
                            // Code blocks don't wrap.
                            if (builder != null && !builder.isEmpty()) {
                                doc.addBlock(builder.build());
                            }
                            builder = null;
                            blockType = BlockType.BODY;
                        } else {
                            // Here we should treat the newline as a space, but we don't want to actually add the space
                            // if the next line ends the paragraph. So we just remember that we're owed a space, and
                            // add it later if the next line continues with more text. This also properly handles the
                            // case of the next line being a comment.
                            newlineSpace = true;
                            newlineSpaceIsItalic = isItalic;
                            newlineSpaceIsSmallCaps = isSmallCaps;
                        }
                    } else if (ch == ' ') {
                        state = ParserState.SKIP_WHITESPACE;
                        builder.addText(' ', isItalic, isSmallCaps);
                    } else if (ch == '*') {
                        isItalic = !isItalic;
                    } else if (ch == '[') {
                        tagBuilder.setLength(0);
                        preTagState = state;
                        state = ParserState.IN_TAG;
                    } else {
                        builder.addText(translateCharacter(ch), isItalic, isSmallCaps);
                    }
                    break;

                case SKIP_WHITESPACE:
                    if (ch == '\n') {
                        state = ParserState.START_OF_LINE;
                    } else if (ch == ' ') {
                        // Skip.
                    } else if (ch == '*') {
                        state = ParserState.IN_LINE;
                        isItalic = !isItalic;
                    } else if (ch == '[') {
                        tagBuilder.setLength(0);
                        preTagState = state;
                        state = ParserState.IN_TAG;
                    } else {
                        state = ParserState.IN_LINE;
                        builder.addText(translateCharacter(ch), isItalic, isSmallCaps);
                    }
                    break;

                case COMMENT:
                    if (ch == '\n') {
                        // Back to normal.
                        state = ParserState.START_OF_LINE;
                    } else {
                        // Skip comment character.
                    }
                    break;

                case IN_TAG:
                    if (ch == ']') {
                        String tag = tagBuilder.toString();
                        BlockType tagBlockType = TAG_BLOCK_TYPE_MAP.get(tag);
                        if (tagBlockType != null) {
                            // Eject current block.
                            if (builder != null && !builder.isEmpty()) {
                                doc.addBlock(builder.build());
                                builder = null;
                            }
                            doc.addBlock(new Block.Builder(tagBlockType).build());
                        } else if (tag.equals("sc")) {
                            if (isSmallCaps) {
                                System.out.println("Warning: [sc] within [sc]");
                            }
                            isSmallCaps = true;
                        } else if (tag.equals("/sc")) {
                            if (!isSmallCaps) {
                                System.out.println("Warning: [/sc] not within [sc]");
                            }
                            isSmallCaps = false;
                        } else if (addMetadataTag(tag, doc)) {
                            // Nothing to do, the method added it.
                        } else if (tag.startsWith("@")) {
                            // Index entry.
                            builder.addSpan(IndexSpan.fromBarSeparatedEntries(tag.substring(1)));
                        } else if (tag.startsWith("^")) {
                            // Ignore footnote.
                        } else if (tag.startsWith("!")) {
                            builder.addSpan(ImageSpan.fromTag(tag.substring(1)));
                        } else {
                            System.out.println("Warning: Unknown block type: " + tag);
                        }
                        state = preTagState;
                        preTagState = null;
                    } else {
                        tagBuilder.append(ch);
                    }
                    break;

                case NUMBERED_LIST:
                    // Parse a paragraph prefix like:   2.
                    if (Character.isDigit(ch)) {
                        tagBuilder.append(ch);
                    } else if (ch == '.') {
                        builder = new Block.Builder(BlockType.NUMBERED_LIST);
                        state = ParserState.SKIP_WHITESPACE;
                    } else {
                        // Wasn't a numbered list. Start a normal paragraph.
                        builder = new Block.Builder(BlockType.BODY);
                        for (int i = 0; i < tagBuilder.length(); i++) {
                            builder.addText(translateCharacter(tagBuilder.charAt(i)), isItalic, isSmallCaps);
                        }
                        state = ParserState.IN_LINE;
                        processSameCharacter = true;
                    }
                    break;

                case LINE_OF_CODE:
                    if (ch == '\n') {
                        doc.addBlock(builder.build());
                        builder = null;
                        state = ParserState.START_OF_LINE;
                    } else {
                        builder.addText(translateCharacter(ch), false, false);
                    }
                    break;
            }
        }

        // Final block.
        if (builder != null && !builder.isEmpty()) {
            doc.addBlock(builder.build());
            builder = null;
        }

        return doc;
    }

    /**
     * Do some simple in-place translations of individual characters.
     */
    private static char translateCharacter(char ch) {
        if (ch == '~') {
            // No-break space.
            ch = '\u00A0';
        }

        return ch;
    }

    /**
     * Checks if the tag is in metadata form, and if so adds the key and value to the document.
     * <p>
     * <p>Metadata form is similar to email header form: "Multi-Word-Key: Value"
     *
     * @return whether the tag was processed as metadata.
     */
    private boolean addMetadataTag(String tag, Doc doc) {
        Matcher matcher = mMetadataPattern.matcher(tag);
        if (matcher.matches()) {
            String key = matcher.group(1);
            String value = matcher.group(2);

            doc.addMetadata(key, value);

            return true;
        }

        return false;
    }
}
