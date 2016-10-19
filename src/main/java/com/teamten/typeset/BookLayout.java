package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Manages the overall layout of the book. This includes the position and format of the page numbers,
 * the section headers, etc.
 */
public class BookLayout {
    private final long mPageWidth;
    private final long mPageHeight;
    private final long mPageMargin;
    private final Font mPageNumberFont;
    private final float mPageNumberFontSize;

    public BookLayout(long pageWidth, long pageHeight, long pageMargin, Font pageNumberFont, float pageNumberFontSize) {
        mPageWidth = pageWidth;
        mPageHeight = pageHeight;
        mPageMargin = pageMargin;
        mPageNumberFont = pageNumberFont;
        mPageNumberFontSize = pageNumberFontSize;
    }

    public long getPageWidth() {
        return mPageWidth;
    }

    public long getPageHeight() {
        return mPageHeight;
    }

    public long getPageMargin() {
        return mPageMargin;
    }

    /**
     * The width of the text on the page.
     */
    public long getBodyWidth() {
        return mPageWidth - 2*mPageMargin;
    }

    /**
     * The height of the text on the page.
     */
    public long getBodyHeight() {
        return mPageHeight - 2*mPageMargin;
    }

    public void drawHeadline(Page page, PDPageContentStream contents) throws IOException {
        int physicalPageNumber = page.getPhysicalPageNumber();
        String pageNumberLabel = String.valueOf(physicalPageNumber);
        long x;
        // TODO pick nice vertical position:
        long y = mPageHeight - mPageMargin + PT.toSp(mPageNumberFontSize)*2;

        if (physicalPageNumber % 2 == 0) {
            // Even page, number on the left.
            x = mPageMargin;
        } else {
            // Odd page, number on the right.
            long labelWidth = mPageNumberFont.getStringMetrics(pageNumberLabel, mPageNumberFontSize).getWidth();
            x = mPageWidth - mPageMargin - labelWidth;
        }

        contents.beginText();
        contents.setFont(((PdfBoxFont) mPageNumberFont).getPdFont(), mPageNumberFontSize);
        contents.newLineAtOffset(PT.fromSpAsFloat(x), PT.fromSpAsFloat(y));
        contents.showText(pageNumberLabel);
        contents.endText();
    }
}
