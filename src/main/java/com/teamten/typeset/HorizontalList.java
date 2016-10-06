package com.teamten.typeset;

import java.io.IOException;
import java.util.List;

/**
 * Accumulates elements in a horizontal list until a paragraph is finished, at which point a list of
 * elements is generated and added to a vertical list.
 */
public class HorizontalList extends ElementList {
    @Override
    protected HBox makeOutputBox(List<Element> elements) {
        return new HBox(elements);
    }

    @Override
    protected long getElementSize(Element element) {
        return element.getWidth();
    }

    /**
     * Add the specified text, in the specified font, to the horizontal list.
     */
    public void addText(String text, Font font, float fontSize) throws IOException {
        long spaceWidth = (long) (font.getSpaceWidth()*fontSize);

        // Roughly copy TeX:
        Glue spaceGlue = new Glue(spaceWidth, spaceWidth / 2, spaceWidth / 3, true);

        // Replace ligatures with Unicode values. TODO Not sure if we want to do this here with strings or
        // on the fly while we're rolling through code points. Seems weird to do it here, since a vertical
        // list constructed in another way wouldn't have it. This code should be Doc-related only.
        text = font.transformLigatures(text);

        int previousCh = 0;
        for (int i = 0; i < text.length(); ) {
            // Pick out the code point at this location. Could take two chars.
            int ch = text.codePointAt(i);

            // Advance to the next code point.
            i += Character.charCount(ch);

            if (ch == ' ') {
                addElement(spaceGlue);
            } else if (ch == '\u00A0') {
                // Non-break space. Precede with infinite penalty.
                addElement(new Penalty(Penalty.INFINITY));
                addElement(spaceGlue);

                // Pretend we're a space for the purposes of previousCh.
                ch = ' ';
            } else {
                // See if we need to kern.
                long kerning = font.getKerning(previousCh, ch, fontSize);
                if (kerning != 0) {
                    addElement(new Kern(kerning, true));
                }

                // Add the single character as a text node. TODO this makes for a large PDF since each
                // character is individually placed. Combine consecutive characters into text blocks.
                addElement(new Text(ch, font, fontSize));
            }

            previousCh = ch;
        }
    }

    /**
     * Adds the necessary glue and penalty to end a paragraph.
     */
    public void addEndOfParagraph() {
        // Add a forced break at the end of the paragraph.
        addElement(new Glue(0, 1, true, 0, false, true));
        addElement(new Penalty(-Penalty.INFINITY));
    }
}
