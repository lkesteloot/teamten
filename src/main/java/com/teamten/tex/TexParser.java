package com.teamten.tex;

import com.teamten.typeset.FontManager;
import com.teamten.typeset.Typesetter;
import com.teamten.typeset.VerticalList;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.teamten.typeset.SpaceUnit.IN;

/**
 * A TeX-like parser. This isn't intended to be used for actually typesetting, it's mostly to make it easy
 * to test the typesetting package.
 */
public class TexParser {
    public static void main(String[] args) throws IOException {
        String texFilename = args[0];
        String pdfFilename = args[1];

        InputStream inputStream = new FileInputStream(texFilename);
        PDDocument pdDoc = new PDDocument();
        FontManager fontManager = new FontManager(pdDoc);

        long pageWidth = IN.toSp(6);
        long pageHeight = IN.toSp(9);
        long pageMargin = IN.toSp(1);

        TexParser texParser = new TexParser();
        Typesetter typesetter = new Typesetter();
        VerticalList verticalList = texParser.parse(inputStream, typesetter, fontManager, pageWidth, pageMargin);

        // Add the vertical list to the PDF.
        typesetter.addVerticalListToPdf(verticalList, pdDoc, pageWidth, pageHeight, pageMargin);

        pdDoc.save(pdfFilename);
    }

    private VerticalList parse(InputStream inputStream, Typesetter typesetter, FontManager fontManager,
                       long pageWidth, long pageHeight) throws IOException {

        VerticalList verticalList = new VerticalList();

        TexTokenizer tokenizer = new TexTokenizer(inputStream);

        Token token;
        while ((token = tokenizer.next()) != null) {
            System.out.println(token);
        }
        // Put parameters into fields.
        // TODO recursive descent parser of TeX code.

        return verticalList;
    }
}
