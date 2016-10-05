package com.teamten.tex;

import com.teamten.typeset.Element;
import com.teamten.typeset.Font;
import com.teamten.typeset.FontManager;
import com.teamten.typeset.HBox;
import com.teamten.typeset.Text;
import com.teamten.typeset.Typesetter;
import com.teamten.typeset.VerticalList;
import com.teamten.util.CodePoints;
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

        mFont = mFontManager.get(FontManager.FontName.TIMES_NEW_ROMAN);
        mFontSize = 11;
    }

    private void fetchToken() throws IOException {
        mToken = mTexTokenizer.next();
    }

    private void skipWhitespace() throws IOException {
        do {
            fetchToken();
        } while (Character.isWhitespace(mToken));
    }

    private void skipText(int expected) throws IOException {
        if (mToken != expected) {
            throw new IllegalStateException("Expected " + (char) expected + " but got " + (char) mToken);
        }

        fetchToken();
    }

    /**
     * Parse a vertical list.
     * @param internal whether the list is internal (within an HBox) or global. If internal, stops on a close
     *                 brace, not eating the brace. If global, stops on end of file.
     */
    private VerticalList parseVerticalList(boolean internal) throws IOException {
        VerticalList verticalList = new VerticalList();

        while (true) {
            if (mToken == -1) {
                return verticalList;
            }

            if (Character.isWhitespace(mToken)) {
                // Skip it.
                fetchToken();
            } else {
                String keyword = Command.toKeyword(mToken);
                if (keyword != null) {
                    switch (keyword) {
                        case "hbox":
                            skipWhitespace();
                            skipText('{');
                            verticalList.addElement(parseHbox());
                            skipText('}');
                            break;
                    }
                } else {
                    throw new IllegalStateException("must have vertical element in vertical list: " + mToken);
                }
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
            if (keyword != null) {
                throw new IllegalStateException("not handled: " + keyword);
            }

            elements.add(new Text(mToken, mFont, mFontSize));

            fetchToken();
        }

        return new HBox(elements);
    }
}
