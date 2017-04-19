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
import static org.junit.Assert.assertTrue;

/**
 * Test for the {@link MarkdownParser} class.
 */
public class MarkdownParserTest {
    @Test
    public void blockParseTest() {
        assertBlockEquals(BlockType.BODY, "Hello", "Hello");
        assertBlockEquals(BlockType.BODY, "Hello", " Hello");
        assertBlockEquals(BlockType.BODY, "1234", "1234");
        assertBlockEquals(BlockType.PART_HEADER, "Hello", "# Hello");
        assertBlockEquals(BlockType.CHAPTER_HEADER, "Hello", "## Hello");
        assertBlockEquals(BlockType.MINOR_SECTION_HEADER, "Hello", "### Hello");
        assertBlockEquals(BlockType.MINOR_HEADER, "Hello", "#### Hello");
        assertBlockEquals(BlockType.NUMBERED_LIST, "Hello", "1. Hello");
        assertBlockEquals(BlockType.CODE, "Hello", "    Hello");
        assertBlockEquals(BlockType.CODE, "    Hello", "        Hello");
        assertBlockEquals(BlockType.MINOR_HEADER, "1234", "#### 1234");
        assertBlockEquals(BlockType.MINOR_HEADER, "1234. Hello", "#### 1234. Hello");
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

        // Initial hyphen for conversation.
        spans = parseStringToBlocks("- Hey there").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "—Hey there", false, false);
        // Space is required:
        spans = parseStringToBlocks("-Hey there").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "-Hey there", false, false);
        // Not in middle of text:
        spans = parseStringToBlocks("Hi- Hey there").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "Hi- Hey there", false, false);
        spans = parseStringToBlocks("*Hi*- Hey there").get(0).getSpans();
        assertEquals(2, spans.size());
        assertTextSpanEquals(spans.get(0), "Hi", true, false);
        assertTextSpanEquals(spans.get(1), "- Hey there", false, false);
    }

    @Test
    public void spanEllipsisParseTest() {
        List<Span> spans;

        // In the middle of text.
        spans = parseStringToBlocks("Hello...there").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "Hello\u00A0.\u00A0.\u00A0.there", false, false);
        spans = parseStringToBlocks("Hello... there").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "Hello\u00A0.\u00A0.\u00A0. there", false, false);

        // End of text.
        spans = parseStringToBlocks("Hello...").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "Hello\u00A0.\u00A0.\u00A0.", false, false);

        // Beginning of text.
        spans = parseStringToBlocks("... there").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "\u00A0.\u00A0.\u00A0. there", false, false);

        // Before punctuation.
        spans = parseStringToBlocks("Hello...!").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "Hello\u00A0.\u00A0.\u00A0.\u00A0!", false, false);
    }

    @Test
    public void spanPunctuationParseTest() {
        List<Span> spans;

        // Period. No special parsing.
        spans = parseStringToBlocks("Hello.").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "Hello.", false, false);

        // Comma. No special parsing.
        spans = parseStringToBlocks("Hello, there.").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "Hello, there.", false, false);

        // Colon, semicolon, question mark, and exclamation mark: insert thin space in front.
        spans = parseStringToBlocks("This: that.").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "This\u202F: that.", false, false);
        spans = parseStringToBlocks("One thing; the other.").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "One thing\u202F; the other.", false, false);
        spans = parseStringToBlocks("Wow!").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "Wow\u202F!", false, false);
        spans = parseStringToBlocks("What?").get(0).getSpans();
        assertEquals(1, spans.size());
        assertTextSpanEquals(spans.get(0), "What\u202F?", false, false);
    }

    @Test
    public void pageOfTagTest() {
        List<Span> spans;

        // Parse PAGE-OF in middle of text.
        spans = parseStringToBlocks("Before [PAGE-OF abc] after").get(0).getSpans();
        assertEquals(3, spans.size());
        assertTextSpanEquals(spans.get(0), "Before ", false, false);
        assertTrue(spans.get(1) instanceof PageRefSpan);
        PageRefSpan pageRefSpan = (PageRefSpan) spans.get(1);
        assertEquals("abc", pageRefSpan.getName());
        assertTextSpanEquals(spans.get(2), " after", false, false);
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
