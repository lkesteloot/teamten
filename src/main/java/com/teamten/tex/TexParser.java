package com.teamten.tex;

import com.teamten.typeset.Element;
import com.teamten.typeset.Font;
import com.teamten.typeset.FontManager;
import com.teamten.typeset.Glue;
import com.teamten.typeset.HBox;
import com.teamten.typeset.Penalty;
import com.teamten.typeset.SpaceUnit;
import com.teamten.typeset.Text;
import com.teamten.typeset.Typesetter;
import com.teamten.typeset.VBox;
import com.teamten.typeset.VerticalList;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.teamten.typeset.SpaceUnit.IN;

/**
 * A TeX-like parser. This isn't intended to be used for actually typesetting, it's mostly to make it easy
 * to test the typesetting package.
 */
public class TexParser {
    private final InputStream mInputStream;
    private final Typesetter mTypesetter;
    private final FontManager mFontManager;
    private final long mPageWidth;
    private final long mPageHeight;
    private final TexTokenizer mTexTokenizer;
    private final Font mFont;
    private final float mFontSize;
    private int mToken;

    public static void main(String[] args) throws IOException {
        String texFilename = args[0];
        String pdfFilename = args[1];

        InputStream inputStream = new FileInputStream(texFilename);
        PDDocument pdDoc = new PDDocument();
        FontManager fontManager = new FontManager(pdDoc);

        long pageWidth = IN.toSp(6);
        long pageHeight = IN.toSp(9);
        long pageMargin = IN.toSp(1);

        Typesetter typesetter = new Typesetter();
        TexParser texParser = new TexParser(inputStream, typesetter, fontManager, pageWidth, pageHeight);

        VerticalList verticalList = texParser.parseVerticalList(false);
        verticalList.println(System.out, "");

        // Add the vertical list to the PDF.
        typesetter.addVerticalListToPdf(verticalList, pdDoc, pageWidth, pageHeight, pageMargin);

        pdDoc.save(pdfFilename);
    }

    public TexParser(InputStream inputStream, Typesetter typesetter, FontManager fontManager, long pageWidth,
                     long pageHeight) throws IOException {

        mInputStream = inputStream;
        mTypesetter = typesetter;
        mFontManager = fontManager;
        mPageWidth = pageWidth;
        mPageHeight = pageHeight;

        mTexTokenizer = new TexTokenizer(mInputStream);
        fetchToken();

        if (false) {
            while (mToken != -1) {
                System.out.println("Got token: " + Token.toString(mToken));
                fetchToken();
            }
        }

        mFont = mFontManager.get(FontManager.FontName.TIMES_NEW_ROMAN);
        mFontSize = 11;
    }

    private void fetchToken() throws IOException {
        mToken = mTexTokenizer.next();
    }

    private void skipWhitespace() throws IOException {
        while (Character.isWhitespace(mToken)) {
            fetchToken();
        }
    }

    private void skip(int expected) throws IOException {
        expect(expected);
        fetchToken();
    }

    private void expect(int expected) {
        if (mToken != expected) {
            throw new IllegalStateException("Expected " + Token.toString(expected) +
                    " but got " + Token.toString(mToken));
        }
    }

    /**
     * Parse a vertical list.
     * @param internal whether the list is internal (within an HBox) or global. If internal, stops on a close
     *                 brace, not eating the brace. If global, stops on end of file.
     */
    private VerticalList parseVerticalList(boolean internal) throws IOException {
        VerticalList verticalList = new VerticalList();

        while (true) {
            skipWhitespace();

            if ((!internal && mToken == -1) || (internal && mToken == '}')) {
                return verticalList;
            }

            switch (mToken) {
                case Token.HBOX:
                    fetchToken();
                    skipWhitespace();
                    skip('{');
                    verticalList.addElement(parseHbox());
                    skip('}');
                    break;

                case Token.GLUE:
                    fetchToken();
                    skipWhitespace();
                    skip('{');
                    verticalList.addElement(parseGlue(false));
                    skip('}');
                    break;

                default:
                    throw new IllegalStateException("not handled: " + Token.toString(mToken));
            }
        }
    }

