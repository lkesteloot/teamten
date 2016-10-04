package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;

import static com.teamten.typeset.SpaceUnit.IN;

/**
 * Test app for trying various things with the typesetter. This gives us more control than using Markdown.
 */
public class TypesetterTest {
    public static void main(String[] args) throws IOException {
        Typesetter typesetter = new Typesetter();
        PDDocument pdDoc = new PDDocument();
        FontManager fontManager = new FontManager(pdDoc);

        long pageWidth = IN.toSp(6);
        long pageHeight = IN.toSp(9);
        long pageMargin = IN.toSp(1);

        Font font = fontManager.get(FontManager.FontName.TIMES_NEW_ROMAN);
        float fontSize = 11;

        VerticalList verticalList = new VerticalList();

        // Simple paragraph.
        HorizontalList horizontalList = new HorizontalList();
        typesetter.addTextToHorizontalList("Hello world! Test app for trying various things with the typesetter.\u00A0This gives us more control than using Markdown.", font, fontSize, horizontalList);
        typesetter.endOfParagraph(horizontalList);
        horizontalList.format(verticalList, pageWidth - 2*pageMargin);

        horizontalList = new HorizontalList();
        typesetter.addTextToHorizontalList("The lower of the ", font, fontSize, horizontalList);
        VerticalList verticalList2 = new VerticalList();
        HorizontalList horizontalList2 = new HorizontalList();
        typesetter.addTextToHorizontalList("one", font, fontSize, horizontalList2);
        verticalList2.addElement(horizontalList2.makeBox());
        horizontalList2 = new HorizontalList();
        typesetter.addTextToHorizontalList("twg", font, fontSize, horizontalList2);
        verticalList2.addElement(horizontalList2.makeBox());
        verticalList2.println(System.out, "");
        horizontalList.addElement(verticalList2.makeBox());
        typesetter.addTextToHorizontalList(" lines.", font, fontSize, horizontalList);
        typesetter.endOfParagraph(horizontalList);
        horizontalList.format(verticalList, pageWidth - 2*pageMargin);

        typesetter.ejectPage(verticalList);
        verticalList.println(System.out, "");

        // Add the vertical list to the PDF.
        typesetter.addVerticalListToPdf(verticalList, pdDoc, pageWidth, pageHeight, pageMargin);

        pdDoc.save("foo.pdf");
    }
}
