/*
 *
 *    Copyright 2016 Lawrence Kesteloot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.teamten.projects;

import com.teamten.image.ImageUtils;
import com.teamten.image.Typeface;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Make the cover for the André Klat book.
 */
public class KlatCover {
    // Text.
    private static final String AUTHOR = "ANDRÉ KLAT";
    private static final String TITLE = "LA FAMILLE KLAT";
    private static final String DATE = "1989";
    // Sizes.
    // DPI from http://en.wikipedia.org/wiki/List_of_displays_by_pixel_density
    private static final int SCREEN_DPI = 109; // Dell U2713HM (2560px / 23.5")
    private static final int PRINT_DPI = 300; // Print at 24% scale (72/300)
    private static final double BLEED_IN = 0.125;
    private static final double PAGE_WIDTH_IN = 6; // Not including bleed, including margin.
    private static final double PAGE_HEIGHT_IN = 9;
    private static final double SPINE_WIDTH_IN = 0.766; // Get from Lulu, depends on page count
    private static final double WIDTH_IN = 2*PAGE_WIDTH_IN + SPINE_WIDTH_IN + 2*BLEED_IN;
    private static final double HEIGHT_IN = PAGE_HEIGHT_IN + 2*BLEED_IN;
    // Red frame.
    private static final double FRAME_MARGIN_LEFT = 0.75;
    private static final double FRAME_MARGIN_RIGHT = 0.70;
    private static final double FRAME_MARGIN_TOP = 0.875;
    private static final double FRAME_MARGIN_BOTTOM = 0.875;
    private static final double FRAME_OUTSIDE_THICKNESS = 0.012;
    private static final double FRAME_PADDING = 0.0135;
    private static final double FRAME_INSIDE_THICKNESS = 0.006;
    // Author.
    private static final double AUTHOR_POS_IN = 1.5;
    private static final double AUTHOR_FONT_SIZE_IN = 0.18;
    // Title.
    private static final double TITLE_POS_IN = 3.2;
    private static final double TITLE_FONT_SIZE_IN = 0.37;
    // Date.
    private static final double DATE_POS_IN = 7.60;
    private static final double DATE_FONT_SIZE_IN = 0.14;
    // Spine lines.
    private static final double SPINE_LINE_WIDTH_IN = 0.006;
    private static final double SPINE_LINE1_POS_IN = FRAME_MARGIN_TOP;
    private static final double SPINE_LINE2_POS_IN = PAGE_HEIGHT_IN - FRAME_MARGIN_BOTTOM;
    // Spine text.
    private static final double SPINE_TEXT_FONT_SIZE_IN = 0.200;
    private static final double SPINE_AUTHOR_POS_IN = 2.25;
    private static final double SPINE_TITLE_POS_IN = 5.00;

    // Colors.
    private static final Color BACKGROUND_COLOR = Color.WHITE;
    private static final Color HIGHLIGHT_COLOR = new Color(0.8f, 0.0f, 0.0f, 1.0f);
    private static final Color DEBUG_COLOR = Color.GRAY;
    private static final Color BLACK_TEXT_COLOR = Color.BLACK;
    private static final Color LIGHT_YELLOW_COLOR = new Color(255, 236, 204);
    private static final Color DARK_YELLOW_COLOR = new Color(255, 218, 153);

    // Typefaces.
    private static final Typeface TYPEFACE = Typeface.MINION;

    // Flags.
    private static final boolean DRAW_BLEED = false;

    private final String mFilename;
    private final int mDpi;

    public static void main(String[] args) throws IOException, FontFormatException {
        int count = 0;
        int dpi = SCREEN_DPI;

        if (count < args.length && args[count].equals("--print")) {
            dpi = PRINT_DPI;
            count++;
        }

        if (count >= args.length) {
            System.err.println("Usage: make_cover [--print] filename");
            System.exit(-1);
        }

        new KlatCover(args[count], dpi).run();
    }

    private KlatCover(String filename, int dpi) {
        mFilename = filename;
        mDpi = dpi;
    }

