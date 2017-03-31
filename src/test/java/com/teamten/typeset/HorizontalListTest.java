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

package com.teamten.typeset;

import com.teamten.font.DummyFont;
import com.teamten.font.FontManager;
import com.teamten.font.SizedFont;
import com.teamten.font.FontVariant;
import com.teamten.font.PdfBoxFontManager;
import com.teamten.font.Typeface;
import com.teamten.typeset.element.Discretionary;
import com.teamten.typeset.element.Element;
import com.teamten.typeset.element.Glue;
import com.teamten.typeset.element.HBox;
import com.teamten.typeset.element.Kern;
import com.teamten.typeset.element.Penalty;
import com.teamten.typeset.element.Text;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.teamten.typeset.SpaceUnit.IN;
import static com.teamten.typeset.SpaceUnit.PT;
import static org.junit.Assert.*;

/**
 * Test for the HorizontalList class.
 */
public class HorizontalListTest {
    @Test
    public void wrapTest() throws IOException {
        PDDocument pdDoc = new PDDocument();
        FontManager fontManager = new PdfBoxFontManager(pdDoc);
        SizedFont font = new SizedFont(fontManager.get(Typeface.TIMES_NEW_ROMAN, FontVariant.REGULAR), 11);

        MockVerticalList verticalList = new MockVerticalList();
        HorizontalList horizontalList = new HorizontalList();

        String s = "Hello there";
        long width = font.getStringMetrics(s).getWidth();
        horizontalList.addElement(new Text(font, s, width, PT.toSp(15), 0));
        horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
        horizontalList.addElement(new Penalty(-Penalty.INFINITY));

        horizontalList.format(verticalList, IN.toSp(5));

        verticalList.assertSize(1);

        pdDoc.close();
    }

    @Test
    public void ligatureTest() throws IOException {
        PDDocument pdDoc = new PDDocument();
        FontManager fontManager = new PdfBoxFontManager(pdDoc);
        SizedFont font = new SizedFont(fontManager.get(Typeface.TIMES_NEW_ROMAN, FontVariant.REGULAR), 11);

        // Fake font with "ffi" (and in fact all) ligatures.
        SizedFont fakeFont = new SizedFont(new DummyFont(new Ligatures()), font.getSize());

        Discretionary hyphen = new Discretionary(
                HBox.makeOnlyString("-", font),
                HBox.makeOnlyString("", font),
                HBox.makeOnlyString("", font),
                0);
        Discretionary empty = new Discretionary(
                HBox.makeOnlyString("", font),
                HBox.makeOnlyString("", font),
                HBox.makeOnlyString("", font),
                0);

        // "difficult" with "fi" ligature. Does not affect discretionary.
        List<Element> origElements = Arrays.asList(
                new Text("dif", font),
                hyphen,
                new Text("fi", font),
                hyphen,
                new Text("cult", font));

        List<Element> newElements = HorizontalList.transformLigatures(origElements, font);
        assertEquals(5, newElements.size());
        assertEquals("dif", newElements.get(0).toTextString());
        assertEquals(hyphen, newElements.get(1));
        assertEquals("\uFB01", newElements.get(2).toTextString());
        assertEquals(hyphen, newElements.get(3));
        assertEquals("cult", newElements.get(4).toTextString());

        // "difficult" with "ffi" ligature. Complicates discretionary.
        newElements = HorizontalList.transformLigatures(origElements, fakeFont);
        assertEquals(4, newElements.size());
        assertEquals("di", newElements.get(0).toTextString());
        discretionaryEquals(newElements.get(1), "f-", "\uFB01", "\uFB03");
        discretionaryEquals(newElements.get(2), "-", "", "");
        assertEquals("cult", newElements.get(3).toTextString());

        // Make sure that simple empty discretionary breaks aren't removed.
        origElements = Arrays.asList(
                new Text("self-", font),
                empty,
                new Text("help", font));
        newElements = HorizontalList.transformLigatures(origElements, font);
        assertEquals(origElements, newElements);

        pdDoc.close();
    }

