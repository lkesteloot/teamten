package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
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
        long width = font.getTextWidth(fontSize, s);
        horizontalList.addElement(new Text(font, fontSize, s, width, PT.toSp(15), 0));
        horizontalList.addElement(new Glue(0, 1, true, 0, false, true));
        horizontalList.addElement(new Penalty(-Penalty.INFINITY));

        horizontalList.format(verticalList, IN.toSp(5));

        verticalList.assertSize(1);
        verticalList.println();
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
