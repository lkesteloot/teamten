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
        COMMENT,
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
        StringBuilder builder = new StringBuilder();
        int chOrEof;
        parsingLoop: while ((chOrEof = reader.read()) != -1) {
            char ch = (char) chOrEof;

            /// System.out.printf("%d (%c) %s %s%n", chOrEof, ch, state, builder);
            switch (state) {
                case START_OF_LINE:
                    if (Character.isWhitespace(ch) && ch != '\n') {
                        // Skip initial white-space.
                    } else if (ch == '%') {
                        // Comment, skip rest of line.
                        state = ParserState.COMMENT;
                    } else if (ch == '\n') {
                        // Blank line, end of chunk.
                        if (builder.length() > 0) {
                            boolean continueProcessing = emitBlock(doc, builder);
                            if (!continueProcessing) {
                                break parsingLoop;
                            }
                        }
                    } else {
                        builder.append(ch);
                        state = ParserState.IN_LINE;
                    }
                    break;

                case IN_LINE:
                    if (ch == '\n') {
                        state = ParserState.START_OF_LINE;
                        builder.append(' ');
                    } else {
                        builder.append(ch);
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
            }
        }

        // Final block.
        if (builder.length() > 0) {
            emitBlock(doc, builder);
        }

        return doc;
    }

    /**
     * Add the chunk (paragraph) as a block to the document.
     *
     * @return whether to continue processing.
     */
    private boolean emitBlock(Doc doc, StringBuilder builder) {
        // Extract the text from the builder.
        String chunk = builder.toString().trim();
        builder.setLength(0);

        // Skip empty blocks. These can happen after the file header, etc.
        if (chunk.isEmpty()) {
            return true;
        }

        BlockType blockType;

        // Figure out type of block.
        if (chunk.startsWith("# ")) {
            chunk = chunk.substring(1).trim();
            blockType = BlockType.PART_HEADER;
        } else if (chunk.startsWith("## ")) {
            chunk = chunk.substring(2).trim();
            blockType = BlockType.CHAPTER_HEADER;
        } else if (chunk.equals("/bye")) {
            // Terminate processing; for testing.
            return false;
        } else {
            blockType = BlockType.BODY;
        }

        // Create DOM.
        Span span = new Span(chunk);
        Block block = new Block(blockType);
        block.addSpan(span);
        doc.addBlock(block);

        return true;
    }
}
