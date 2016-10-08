package com.teamten.typeset;

import com.google.common.base.Strings;
import com.teamten.hyphen.HyphenDictionary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
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
        elements = transformLigatures(elements, font, fontSize);

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

                    for (int i = 0; i < syllables.size(); i++) {
                        String syllable = syllables.get(i);

                        // Add syllable.
                        newElements.add(new Text(syllable, text.getFont(), text.getFontSize()));

                        // Add discretionary hyphen.
                        if (i < syllables.size() - 1) {
                            String preBreak;
                            if (syllable.endsWith("-")) {
                                // Hyphen already exists in word.
                                preBreak = "";
                            } else {
                                // Add implicit hyphen.
                                preBreak = "-";
                            }
                            newElements.add(new Discretionary(
                                    HBox.makeOnlyString(preBreak, text.getFont(), text.getFontSize()),
                                    HBox.makeOnlyString("", text.getFont(), text.getFontSize()),
                                    HBox.makeOnlyString("", text.getFont(), text.getFontSize()),
                                    Discretionary.HYPHEN_PENALTY));

                        }
                    }
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

    /**
     * Return a new list of elements with ligatures converted to their one-character form.
     */
    static List<Element> transformLigatures(List<Element> elements, Font font, float fontSize)
            throws IOException {

        // If it weren't for hyphenation, we'd just go through the elements and substitute the
        // ligatures in the Text elements. But a discretionary break can cut in the middle of
        // a ligature, such as in the word "dif-fi-cult", cutting the "ffi" ligature.
        //
        // So our plan is to:
        //
        // 1. Find patterns of Text/Discretionary/Text elements.
        // 2. Reconstruct the entire pre-break, post-break, and no-break text.
        // 3. Transform ligatures in each.
        // 4. Find common prefixes and suffixes.
        // 5. Reconstruct the Text/Discretionary/Text elements based on that.
        //
        // For example:
        //
        //   Original text: difficult
        //   Hyphenated:    dif-fi-cult
        //   As elements:   Text(dif)Discretionary(-,,)Text(fi)Discretionary(-,,)Text(cult)
        //   With "fi":     Text(dif)Discretionary(-,,)Text(`fi`)Discretionary(-,,)Text(cult)
        //   With "ffi":    Text(di)Discretionary(f-,`fi`,`ffi`)Discretionary(-,,)Text(cult)
        //
        // (The text in `backticks` above are ligature characters.

        // First, make a copy of the list. Use a linked list because we'll be popping and pushing
        // things on the front.
        Deque<Element> oldElements = new LinkedList<>(elements);
        elements = null;

        // Our new elements.
        List<Element> newElements = new ArrayList<>();

        // Go through each element.
        while (!oldElements.isEmpty()) {
            Element element = oldElements.removeFirst();
            if (element instanceof Text || element instanceof Discretionary) {
                // Look for a Text/Discretionary/Text pattern.
                Text beforeText;
                Discretionary discretionary;
                Text afterText;

                if (element instanceof Text) {
                    beforeText = (Text) element;
                    discretionary = (Discretionary) (oldElements.peekFirst() instanceof Discretionary ?
                            oldElements.removeFirst() : null);
                } else {
                    beforeText = null;
                    discretionary = (Discretionary) element;
                }
                afterText = (Text) (discretionary != null && oldElements.peekFirst() instanceof Text ?
                        oldElements.removeFirst() : null);

                // We now have one of the following cases:
                //
                //    text / null / null
                //    text / discretionary / null
                //    text / discretionary / text
                //    null / discretionary / null
                //    null / discretionary / text
                //
                // Sanity check.
                if (beforeText != null && afterText != null &&
                        (beforeText.getFont() != afterText.getFont() ||
                                beforeText.getFontSize() != afterText.getFontSize())) {

                    throw new IllegalStateException("before and after text fonts don't match");
                }

                // Generate the full prebreak, postbreak, and nobreak strings.
                String entirePreBreak = (beforeText == null ? "" : beforeText.getText()) +
                        (discretionary == null ? "" : discretionary.getPreBreak().getOnlyString());
                String entirePostBreak = (discretionary == null ? "" : discretionary.getPostBreak().getOnlyString()) +
                        (afterText == null ? "" : afterText.getText());
                String entireNoBreak = (beforeText == null ? "" : beforeText.getText()) +
                        (discretionary == null ? "" : discretionary.getNoBreak().getOnlyString()) +
                        (afterText == null ? "" : afterText.getText());

                // Substitute ligatures in all three.
                entirePreBreak = font.transformLigatures(entirePreBreak);
                entirePostBreak = font.transformLigatures(entirePostBreak);
                entireNoBreak = font.transformLigatures(entireNoBreak);

                // Find longest common prefix and suffix.
                String commonPrefix = Strings.commonPrefix(entirePreBreak, entireNoBreak);
                String commonSuffix = Strings.commonSuffix(entirePostBreak, entireNoBreak);

                // Find what's left, to put in the discretionary.
                String preBreak = entirePreBreak.substring(commonPrefix.length());
                String postBreak = entirePostBreak.substring(0, entirePostBreak.length() - commonSuffix.length());
                String noBreak = entireNoBreak.substring(commonPrefix.length(),
                        entireNoBreak.length() - commonSuffix.length());

                // Replace the Text elements.
                beforeText = commonPrefix.isEmpty() ? null : new Text(commonPrefix, font, fontSize);
                discretionary = discretionary == null ? null : new Discretionary(
                        HBox.makeOnlyString(preBreak, font, fontSize),
                        HBox.makeOnlyString(postBreak, font, fontSize),
                        HBox.makeOnlyString(noBreak, font, fontSize),
                        discretionary.getPenalty());
                afterText = commonSuffix.isEmpty() ? null : new Text(commonSuffix, font, fontSize);

                // Add the elements to the output.
                if (beforeText != null) {
                    newElements.add(beforeText);
                }
                if (discretionary != null) {
                    newElements.add(discretionary);
                }

                // The afterText must be processed again, potentially with the next discretionary, so put
                // it back in the input. Note that this means that its string will have the ligatures
                // substituted twice. This is fine.
                if (afterText != null) {
                    oldElements.addFirst(afterText);
                }
            } else {
                // Not text or discretionary, leave it as-is.
                newElements.add(element);
            }
        }

        return newElements;
    }

    /*

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