    private void run() throws IOException, FontFormatException {
        int width = toPixels(WIDTH_IN);
        int height = toPixels(HEIGHT_IN);

        BufferedImage image = ImageUtils.make(width, height, BACKGROUND_COLOR);
        Graphics2D g = ImageUtils.createGraphics(image);

        int pageLeft = toPixels(BLEED_IN + PAGE_WIDTH_IN + SPINE_WIDTH_IN);
        int bleed = toPixels(BLEED_IN);
        int spineX = toPixels(BLEED_IN + PAGE_WIDTH_IN);
        int spineWidth = toPixels(SPINE_WIDTH_IN);

        // ----------------------------------------------------------------------------------
        // Background

        g.setColor(LIGHT_YELLOW_COLOR);
        g.fillRect(0, 0, width, height);
        int region = toPixels(1.0);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int dist = region;
                dist = Math.min(dist, width - bleed - x);
                dist = Math.min(dist, x - bleed);
                dist = Math.min(dist, height - bleed - y);
                dist = Math.min(dist, y - bleed);
                if (x < spineX) {
                    dist = Math.min(dist, spineX - x);
                } else if (x > spineX + spineWidth) {
                    dist = Math.min(dist, x - (spineX + spineWidth));
                } else {
                    dist = 0;
                }
                dist = Math.max(dist, 0);

                double t = (double) dist / region;

                t = t/2 + 0.5;

                Color c = ImageUtils.interpolateColor(DARK_YELLOW_COLOR, LIGHT_YELLOW_COLOR, t);
                image.setRGB(x, y, c.getRGB());
            }
        }


        // ----------------------------------------------------------------------------------
        // Front page

        // Frame. Use integer math to avoid round-off errors.
        int frameX = pageLeft + toPixels(FRAME_MARGIN_LEFT);
        int frameY = bleed + toPixels(FRAME_MARGIN_TOP);
        int frameWidth = toPixels(PAGE_WIDTH_IN - FRAME_MARGIN_LEFT - FRAME_MARGIN_RIGHT);
        int frameHeight = toPixels(PAGE_HEIGHT_IN - FRAME_MARGIN_TOP - FRAME_MARGIN_BOTTOM);
        int frameOutsideThickness = toPixels(FRAME_OUTSIDE_THICKNESS);
        int framePadding = toPixels(FRAME_PADDING);
        int frameInsideThickness = toPixels(FRAME_INSIDE_THICKNESS);
        drawRect(g, HIGHLIGHT_COLOR, frameOutsideThickness,
                frameX,
                frameY,
                frameWidth,
                frameHeight);
        drawRect(g, HIGHLIGHT_COLOR, frameInsideThickness,
                frameX + frameOutsideThickness + framePadding,
                frameY + frameOutsideThickness + framePadding,
                frameWidth - 2*(frameOutsideThickness + framePadding),
                frameHeight - 2*(frameOutsideThickness + framePadding));

        int y;
        Font font;
        TextLayout textLayout;
        int textWidth;
        int textHeight;

        // Author.
        font = ImageUtils.getFont(TYPEFACE, false, false, false, toPixels(AUTHOR_FONT_SIZE_IN));
        g.setColor(BLACK_TEXT_COLOR);
        g.setFont(font);
        textLayout = new TextLayout(AUTHOR, font, g.getFontRenderContext());
        textWidth = (int) textLayout.getBounds().getWidth();
        textHeight = (int) textLayout.getBounds().getHeight();
        y = toPixels(AUTHOR_POS_IN + BLEED_IN);
        textLayout.draw(g, frameX + frameWidth/2 - textWidth/2, y + textHeight/2);

        // Title.
        font = ImageUtils.getFont(TYPEFACE, false, false, false, toPixels(TITLE_FONT_SIZE_IN));
        g.setColor(HIGHLIGHT_COLOR);
        g.setFont(font);
        textLayout = new TextLayout(TITLE, font, g.getFontRenderContext());
        textWidth = (int) textLayout.getBounds().getWidth();
        textHeight = (int) textLayout.getBounds().getHeight();
        y = toPixels(TITLE_POS_IN + BLEED_IN);
        textLayout.draw(g, frameX + frameWidth/2 - textWidth/2, y + textHeight/2);

        // Date.
        font = ImageUtils.getFont(TYPEFACE, false, false, false, toPixels(DATE_FONT_SIZE_IN));
        g.setColor(BLACK_TEXT_COLOR);
        g.setFont(font);
        textLayout = new TextLayout(DATE, font, g.getFontRenderContext());
        textWidth = (int) textLayout.getBounds().getWidth();
        textHeight = (int) textLayout.getBounds().getHeight();
        y = toPixels(DATE_POS_IN + BLEED_IN);
        textLayout.draw(g, frameX + frameWidth/2 - textWidth/2, y + textHeight/2);

        // ----------------------------------------------------------------------------------
        // Spine

        // Red lines.
        g.setPaint(HIGHLIGHT_COLOR);
        g.fillRect(spineX, bleed + toPixels(SPINE_LINE1_POS_IN), spineWidth, toPixels(SPINE_LINE_WIDTH_IN));
        g.fillRect(spineX, bleed + toPixels(SPINE_LINE2_POS_IN), spineWidth, toPixels(SPINE_LINE_WIDTH_IN));

        // Get text parameters. Use the height of an "A" for centering.
        BufferedImage textImage;
        font = ImageUtils.getFont(TYPEFACE, false, false, false, toPixels(SPINE_TEXT_FONT_SIZE_IN));
        textLayout = new TextLayout("A", font, g.getFontRenderContext());
        textHeight = (int) textLayout.getBounds().getHeight();

        // Author.
        textImage = renderText(g, font, BLACK_TEXT_COLOR, AUTHOR);
        textImage = ImageUtils.rotate(textImage, -Math.PI/2, true);
        g.drawImage(textImage, spineX + spineWidth/2 + textHeight/2 - textImage.getWidth(),
                height - bleed - toPixels(SPINE_AUTHOR_POS_IN) - textImage.getHeight(), null);

        // Title.
        textImage = renderText(g, font, HIGHLIGHT_COLOR, TITLE);
        textImage = ImageUtils.rotate(textImage, -Math.PI/2, true);
        g.drawImage(textImage, spineX + spineWidth/2 + textHeight/2 - textImage.getWidth(),
                height - bleed - toPixels(SPINE_TITLE_POS_IN) - textImage.getHeight(), null);

        // ----------------------------------------------------------------------------------
        // Debug

        if (DRAW_BLEED) {
            g.setColor(DEBUG_COLOR);
            g.drawRect(toPixels(BLEED_IN), toPixels(BLEED_IN),
                    toPixels(WIDTH_IN - 2*BLEED_IN), toPixels(HEIGHT_IN - 2*BLEED_IN));
        }

        g.dispose();
        ImageUtils.save(image, mFilename);
    }

    /**
     * Draw text into an image, trimming the result.
     */
    private BufferedImage renderText(Graphics2D parentG, Font font, Color color, String text) {
        TextLayout textLayout = new TextLayout(text, font, parentG.getFontRenderContext());
        int width = (int) textLayout.getBounds().getWidth();
        int height = (int) textLayout.getBounds().getHeight();

        BufferedImage image = ImageUtils.makeTransparent(width*2, height*2);
        Graphics2D g = ImageUtils.createGraphics(image);

        g.setColor(color);
        g.setFont(font);
        textLayout.draw(g, width/2, height*3/2);
        g.dispose();

        image = ImageUtils.trim(image);

        return image;
    }

    /**
     * Draw the string at y so that the letters are evenly spread from x1 to x2.
     */
    private void justifyText(Graphics2D g, Font font, Color color,
                             String text, int x1, int x2, int y) {

        GlyphVector glyphVector = font.createGlyphVector(g.getFontRenderContext(), text);
        double textWidth = glyphVector.getLogicalBounds().getWidth();

        // Adjust kerning.
        double adjust = 0;
        if (text.length() != glyphVector.getNumGlyphs()) {
            throw new IllegalStateException("Glyph count is unexpected (" +
                    glyphVector.getNumGlyphs() + " vs. " + text.length() + ")");
        }
        Map<String,Double> kerning = new HashMap<String,Double>();
        kerning.put("LA", -0.00);
        kerning.put("AW", -0.10);
        kerning.put("NC", -0.10);
        kerning.put("CE", -0.05);
        for (int i = 1; i < glyphVector.getNumGlyphs(); i++) {
            String pair = text.substring(i - 1, i + 1);
            Double kern = kerning.get(pair);
            if (kern != null) {
                GlyphMetrics glyphMetrics = glyphVector.getGlyphMetrics(i);
                double delta = glyphMetrics.getAdvanceX()*kern;
                adjust += delta;
                textWidth += delta;
            }

            Point2D p = glyphVector.getGlyphPosition(i);
            p.setLocation(p.getX() + adjust, p.getY());
            glyphVector.setGlyphPosition(i, p);
        }

        // Spread out the letters.
        double padding = x2 - x1 - textWidth;
        double characterPadding = padding / (text.length() - 1);

        for (int i = 1; i < glyphVector.getNumGlyphs(); i++) {
            Point2D p = glyphVector.getGlyphPosition(i);
            p.setLocation(p.getX() + i*characterPadding, p.getY());
            glyphVector.setGlyphPosition(i, p);
        }

        g.setPaint(color);
        g.fill(glyphVector.getOutline(x1, y));
    }

    /**
     * Draws a rectangle outline of the given thickness.
     */
    private void drawRect(Graphics2D g, Color color, int thickness, int x, int y, int width, int height) {
        g.setPaint(color);

        g.fillRect(x, y, thickness, height);
        g.fillRect(x, y, width, thickness);
        g.fillRect(x + width - thickness, y, thickness, height);
        g.fillRect(x, y + height - thickness, width, thickness);
    }

    private int toPixels(double inches) {
        return (int) (inches*mDpi + 0.5);
    }
}