    @Test
    public void kerningTest() throws IOException {
        PDDocument pdDoc = new PDDocument();
        FontManager fontManager = new PdfBoxFontManager(pdDoc);
        SizedFont font = new SizedFont(fontManager.get(Typeface.TIMES_NEW_ROMAN, FontVariant.REGULAR), 11);

        // Test kerning internal to Text nodes and between them.
        List<Element> origElements = Arrays.asList(
                new Text("AV", font),
                new Text("A", font));

        List<Element> newElements = HorizontalList.addKerning(origElements, font);
        assertEquals(5, newElements.size());
        assertEquals("A", newElements.get(0).toTextString());
        assertEquals(Kern.class, newElements.get(1).getClass());
        assertEquals("V", newElements.get(2).toTextString());
        assertEquals(Kern.class, newElements.get(3).getClass());
        assertEquals("A", newElements.get(4).toTextString());

        // Test discretionary.
        origElements = Arrays.asList(
                new Text("A", font),
                new Discretionary(
                        HBox.makeOnlyString("Vo-", font),
                        HBox.makeOnlyString("V", font),
                        HBox.makeOnlyString("VoV", font),
                        0),
                new Text("A", font));

        newElements = HorizontalList.addKerning(origElements, font);
        // Expected result: T(A)D(H(KT(V)KT(o-)), H(T(V)), H(KT(V)KT(oV)))KT(A)
        // Where K is Kern, T(X) is Text, D(X,X,X) is Discretionary, and H(X) is HBox.
        assertEquals(4, newElements.size());
        assertEquals("A", newElements.get(0).toTextString());
        {
            Discretionary discretionary = (Discretionary) newElements.get(1);
            {
                List<Element> preBreak = discretionary.getPreBreak().getElements();
                assertEquals(4, preBreak.size());
                assertEquals(Kern.class, preBreak.get(0).getClass());
                assertEquals("V", preBreak.get(1).toTextString());
                assertEquals(Kern.class, preBreak.get(2).getClass());
                assertEquals("o-", preBreak.get(3).toTextString());
            }
            {
                HBox postBreak = discretionary.getPostBreak();
                assertEquals("V", postBreak.toTextString());
            }
            {
                List<Element> noBreak = discretionary.getNoBreak().getElements();
                assertEquals(4, noBreak.size());
                assertEquals(Kern.class, noBreak.get(0).getClass());
                assertEquals("V", noBreak.get(1).toTextString());
                assertEquals(Kern.class, noBreak.get(2).getClass());
                assertEquals("oV", noBreak.get(3).toTextString());
            }
        }
        assertEquals(Kern.class, newElements.get(2).getClass());
        assertEquals("A", newElements.get(3).toTextString());

        // Try just-hyphen discretionary. See all the special handling for this in addKerning().
        origElements = Arrays.asList(
                new Text("A", font),
                new Discretionary(
                        HBox.makeOnlyString("-", font),
                        HBox.makeOnlyString("", font),
                        HBox.makeOnlyString("", font),
                        0),
                new Text("V", font));

        newElements = HorizontalList.addKerning(origElements, font);
        // Expected result: T(A)D(H(T(-)), H(T()), H(K))T(V)
        // Where K is Kern, T(X) is Text, D(X,X,X) is Discretionary, and H(X) is HBox.
        assertEquals(3, newElements.size());
        assertEquals("A", newElements.get(0).toTextString());
        {
            Discretionary discretionary = (Discretionary) newElements.get(1);
            {
                HBox preBreak = discretionary.getPreBreak();
                assertEquals("-", preBreak.toTextString());
            }
            {
                HBox postBreak = discretionary.getPostBreak();
                assertEquals("", postBreak.toTextString());
            }
            {
                List<Element> noBreak = discretionary.getNoBreak().getElements();
                assertEquals(1, noBreak.size());
                assertEquals(Kern.class, noBreak.get(0).getClass());
            }
        }
        assertEquals("V", newElements.get(2).toTextString());
        Element.println(newElements, System.out, "");
    }

    /**
     * For unit testing, a quick way to check the contents of a discretionary break. Does not test
     * the penalty.
     * @param element this is of type "Element" so that a cast doesn't have to be done in the caller.
     * @param preBreak expected content of pre-break HBox.
     * @param postBreak expected content of post-break HBox.
     * @param noBreak expected content of no-break HBox.
     */
    private static void discretionaryEquals(Element element, String preBreak, String postBreak, String noBreak) {
        // Will throw if wrong type. That's fine.
        Discretionary discretionary = (Discretionary) element;

        // Check the individual components.
        assertEquals(preBreak, discretionary.getPreBreak().getOnlyString());
        assertEquals(postBreak, discretionary.getPostBreak().getOnlyString());
        assertEquals(noBreak, discretionary.getNoBreak().getOnlyString());
    }

    /**
     * For accumulating elements and testing them.
     */
    private static class MockVerticalList implements ElementSink {
        private final List<Element> mElements = new ArrayList<>();

        @Override
        public void addElement(Element element) {
            mElements.add(element);
        }

        public List<Element> getElements() {
            return mElements;
        }

        public void assertSize(int size) {
            assertEquals(size, mElements.size());
        }

        /**
         * Print the whole vertical list, for debugging.
         */
        public void println() {
            System.out.println("Vertical list:");
            for (int i = 0; i < mElements.size(); i++) {
                mElements.get(i).println(System.out, "    ");
            }
        }
    }
}
