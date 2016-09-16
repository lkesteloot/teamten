package com.teamten.markdown;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a Markdown file into a DOM.
 */
public class MarkdownParser {
    private static final Splitter CHUNK_SPLITTER = Splitter.on("\n\n").
        omitEmptyStrings().trimResults();
    private static final Splitter LINE_SPLITTER = Splitter.on("\n").
        omitEmptyStrings().trimResults();
    private static final Joiner LINE_JOINER = Joiner.on(" ").skipNulls();

    public static void main(String[] args) throws IOException {
        InputStream inputStream = new FileInputStream(args[0]);
        MarkdownParser parser = new MarkdownParser();
        Doc doc = parser.parse(inputStream);

        for (Block block : doc.getBlocks()) {
            System.out.println("Block: " + block);
        }
    }

    public Doc parse(InputStream inputStream) throws IOException {
        String contents = IOUtils.toString(inputStream);

        // Our document.
        Doc doc = new Doc();

        // Cut at double newlines.
        Iterable<String> chunks = CHUNK_SPLITTER.split(contents);

        // Create a block for each.
        for (String chunk : chunks) {
            BlockType blockType;

            // Figure out type of block.
            if (chunk.startsWith("# ")) {
                chunk = chunk.substring(1).trim();
                blockType = BlockType.PART_HEADER;
            } else if (chunk.startsWith("## ")) {
                chunk = chunk.substring(2).trim();
                blockType = BlockType.CHAPTER_HEADER;
            } else {
                blockType = BlockType.BODY;
            }


            // Remove newlines.
            Iterable<String> lines = LINE_SPLITTER.split(chunk);
            chunk = LINE_JOINER.join(lines);

            // Create DOM.
            Span span = new Span(chunk);
            Block block = new Block(blockType);
            block.addSpan(span);
            doc.addBlock(block);
        }

        return doc;
    }
}
