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
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Markdown file into a DOM.
 */
public class MarkdownParser {
    private static final Map<String,BlockType> TAG_BLOCK_TYPE_MAP = new HashMap<>();
    private static final Pattern mMetadataPattern = Pattern.compile("([A-Za-z-]+): (.*)");
    private boolean mForceBody;

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
        TAG_BLOCK_TYPE_MAP.put("SEPARATOR", BlockType.SEPARATOR);
        TAG_BLOCK_TYPE_MAP.put("NEW-PAGE", BlockType.NEW_PAGE);
        TAG_BLOCK_TYPE_MAP.put("ODD-PAGE", BlockType.ODD_PAGE);
    }

    public static void main(String[] args) throws IOException {
        InputStream inputStream = new FileInputStream(args[0]);
        MarkdownParser parser = new MarkdownParser();
        Doc doc = parser.parse(inputStream);

        for (Block block : doc.getBlocks()) {
            System.out.println("Block: " + block);
        }
    }

    public MarkdownParser() {
        mForceBody = false;
    }

    /**
     * Whether to force the parse as a body block. Setting this to true causes characters like # to be treated
     * like any other. Defaults to false.
     */
    public void setForceBody(boolean forceBody) {
        mForceBody = forceBody;
    }

    public Doc parse(InputStream inputStream) throws IOException {
        Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        // Our document.
        Doc doc = new Doc();

        ParserState state = ParserState.START_OF_LINE;
        BlockType blockType = BlockType.BODY;
        int lineNumber = 1;
        Block.Builder builder = null;
        FontVariantFlags flags = FontVariantFlags.PLAIN;
        // Whether we're owed a space from the end of the previous line.
        boolean newlineSpace = false;
        FontVariantFlags newlineSpaceFlags = FontVariantFlags.PLAIN;
        // Accumulated tag.
        StringBuilder tagBuilder = new StringBuilder();
        ParserState preTagState = null;
        boolean processSameCharacter = false;
        int chOrEof = 0;
        while (processSameCharacter || (chOrEof = reader.read()) != -1) {
            char ch = (char) chOrEof;
            processSameCharacter = false;

            // We only accept space and newline as whitespace. Avoid tabs, carriage returns, etc.
            if (Character.isWhitespace(ch) && ch != '\n' && ch != ' ') {
                System.out.printf("Warning (line " + lineNumber + "): Skipped whitespace character 0x%02x\n", (int) ch);
                continue;
            }

            /// System.out.printf("%d (%c) %s %s%n", chOrEof, ch, state, builder);
            switch (state) {
                case START_OF_LINE:
                    if (ch == ' ' && !mForceBody) {
                        if (blockType == BlockType.BODY) {
                            state = ParserState.ONE_SPACE;
                        } else {
                            // Skip initial whitespace, probably after "#" or similar.
                        }
                    } else if (ch == '%' && !mForceBody) {
                        // Comment, skip rest of line.
                        state = ParserState.COMMENT;
                    } else if (builder == null && Character.isDigit(ch) && !mForceBody && blockType == BlockType.BODY) {
                        state = ParserState.NUMBERED_LIST;
                        // Not really a tag, but we use the builder to keep the number in case it ends up
                        // not being a numbered list (just a line that starts with a number).
                        tagBuilder.setLength(0);
                        tagBuilder.append(ch);
                    } else if (builder == null && ch == '•' && !mForceBody) {
                        // This is a real bullet symbol. On Mac, use Alt-8 to type it.
                        state = ParserState.SKIP_WHITESPACE;
                        blockType = BlockType.BULLET_LIST;
                        builder = new Block.Builder(blockType, lineNumber);
                    } else if (builder == null && ch == '#' && blockType == BlockType.BODY && !mForceBody) {
                        blockType = BlockType.PART_HEADER;
                    } else if (builder == null && ch == '#' && blockType == BlockType.PART_HEADER && !mForceBody) {
                        blockType = BlockType.CHAPTER_HEADER;
                    } else if (builder == null && ch == '#' && blockType == BlockType.CHAPTER_HEADER && !mForceBody) {
                        blockType = BlockType.MINOR_SECTION_HEADER;
                    } else if (builder == null && ch == '#' && blockType == BlockType.MINOR_SECTION_HEADER && !mForceBody) {
                        blockType = BlockType.MINOR_HEADER;
                    } else if (ch == '*') {
                        flags = flags.toggleItalic();
                    } else if (ch == '`') {
                        flags = flags.toggleCode();
                    } else if (ch == '\n') {
                        // Blank line, end of chunk.
                        lineNumber++;
                        newlineSpace = false;
                        if (builder != null && !builder.isEmpty()) {
                            doc.addBlock(builder.build());
                            builder = null;
                            flags = FontVariantFlags.PLAIN;
                            blockType = BlockType.BODY;
                        }
                    } else if (ch == '[') {
                        tagBuilder.setLength(0);
                        preTagState = state;
                        state = ParserState.IN_TAG;
                    } else {
                        if (builder == null) {
                            builder = new Block.Builder(blockType, lineNumber);
                        }
                        if (newlineSpace) {
                            builder.addText(' ', newlineSpaceFlags);
                            newlineSpace = false;
                        }
                        builder.addText(ch, flags);
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
                    if (ch == ' ') {
                        state = ParserState.LINE_OF_CODE;
                        builder = new Block.Builder(BlockType.CODE, lineNumber);
                    } else if (ch == '>') {
                        state = ParserState.LINE_OF_CODE;
                        builder = new Block.Builder(BlockType.OUTPUT, lineNumber);
                    } else if (ch == '<') {
                        state = ParserState.LINE_OF_CODE;
                        builder = new Block.Builder(BlockType.INPUT, lineNumber);
                    } else if (ch == '/') {
                        state = ParserState.IN_LINE;
                        builder = new Block.Builder(BlockType.POETRY, lineNumber);
                    } else {
                        state = ParserState.START_OF_LINE;
                        processSameCharacter = true;
                    }
                    break;

                case IN_LINE:
                    if (ch == '\n') {
                        lineNumber++;
                        state = ParserState.START_OF_LINE;
                        if (builder != null && builder.getBlockType() == BlockType.POETRY) {
                            // Poetry doesn't wrap.
                            if (!builder.isEmpty()) {
                                doc.addBlock(builder.build());
                            }
                            builder = null;
                            flags = FontVariantFlags.PLAIN;
                            blockType = BlockType.BODY;
                        } else {
                            // Here we should treat the newline as a space, but we don't want to actually add the space
                            // if the next line ends the paragraph. So we just remember that we're owed a space, and
                            // add it later if the next line continues with more text. This also properly handles the
                            // case of the next line being a comment.
                            newlineSpace = true;
                            newlineSpaceFlags = flags;
                        }
                    } else if (ch == ' ') {
                        state = ParserState.SKIP_WHITESPACE;
                        builder.addText(' ', flags);
                    } else if (ch == '*') {
                        flags = flags.toggleItalic();
                    } else if (ch == '`') {
                        flags = flags.toggleCode();
                    } else if (ch == '[') {
                        tagBuilder.setLength(0);
                        preTagState = state;
                        state = ParserState.IN_TAG;
                    } else {
                        builder.addText(ch, flags);
                    }
                    break;

                case SKIP_WHITESPACE:
                    if (ch == '\n') {
                        lineNumber++;
                        state = ParserState.START_OF_LINE;
                    } else if (ch == ' ') {
                        // Skip.
                    } else if (ch == '*') {
                        state = ParserState.IN_LINE;
                        flags = flags.toggleItalic();
                    } else if (ch == '`') {
                        state = ParserState.IN_LINE;
                        flags = flags.toggleCode();
                    } else if (ch == '[') {
                        tagBuilder.setLength(0);
                        preTagState = state;
                        state = ParserState.IN_TAG;
                    } else {
                        state = ParserState.IN_LINE;
                        builder.addText(ch, flags);
                    }
                    break;

                case COMMENT:
                    if (ch == '\n') {
                        // Back to normal.
                        lineNumber++;
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
                                flags = FontVariantFlags.PLAIN;
                            }
                            doc.addBlock(new Block.Builder(tagBlockType, lineNumber).build());
                        } else if (tag.equals("sc")) {
                            if (flags.isSmallCaps()) {
                                System.out.println("Warning (line " + lineNumber + "): [sc] within [sc]");
                            }
                            flags = flags.withSmallCaps(true);
                        } else if (tag.equals("/sc")) {
                            if (!flags.isSmallCaps()) {
                                System.out.println("Warning (line " + lineNumber + "): [/sc] not within [sc]");
                            }
                            flags = flags.withSmallCaps(false);
                        } else if (addMetadataTag(tag, doc)) {
                            // Nothing to do, the method added it.
                        } else if (tag.startsWith("@")) {
                            // Index entry.
                            if (builder == null) {
                                // Not tested.
                                builder = new Block.Builder(blockType, lineNumber);
                            }
                            builder.addSpan(IndexSpan.fromBarSeparatedEntries(tag.substring(1)));
                        } else if (tag.startsWith("^")) {
                            // Footnote.
                            if (builder == null) {
                                // Not tested.
                                builder = new Block.Builder(blockType, lineNumber);
                            }
                            Block block = parseSingleBlock(tag.substring(1));
                            builder.addSpan(new FootnoteSpan(block));
                        } else if (tag.startsWith("!")) {
                            if (builder == null) {
                                builder = new Block.Builder(blockType, lineNumber);
                            }

                            // Split into filename and the caption.
                            String[] parts = tag.substring(1).trim().split(" ", 2);
                            String filename = parts[0];
                            Block caption = parts.length == 1
                                    ? null
                                    : parseSingleBlock(parts[1].trim()).withBlockType(BlockType.CAPTION);

                            builder.addSpan(new ImageSpan(filename, caption));
                        } else {
                            System.out.println("Warning (line " + lineNumber + "): Unknown block type: " + tag);
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
                        // It was a numbered list.
                        int counter = Integer.parseInt(tagBuilder.toString(), 10);
                        builder = Block.numberedListBuilder(lineNumber, counter);
                        state = ParserState.SKIP_WHITESPACE;
                    } else {
                        // Wasn't a numbered list. Start a normal paragraph.
                        builder = new Block.Builder(blockType, lineNumber);
                        for (int i = 0; i < tagBuilder.length(); i++) {
                            builder.addText(tagBuilder.charAt(i), flags);
                        }
                        state = ParserState.IN_LINE;
                        processSameCharacter = true;
                    }
                    break;

                case LINE_OF_CODE:
                    if (ch == '\n') {
                        lineNumber++;
                        doc.addBlock(builder.build());
                        builder = null;
                        flags = FontVariantFlags.PLAIN;
                        state = ParserState.START_OF_LINE;
                    } else {
                        if (ch == '`') {
                            // Toggle bold.
                            flags = flags.toggleBold();
                        } else {
                            builder.addText(ch, flags);
                        }
                    }
                    break;
            }
        }

        // Check if we were in the middle of parsing a number for a numbered list.
        if (state == ParserState.NUMBERED_LIST) {
            builder = new Block.Builder(blockType, lineNumber);
            for (int i = 0; i < tagBuilder.length(); i++) {
                builder.addText(tagBuilder.charAt(i), flags);
            }
        }

        // Final block.
        if (builder != null && !builder.isEmpty()) {
            doc.addBlock(builder.build());
            builder = null;
            flags = FontVariantFlags.PLAIN;
        }

        // Post-process the blocks to replace apostrophes, quotes, etc.
        for (Block block : doc.getBlocks()) {
            block.postProcessText();
        }

        return doc;
    }

    /**
     * Parse a single block from a string, returning the block. This does not support comments in
     * the Markdown.
     *
     * @throws IllegalArgumentException if the text cannot be parsed to exactly one block.
     */
    public static Block parseSingleBlock(String text) {
        Doc doc = parseDoc(text);

        List<Block> blocks = doc.getBlocks();

        if (blocks.size() != 1) {
            throw new IllegalArgumentException("text was not a single block: " + text);
        }

        return blocks.get(0);
    }

    /**
     * Parse a doc from a string. This does not support comments in the Markdown.
     */
    public static Doc parseDoc(String text) {
        try {
            InputStream inputStream = new ByteArrayInputStream(text.getBytes("UTF-8"));
            MarkdownParser parser = new MarkdownParser();
            parser.setForceBody(true);

            return parser.parse(inputStream);
        } catch (IOException e) {
            // Shouldn't happen, we don't do any actual I/O.
            throw new UncheckedIOException(e);
        }
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
