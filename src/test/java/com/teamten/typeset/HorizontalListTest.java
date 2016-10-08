package com.teamten.typeset;

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
        FontManager fontManager = new FontManager(pdDoc);
        Font font = fontManager.get(FontManager.FontName.TIMES_NEW_ROMAN);
        float fontSize = 11;

        MockVerticalList verticalList = new MockVerticalList();
        HorizontalList horizontalList = new HorizontalList();

        String s = "Hello there";
        long width = font.getStringMetrics(s, fontSize).getWidth();
        horizontalList.addElement(new Text(font, fontSize, s, width, PT.toSp(15), 0));
        horizontalList.addElement(new Glue(0, 1, true, 0, false, true));
        horizontalList.addElement(new Penalty(-Penalty.INFINITY));

        horizontalList.format(verticalList, IN.toSp(5));

        verticalList.assertSize(1);
        // TODO fill this out.

        pdDoc.close();
    }

    @Test
    public void ligatureTest() throws IOException {
        PDDocument pdDoc = new PDDocument();
        FontManager fontManager = new FontManager(pdDoc);
        Font font = fontManager.get(FontManager.FontName.TIMES_NEW_ROMAN);
        float fontSize = 11;

        // Fake font with "ffi" (and in fact all) ligatures.
        Font fakeFont = new DummyFont(new Ligatures());

        Discretionary hyphen = new Discretionary(
                HBox.makeOnlyString("-", font, fontSize),
                HBox.makeOnlyString("", font, fontSize),
                HBox.makeOnlyString("", font, fontSize),
                0);
        Discretionary empty = new Discretionary(
                HBox.makeOnlyString("", font, fontSize),
                HBox.makeOnlyString("", font, fontSize),
                HBox.makeOnlyString("", font, fontSize),
                0);

        // "difficult" with "fi" ligature. Does not affect discretionary.
        List<Element> origElements = Arrays.asList(
                new Text("dif", font, fontSize),
                hyphen,
                new Text("fi", font, fontSize),
                hyphen,
                new Text("cult", font, fontSize));

        List<Element> newElements = HorizontalList.transformLigatures(origElements, font, fontSize);
        assertEquals(5, newElements.size());
        assertEquals("dif", newElements.get(0).toTextString());
        assertEquals(hyphen, newElements.get(1));
        assertEquals("\uFB01", newElements.get(2).toTextString());
        assertEquals(hyphen, newElements.get(3));
        assertEquals("cult", newElements.get(4).toTextString());

        // "difficult" with "ffi" ligature.
        newElements = HorizontalList.transformLigatures(origElements, fakeFont, fontSize);
        assertEquals(4, newElements.size());
        assertEquals("di", newElements.get(0).toTextString());
        discretionaryEquals(newElements.get(1), "f-", "\uFB01", "\uFB03");
        discretionaryEquals(newElements.get(2), "-", "", "");
        assertEquals("cult", newElements.get(3).toTextString());

        // Make sure that simple empty discretionary breaks aren't removed.
        origElements = Arrays.asList(
                new Text("self-", font, fontSize),
                empty,
                new Text("help", font, fontSize));
        newElements = HorizontalList.transformLigatures(origElements, font, fontSize);
        assertEquals(origElements, newElements);

        pdDoc.close();
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