    /**
     * Reads a horizontal box until a closing }. Does not eat the close brace.
     */
    private HBox parseHbox() throws IOException {
        List<Element> elements = new ArrayList<>();

        while (true) {
            if (mToken == -1) {
                throw new IllegalStateException("unexpected end of file in hbox");
            }

            if (mToken == '}') {
                // End of HBox. Don't eat.
                break;
            }

            switch (mToken) {
                case Token.HBOX:
                    fetchToken();
                    skipWhitespace();
                    skip('{');
                    elements.add(parseHbox());
                    skip('}');
                    break;

                case Token.VBOX:
                    fetchToken();
                    skipWhitespace();
                    skip('{');
                    elements.add(parseVbox());
                    skip('}');
                    break;

                case Token.GLUE:
                    fetchToken();
                    skipWhitespace();
                    skip('{');
                    elements.add(parseGlue(true));
                    skip('}');
                    break;

                case Token.PENALTY:
                    fetchToken();
                    long penalty = parseLong();
                    skipWhitespace();
                    elements.add(new Penalty(penalty));
                    break;

                default:
                    if (Token.isCommand(mToken)) {
                        throw new IllegalStateException("not handled: " + Token.toString(mToken));
                    } else {
                        elements.add(new Text(mToken, mFont, mFontSize));
                        fetchToken();
                    }
            }
        }

        return new HBox(elements);
    }

    /**
     * Reads a vertical box until a closing }. Does not eat the close brace.
     */
    private VBox parseVbox() throws IOException {
        VerticalList verticalList = parseVerticalList(true);

        return new VBox(verticalList.getElements());
    }

    /**
     * Reads a glue until a closing }. Does not eat the closing brace.
     */
    private Glue parseGlue(boolean isHorizontal) throws IOException {
        Glue.Expandability amount = parseDistance();
        if (amount.isInfinite()) {
            throw new IllegalArgumentException("glue amount cannot be infinite");
        }

        Glue.Expandability plus;
        Glue.Expandability minus;
        skipWhitespace();
        if (mToken == Token.PLUS) {
            skip(Token.PLUS);
            plus = parseDistance();
            skipWhitespace();
        } else {
            plus = new Glue.Expandability(0, false);
        }
        if (mToken == Token.MINUS) {
            skip(Token.MINUS);
            minus = parseDistance();
            skipWhitespace();
        } else {
            minus = new Glue.Expandability(0, false);
        }
        expect('}');

        return new Glue(amount.getAmount(), plus, minus, isHorizontal);
    }

    /**
     * Parse a distance, such as "2in", "3.5 in", or "-2 mm". The number must be
     * parsable as a (possibly signed) double. The unit must be one of the ones
     * from this class, in upper or lower case, preceded by optional whitespace.
     * Only abbreviations are permitted (e.g., "inch" is left after "in" and
     * "centimeter" is rejected). The reader is left immediately after the
     * unit.
     *
     * @return the distance in scaled points.
     * @throws IOException from the Reader.
     * @throws NumberFormatException if the distance cannot be parsed.
     */
    private Glue.Expandability parseDistance() throws IOException {
        StringBuilder sb = new StringBuilder();

        skipWhitespace();

        // Read the double.
        while (mToken != -1) {
            // If it's part of a double, add it to our builder. Luckily none of
            // our units start with an "e".
            if (mToken == '-' || Character.isDigit((char) mToken) || mToken == 'e' || mToken == 'E' || mToken == '.') {
                sb.appendCodePoint(mToken);
            } else {
                // End of double.
                break;
            }
            fetchToken();
        }

        // See if we reached the end of the file.
        if (mToken == -1) {
            throw new NumberFormatException("missing unit");
        }

        // Parse the value. Might throw a NumberFormatException.
        double value = Double.parseDouble(sb.toString());

        // Skip whitespace.
        skipWhitespace();

        // See if we reached the end of the file.
        if (mToken == -1) {
            throw new NumberFormatException("missing unit");
        }

        // Read the next sequence of letters, which should be a unit.
        StringBuilder unitString = new StringBuilder();
        while (Character.isLetter(mToken)) {
            unitString.appendCodePoint(mToken);
            fetchToken();
        }

        if (unitString.length() == 0) {
            throw new NumberFormatException("missing unit");
        }

        Glue.Expandability expandability;

        if (unitString.toString().equals("inf")) {
            expandability = new Glue.Expandability((long) value, true);
        } else {
            SpaceUnit unit;
            try {
                // Parse unit.
                unit = SpaceUnit.valueOf(unitString.toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid unit.
                throw new NumberFormatException("unknown unit " + unitString);
            }

            expandability = new Glue.Expandability(unit.toSp(value), false);
        }

        return expandability;
    }

    /**
     * Parse a signed long, with optional leading whitespace.
     */
    private long parseLong() throws IOException {
        long value = 0;
        boolean isNegative = false;

        skipWhitespace();

        if (mToken == '-') {
            isNegative = true;
            fetchToken();
        } else if (mToken == '+') {
            fetchToken();
        }

        while (mToken >= '0' && mToken <= '9') {
            value = value*10 + (mToken - '0');
            fetchToken();
        }

        if (isNegative) {
            value = -value;
        }

        return value;
    }
}
