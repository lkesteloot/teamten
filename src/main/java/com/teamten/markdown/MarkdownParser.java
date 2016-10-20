package com.teamten.markdown;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a Markdown file into a DOM.
 */
public class MarkdownParser {
    private enum ParserState {
        START_OF_LINE,
        IN_LINE,
        SKIP_WHITESPACE,
        COMMENT,
        IN_TAG,
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
        // Whether we're owed a space from the end of the previous line.
        boolean newlineSpace = false;
        boolean newlineSpaceIsItalic = false;
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
                            builder.add(' ', newlineSpaceIsItalic);
                            newlineSpace = false;
                        }
                        builder.add(translateCharacter(ch), isItalic);
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
                    } else if (Character.isWhitespace(ch)) {
                        state = ParserState.SKIP_WHITESPACE;
                        builder.add(' ', isItalic);
                    } else if (ch == '*') {
                        isItalic = !isItalic;
                    } else if (ch == '[') {
                        tagBuilder.setLength(0);
                        preTagState = state;
                        state = ParserState.IN_TAG;
                    } else {
                        builder.add(translateCharacter(ch), isItalic);
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
                    } else {
                        state = ParserState.IN_LINE;
                        builder.add(translateCharacter(ch), isItalic);
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
                        if (tag.equals("TOC")) {
                            // Eject current block.
                            if (builder != null && !builder.isEmpty()) {
                                doc.addBlock(builder.build());
                                builder = null;
                            }
                            doc.addBlock(new Block.Builder(BlockType.TABLE_OF_CONTENTS).build());
                        } else {
                            System.out.println("Unknown block type: " + tag);
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
}
