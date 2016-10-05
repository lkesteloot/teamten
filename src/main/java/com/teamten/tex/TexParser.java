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
import com.teamten.typeset.VerticalList;
import com.teamten.util.CodePoints;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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
                System.out.println("Got token: " + Command.toString(mToken));
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

    private void skipText(int expected) throws IOException {
        expect(expected);
        fetchToken();
    }

    private void expect(int expected) {
        if (mToken != expected) {
            throw new IllegalStateException("Expected " + Command.toString(expected) +
                    " but got " + Command.toString(mToken));
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

            if (mToken == -1) {
                return verticalList;
            }

            String keyword = Command.toKeyword(mToken);
            if (keyword != null) {
                switch (keyword) {
                    case "hbox":
                        fetchToken();
                        skipWhitespace();
                        skipText('{');
                        verticalList.addElement(parseHbox());
                        skipText('}');
                        break;

                    case "glue":
                        fetchToken();
                        skipWhitespace();
                        skipText('{');
                        verticalList.addElement(parseGlue(false));
                        skipText('}');
                        break;

                    default:
                        throw new IllegalStateException("not handled: " + keyword);
                }
            } else {
                throw new IllegalStateException("must have vertical element in vertical list: " + mToken);
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

            String keyword = Command.toKeyword(mToken);
            if (keyword == null) {
                elements.add(new Text(mToken, mFont, mFontSize));
                fetchToken();
            } else {
                switch (keyword) {
                    case "hbox":
                        fetchToken();
                        skipWhitespace();
                        skipText('{');
                        elements.add(parseHbox());
                        skipText('}');
                        break;

                    case "glue":
                        fetchToken();
                        skipWhitespace();
                        skipText('{');
                        elements.add(parseGlue(true));
                        skipText('}');
                        break;

                    case "penalty":
                        fetchToken();
                        long penalty = parseLong();
                        skipWhitespace();
                        elements.add(new Penalty(penalty));
                        break;

                    default:
                        throw new IllegalStateException("not handled: " + keyword);
                }
            }
        }

        return new HBox(elements);
    }

    /**
     * Reads a glue until a closing }. Does not eat the closing brace.
     */
    private Glue parseGlue(boolean isHorizontal) throws IOException {
        long amount = parseDistance();
        skipWhitespace();
        expect('}');

        return new Glue(amount, 0, 0, isHorizontal);
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
    private long parseDistance() throws IOException {
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

        // Read the second character of the unit.
        int ch1 = mToken;
        fetchToken();
        int ch2 = mToken;

        // See if we reached the end of the file.
        if (ch2 == -1) {
            throw new NumberFormatException("missing unit");
        }
        fetchToken();

        String unitString = new StringBuilder().appendCodePoint(ch1).appendCodePoint(ch2).toString();

        SpaceUnit unit;
        try {
            // Parse unit.
            unit = SpaceUnit.valueOf(unitString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Invalid unit.
            throw new NumberFormatException("unknown unit " + unitString);
        }

        return unit.toSp(value);
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
