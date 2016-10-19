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
        horizontalList.addText("Hello world! Test app for trying various things with the typesetter.\u00A0This gives us more control than using Markdown.", font, fontSize);
        horizontalList.addEndOfParagraph();
        horizontalList.format(verticalList, pageWidth - 2*pageMargin);

        horizontalList = new HorizontalList();
        horizontalList.addText("The lower of the ", font, fontSize);
        VerticalList verticalList2 = new VerticalList();
        HorizontalList horizontalList2 = new HorizontalList();
        horizontalList2.addText("one", font, fontSize);
        verticalList2.addElement(horizontalList2.makeBox(0));
        horizontalList2 = new HorizontalList();
        horizontalList2.addText("twg", font, fontSize);
        verticalList2.addElement(horizontalList2.makeBox(0));
        verticalList2.println(System.out, "");
        horizontalList.addElement(verticalList2.makeBox(0));
        horizontalList.addText(" lines.", font, fontSize);
        horizontalList.addEndOfParagraph();
        horizontalList.format(verticalList, pageWidth - 2*pageMargin);

        verticalList.ejectPage();
        verticalList.println(System.out, "");

        // Add the vertical list to the PDF.
        typesetter.addVerticalListToPdf(verticalList, pdDoc, pageWidth, pageHeight, pageMargin);

        pdDoc.save("foo.pdf");
    }
}
