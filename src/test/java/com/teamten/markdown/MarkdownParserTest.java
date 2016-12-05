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

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test for the {@link MarkdownParser} class.
 */
public class MarkdownParserTest {
    @Test
    public void blockParseTest() {
        assertBlockEquals(BlockType.BODY, "Hello", "Hello");
        assertBlockEquals(BlockType.BODY, "Hello", " Hello");
        assertBlockEquals(BlockType.PART_HEADER, "Hello", "# Hello");
        assertBlockEquals(BlockType.CHAPTER_HEADER, "Hello", "## Hello");
        assertBlockEquals(BlockType.MINOR_SECTION_HEADER, "Hello", "### Hello");
        assertBlockEquals(BlockType.MINOR_HEADER, "Hello", "#### Hello");
        assertBlockEquals(BlockType.NUMBERED_LIST, "Hello", "1. Hello");
        assertBlockEquals(BlockType.CODE, "Hello", "    Hello");
        assertBlockEquals(BlockType.CODE, "    Hello", "        Hello");
    }

    @Test
    public void spanParseTest() {
        List<Span> spans;

        spans = parseStringToBlocks("Hello").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "Hello", false, false);

        // Italics.
        spans = parseStringToBlocks("Hello *there* my friend").get(0).getSpans();
        assertEquals(3, spans.size());
        assertTextSpanEquals(spans.get(0), "Hello ", false, false);
        assertTextSpanEquals(spans.get(1), "there", true, false);
        assertTextSpanEquals(spans.get(2), " my friend", false, false);

        // Small caps.
        spans = parseStringToBlocks("Hello [sc]there[/sc] my friend").get(0).getSpans();
        assertEquals(3, spans.size());
        assertTextSpanEquals(spans.get(0), "Hello ", false, false);
        assertTextSpanEquals(spans.get(1), "there", false, true);
        assertTextSpanEquals(spans.get(2), " my friend", false, false);

        // Code lines don't parse italics.
        spans = parseStringToBlocks("    Hello *there* my friend").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "Hello *there* my friend", false, false);
    }

    private static void assertBlockEquals(BlockType expectedBlockType, String expectedText, String input) {
        Block block = parseStringToBlocks(input).get(0);
        assertEquals(expectedBlockType, block.getBlockType());
        assertEquals(expectedText, block.getText());
    }

    private static void assertTextSpanEquals(Span span, String text, boolean isItalics, boolean isSmallCaps) {
        TextSpan textSpan = (TextSpan) span;
        assertEquals(text, textSpan.getText());
        assertEquals(isItalics, textSpan.isItalic());
        assertEquals(isSmallCaps, textSpan.isSmallCaps());
    }

    private static Doc parseStringToDoc(String input) {
        MarkdownParser markdownParser = new MarkdownParser();
        try {
            return markdownParser.parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Block> parseStringToBlocks(String input) {
        return parseStringToDoc(input).getBlocks();
    }
}
