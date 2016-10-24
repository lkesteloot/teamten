package com.teamten.typeset;

import com.teamten.font.FontManager;
import com.teamten.font.FontSize;
import com.teamten.font.FontVariant;
import com.teamten.font.PdfBoxFontManager;
import com.teamten.font.Typeface;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;

/**
 * Test app for trying various things with the typesetter. This gives us more control than using Markdown.
 */
public class TypesetterTest {
    public static void main(String[] args) throws IOException {
        Typesetter typesetter = new Typesetter();
        PDDocument pdDoc = new PDDocument();
        FontManager fontManager = new PdfBoxFontManager(pdDoc);

        Config config = Config.testConfig();
        FontSize font = fontManager.get(Typeface.TIMES_NEW_ROMAN, FontVariant.REGULAR, 11);
        BookLayout bookLayout = new BookLayout();

        VerticalList verticalList = new VerticalList();

        // Simple paragraph.
        HorizontalList horizontalList = new HorizontalList();
        horizontalList.addText("Hello world! Test app for trying various things with the typesetter.\u00A0This gives us more control than using Markdown.", font);
        horizontalList.addEndOfParagraph();
        horizontalList.format(verticalList, config.getBodyWidth());

        horizontalList = new HorizontalList();
        horizontalList.addText("The lower of the ", font);
        VerticalList verticalList2 = new VerticalList();
        HorizontalList horizontalList2 = new HorizontalList();
        horizontalList2.addText("one", font);
        verticalList2.addElement(horizontalList2.makeBox(0));
        horizontalList2 = new HorizontalList();
        horizontalList2.addText("twg", font);
        verticalList2.addElement(horizontalList2.makeBox(0));
        verticalList2.println(System.out, "");
        horizontalList.addElement(verticalList2.makeBox(0));
        horizontalList.addText(" lines.", font);
        horizontalList.addEndOfParagraph();
        horizontalList.format(verticalList, config.getBodyWidth());

        verticalList.ejectPage();
        verticalList.println(System.out, "");

        // Add the vertical list to the PDF.
        typesetter.addVerticalListToPdf(verticalList, config, bookLayout, fontManager, pdDoc);

        pdDoc.save("foo.pdf");
    }
}
