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
        IN_LINE,
        SKIP_WHITESPACE,
        COMMENT,
        IN_TAG,
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
        int chOrEof;
        while ((chOrEof = reader.read()) != -1) {
            char ch = (char) chOrEof;

            /// System.out.printf("%d (%c) %s %s%n", chOrEof, ch, state, builder);
            switch (state) {
                case START_OF_LINE:
                    if (Character.isWhitespace(ch) && ch != '\n') {
                        // Skip initial white-space.
                    } else if (ch == '%') {
                        // Comment, skip rest of line.
                        state = ParserState.COMMENT;
                    } else if (builder == null && ch == '#' && blockType == BlockType.BODY) {
                        blockType = BlockType.PART_HEADER;
                    } else if (builder == null && ch == '#' && blockType == BlockType.PART_HEADER) {
                        blockType = BlockType.CHAPTER_HEADER;
                    } else if (builder == null && ch == '#' && blockType == BlockType.CHAPTER_HEADER) {
                        blockType = BlockType.MINOR_SECTION_HEADER;
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

                case IN_LINE:
                    if (ch == '\n') {
                        state = ParserState.START_OF_LINE;
                        // Here we should treat the newline as a space, but we don't want to actually add the space
                        // if the next line ends the paragraph. So we just remember that we're owed a space, and
                        // add it later if the next line continues with more text. This also properly handles the
                        // case of the next line being a comment.
                        newlineSpace = true;
                        newlineSpaceIsItalic = isItalic;
                        newlineSpaceIsSmallCaps = isSmallCaps;
                    } else if (Character.isWhitespace(ch)) {
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
                    } else if (Character.isWhitespace(ch)) {
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
                            builder.addSpan(new IndexSpan(tag.substring(1)));
                        } else {
                            System.out.println("Warning: Unknown block type: " + tag);
                        }
                        state = preTagState;
                        preTagState = null;
                    } else {
                        tagBuilder.append(ch);
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
     *
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
