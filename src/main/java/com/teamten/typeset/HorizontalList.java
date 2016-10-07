package com.teamten.typeset;

import com.teamten.hyphen.HyphenDictionary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
        addText(text, font, fontSize, null);
    }

    /**
     * Add the specified text, in the specified font, to the horizontal list.
     *
     * @param hyphenDictionary the dictionary to use for hyphenation, or null to skip hyphenation.
     */
    public void addText(String text, Font font, float fontSize, HyphenDictionary hyphenDictionary) throws IOException {
        // First, convert the single string to a sequence of elements, where each word
        // is a single Text element. There will be other elements, like glues and
        // penalties.
        List<Element> elements = textToWords(text, font, fontSize);

        // Second, go through the text elements and add discretionary hyphens.
        if (hyphenDictionary != null) {
            elements = hyphenate(elements, hyphenDictionary);
        }

        // Third, go through the text elements and replace the ligatures.
        /// elements = transformLigatures(elements, font, fontSize);

        // Finally, add kerning between and within text elements.
        //// elements = addKerning(elements, font, fontSize);

        // Add all the final elements to our horizontal list.
        for (Element element : elements) {
            element.println(System.out, "");
            addElement(element);
        }
    }

    /**
     * Take the single large string and break it into three kinds of elements: glue (for space and non-breaking
     * space); words; and sequences of non-word characters.
     */
    private static List<Element> textToWords(String text, Font font, float fontSize) throws IOException {
        List<Element> elements = new ArrayList<>();

        long spaceWidth = (long) (font.getSpaceWidth() * fontSize);

        // Roughly copy TeX. TODO think of caching this in the font.
        Glue spaceGlue = new Glue(spaceWidth, spaceWidth / 2, spaceWidth / 3, true);

        for (int i = 0; i < text.length(); ) {
            // Pick out the code point at this location. Could take two chars.
            int ch = text.codePointAt(i);

            // Advance to the next code point.
            i += Character.charCount(ch);

            if (ch == ' ') {
                elements.add(spaceGlue);
            } else if (ch == '\u00A0') {
                // Non-break space. Precede with infinite penalty.
                elements.add(new Penalty(Penalty.INFINITY));
                elements.add(spaceGlue);
            } else {
                StringBuilder word = new StringBuilder();
                word.appendCodePoint(ch);

                // See what kind of word or non-word we just ran into.
                boolean isWord = isWordCharacter(ch);

                // Look forward and grab all the letters of the word (or not word).
                while (i < text.length()) {
                    ch = text.codePointAt(i);
                    if (isWord != isWordCharacter(ch) || ch == ' ' || ch == '\u00A0') {
                        break;
                    }

                    i += Character.charCount(ch);
                    word.appendCodePoint(ch);
                }

                // Add the whole word at once.
                elements.add(new Text(word.toString(), font, fontSize));
            }
        }

        return elements;
    }

    /**
     * Return a modified copy of the element list with the words hyphenated, meaning that discretionary
     * breaks have been inserted. Any word to be hyphenated must be within a single Text element.
     */
    private static List<Element> hyphenate(List<Element> elements,HyphenDictionary hyphenDictionary) throws IOException {
        List<Element> newElements = new ArrayList<>();

        for (Element element : elements) {
            if (element instanceof Text) {
                Text text = (Text) element;
                String word = text.getText();

                if (word.length() > 0 && isWordCharacter(word.codePointAt(0))) {
                    List<String> syllables = hyphenDictionary.hyphenate(word);
                    System.out.print("The word " + word + " was hyphenated to: ");

                    for (int i = 0; i < syllables.size(); i++) {
                        String syllable = syllables.get(i);
                        System.out.print(syllable + " - ");
                        newElements.add(new Text(syllable, text.getFont(), text.getFontSize()));
                        if (i < syllables.size() - 1) {
                            if (syllable.endsWith("-")) {
                                newElements.add(Discretionary.EMPTY);
                            } else {
                                newElements.add(new Discretionary(
                                        new HBox(Arrays.asList(new Text("-", text.getFont(), text.getFontSize()))),
                                        HBox.EMPTY, HBox.EMPTY, Discretionary.HYPHEN_PENALTY));
                            }
                        }
                    }
                    System.out.println();
                } else {
                    // Not a word, leave it as-is.
                    newElements.add(element);
                }
            } else {
                // Just add any other type.
                newElements.add(element);
            }
        }

        return newElements;
    }

    /**
     * Whether the character can be part of a hyphenated word.
     */
    public static boolean isWordCharacter(int ch) {
        // Both kinds of apostrophes. TODO normalize this apostrophe stuff elsewhere.
        return Character.isLetter(ch) || ch == '-' || ch == '\'' || ch == '’';
    }

    /*
        // Replace ligatures with Unicode values. TODO Not sure if we want to do this here with strings or
        // on the fly while we're rolling through code points. Seems weird to do it here, since a vertical
        // list constructed in another way wouldn't have it. This code should be Doc-related only.
        text = font.transformLigatures(text);

    }        int previousCh = 0;

            previousCh = ch;
    // See if we need to kern.
    long kerning = font.getKerning(previousCh, ch, fontSize);
                if (kerning != 0) {
                        addElement(new Kern(kerning, true));
                        }

                    // Pretend we're a space for the purposes of previousCh.
                    ch = ' ';
    */

    /**
     * Adds the necessary glue and penalty to end a paragraph.
     */
    public void addEndOfParagraph() {
        // Add a forced break at the end of the paragraph.
        addElement(new Glue(0, 1, true, 0, false, true));
        addElement(new Penalty(-Penalty.INFINITY));
    }
}
