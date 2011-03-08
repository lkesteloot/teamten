// Copyright 2011 Lawrence Kesteloot

import com.teamten.image.ImageUtils;
import com.teamten.image.Typeface;
import com.teamten.util.Files;
import java.awt.Color;
import java.awt.font.TextLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Make a cover for the Graphics Engine book.
 */
public class Cover {
    /// private static final int DPI = 72;          // Screen
    private static final int DPI = 300;         // Print
    private static final boolean DRAW_SPINE_AND_BLEED = false;
    private static final double WIDTH_IN = 17.40;
    private static final double HEIGHT_IN = 11.25;
    private static final Color BACKGROUND_COLOR = new Color(204, 108, 5);
    private static final double BLEED_IN = 0.125;
    private static final double SPINE_WIDTH_IN = 0.153;
    private static final double SPINE_POS_IN = 8.63;
    private static final Color SPINE_COLOR = Color.GRAY;
    private static final double MARGIN_IN = 0.5;
    private static final double RIBBON_TOP_IN = 3;
    private static final double RIBBON_BOTTOM_IN = 6;
    private static final double RIBBON_SHADOW_IN = 0.25;
    private static final Color RIBBON_COLOR = new Color(3, 79, 128);
    private static final Color RIBBON_SHADOW_COLOR =
        ImageUtils.interpolateColor(RIBBON_COLOR, Color.BLACK, 0.4);
    private static final double DIAGRAM_X_IN = 8.8;
    private static final double DIAGRAM_Y_IN = -1;
    private static final Color DIAGRAM_COLOR = new Color(169, 58, 0);
    private static final double DIAGRAM_SCALE = 1.3;
    private static final double TITLE_PT = 240/300.;
    private static final double TITLE_POS_IN = 4;
    private static final Color TITLE_TEXT_COLOR = Color.WHITE; // new Color(253, 187, 36);
    private static final double AUTHOR_PT = 120/300.;
    private static final double AUTHOR1_POS_IN = 5.05;
    private static final double AUTHOR2_POS_IN = 5.55;
    private static final Color AUTHOR_TEXT_COLOR = TITLE_TEXT_COLOR;
    private static final double BLURB_PT = 60/300.;
    private static final double BLURB_POS_IN = 3.6;
    private static final double BLURB_LEADING_IN = 0.32;
    private static final Color BLURB_TEXT_COLOR = TITLE_TEXT_COLOR;
    private static final double CODE_PT = 100/300.;
    private static final double CODE_X_IN = MARGIN_IN*2;
    private static final double CODE_Y_IN = -0.20;
    private static final double CODE_LEADING_IN = 0.35;
    /// private static final Color CODE_TEXT_COLOR = new Color(255, 255, 0, 20);
    private static final Color CODE_TEXT_COLOR = new Color(0, 0, 0, 20);
    /// private static final Color CODE_TEXT_COLOR = new Color(214, 130, 20);
    private static final double GEARS_SCALE = 0.03;
    private static final double GEARS_OFFSET_X = 16;
    private static final double GEARS_OFFSET_Y = 4;
    private static final double GEARS_ROTATE_DEG = 30;
    private static final Color[] GEAR_COLORS = new Color[] {
        new Color(173, 59, 0),
        new Color(183, 69, 2),
        new Color(170, 65, 0),
        new Color(181, 74, 1),
        new Color(200, 96, 6),
        new Color(194, 91, 4),
        new Color(222, 152, 12),
        new Color(214, 130, 20),
        new Color(201, 105, 5)
    };
    private static final int TEXTURE_STRENGTH = 16;

    private static final int WIDTH = toPixels(WIDTH_IN);
    private static final int HEIGHT = toPixels(HEIGHT_IN);
    private static final int MARGIN = toPixels(MARGIN_IN);

    public static void main(String[] args) throws Exception {
        BufferedImage image = ImageUtils.make(WIDTH, HEIGHT, BACKGROUND_COLOR);

        // Front.
        Graphics2D g = ImageUtils.createGraphics(image);

        // Gears.
        drawGears(g, "gears.raw");

        // Assembly language.
        drawAssembly(g);

        // Diagram.
        drawDragram(g, "ge.model.png");

        // Texture.
        drawTexture(g);

        // Ribbon.
        g.setColor(RIBBON_COLOR);
        int ribbonTop = toPixels(RIBBON_TOP_IN); 
        int ribbonBottom = toPixels(RIBBON_BOTTOM_IN);
        g.fillRect(0, ribbonTop, WIDTH, ribbonBottom - ribbonTop);
        int shadowSize = toPixels(RIBBON_SHADOW_IN);
        for (int i = 0; i < shadowSize; i++) {
            double shadow = (double) i / shadowSize;
            shadow = Math.pow(shadow, 0.5);
            g.setColor(ImageUtils.interpolateColor(RIBBON_SHADOW_COLOR, RIBBON_COLOR, shadow));
            g.drawLine(0, ribbonTop + i, WIDTH, ribbonTop + i);
            g.drawLine(0, ribbonBottom - i, WIDTH, ribbonBottom - i);
        }

        // Title.
        Font font = ImageUtils.getFont(Typeface.HELVETICA, true, false, false, TITLE_PT*DPI);
        g.setColor(TITLE_TEXT_COLOR);
        g.setFont(font);
        TextLayout textLayout = new TextLayout("Graphics Engine", font, g.getFontRenderContext());
        int width = (int) textLayout.getBounds().getWidth();
        textLayout.draw(g, WIDTH - MARGIN - width, toPixels(TITLE_POS_IN));

        // Authors.
        font = ImageUtils.getFont(Typeface.HELVETICA, false, false, false, AUTHOR_PT*DPI);
        g.setColor(AUTHOR_TEXT_COLOR);
        g.setFont(font);
        textLayout = new TextLayout("Lawrence Kesteloot", font, g.getFontRenderContext());
        width = (int) textLayout.getBounds().getWidth();
        textLayout.draw(g, WIDTH - MARGIN - width, toPixels(AUTHOR1_POS_IN));
        textLayout = new TextLayout("Rob Wheeler", font, g.getFontRenderContext());
        width = (int) textLayout.getBounds().getWidth();
        textLayout.draw(g, WIDTH - MARGIN - width, toPixels(AUTHOR2_POS_IN));

        // Blurb.
        String[] blurb =
            ("This book outlines the design of the Graphics Engine (GE), a chip with an "
            + "optimized#instruction set for transforming and rasterizing 3-D graphics primitives "
            + "for low-end#machines. The GE\u2019s design is ideal for home and arcade "
            + "video game machines.#A general-purpose CPU and the GE share memory for "
            + "communication: The CPU#calculates the game dynamics and sets up a graphics "
            + "data structure (a display list of#3-D primitives) that the GE traverses and "
            + "rasterizes in a pipeline arrangement.#The GE has a RISC core with a back-end "
            + "specialized for rasterizing triangles.").split("#");

        font = ImageUtils.getFont(Typeface.HELVETICA, false, false, false, BLURB_PT*DPI);
        g.setColor(BLURB_TEXT_COLOR);
        g.setFont(font);
        for (int i = 0; i < blurb.length; i++) {
            textLayout = new TextLayout(blurb[i], font, g.getFontRenderContext());
            textLayout.draw(g, MARGIN, toPixels(BLURB_POS_IN + i*BLURB_LEADING_IN));
        }

        if (DRAW_SPINE_AND_BLEED) {
            g.setColor(SPINE_COLOR);

            // Spine.
            g.drawRect(toPixels(SPINE_POS_IN), 0, toPixels(SPINE_WIDTH_IN), HEIGHT);

            // Bleed.
            int bleed = toPixels(BLEED_IN);
            g.drawRect(bleed, bleed, WIDTH - 2*bleed, HEIGHT - 2*bleed);
        }

        g.dispose();

        ImageUtils.save(image, "cover.png");

        if (DPI > 72) {
            image = ImageUtils.scale(image, 72.0 / DPI);
            ImageUtils.save(image, "cover_small.png");
        }
    }

    private static void drawAssembly(Graphics2D g) throws Exception {
        String[] code = new String[] {
            "LD      A1, y",
            "LD      TMP, y+1",
            "SUB     A1, A1, TMP",
            "LD      B, x+1",
            "LD      TMP, x",
            "SUB     B, B, TMP",
            "ST      planes+12, B",
            "LD      INST, y+1",
            "LD      TMP, x",
            "MUL     C1, INST, TMP",
            "LD      INST, y",
            "LD      TMP, x+1",
            "MUL     TMP, INST, TMP",
            "SUB     C1, C1, TMP",
            "MUL     TMP, A1, X",
            "ADD     CC1, C1, TMP",
            "MUL     TMP, B, Y",
            "ADD     CC1, CC1, TMP",
            "LD      INST, x+0",
            "LD      TMP, y+2",
            "MUL     TMP, TMP, INST",
            "SUB     D2, R0, TMP",
            "LD      INST, x+2",
            "LD      TMP, y+0",
            "MUL     TMP, TMP, INST",
            "ADD     D2, D2, TMP",
            "ADD     DET, D1, D2",
            "TREESET X,Y,R0",
            "TREEIN  C1,A1",
            "TREEIN  C2,A2",
            "TREEIN  C3,A3",
            "TREEVAL ZC,ZA,0",
            "TREEVAL RC,RA,1",
            "TREEVAL GC,GA,2",
            "TREEVAL BC,BA,3",
            "TREEPUT",
        };

        Font font = ImageUtils.getFont(Typeface.COURIER, true, false, false, CODE_PT*DPI);
        g.setColor(CODE_TEXT_COLOR);
        g.setFont(font);
        for (int i = 0; i < code.length; i++) {
            TextLayout textLayout = new TextLayout(code[i], font, g.getFontRenderContext());
            textLayout.draw(g, toPixels(CODE_X_IN), toPixels(CODE_Y_IN + i*CODE_LEADING_IN));
        }
    }

    private static void drawGears(Graphics2D g, String filename) throws IOException {
        int colorIndex = 0;

        double sin = Math.sin(GEARS_ROTATE_DEG*Math.PI/180);
        double cos = Math.cos(GEARS_ROTATE_DEG*Math.PI/180);

        for (String line : Files.readLines(new File(filename))) {
            String[] fields = line.split(" ");

            if (fields[0].equals("circle")) {
                double ox = Double.parseDouble(fields[1])*GEARS_SCALE;
                double oy = Double.parseDouble(fields[2])*GEARS_SCALE;
                double r = Double.parseDouble(fields[3])*GEARS_SCALE;
                double x = ox*cos + oy*sin + GEARS_OFFSET_X;
                double y = ox*sin - oy*cos + GEARS_OFFSET_Y;
                g.setColor(BACKGROUND_COLOR);
                g.fillArc(toPixels(x - r), toPixels(y - r), toPixels(r*2), toPixels(r*2), 0, 360);
            } else if (fields[0].equals("polyline")) {
                int numPoints = (fields.length - 1)/2;
                int[] x = new int[numPoints];
                int[] y = new int[numPoints];

                for (int i = 0; i < numPoints; i++) {
                    double ox = Double.parseDouble(fields[i*2 + 1])*GEARS_SCALE;
                    double oy = Double.parseDouble(fields[i*2 + 2])*GEARS_SCALE;
                    x[i] = toPixels(ox*cos + oy*sin + GEARS_OFFSET_X);
                    y[i] = toPixels(ox*sin - oy*cos + GEARS_OFFSET_Y);
                }

                g.setColor(GEAR_COLORS[colorIndex++ % GEAR_COLORS.length]);
                g.fillPolygon(x, y, numPoints);
            } else {
                System.err.printf("Unknown command \"%s\" in raw file %s%n", fields[0], filename);
            }
        }
    }

    private static void drawDragram(Graphics2D g, String filename) throws IOException {
        BufferedImage diagram = ImageUtils.load(filename);
        diagram = ImageUtils.grayToTransparent(diagram, 255, 0);
        diagram = ImageUtils.scale(diagram, DPI/300.0*DIAGRAM_SCALE);
        BufferedImage solid = ImageUtils.make(diagram.getWidth(), diagram.getHeight(),
                DIAGRAM_COLOR);
        diagram = ImageUtils.clipToMask(solid, diagram);
        g.drawImage(diagram, toPixels(DIAGRAM_X_IN), toPixels(DIAGRAM_Y_IN), null);
    }

    /**
     * Draw a texture patten on top of the whole image.
     */
    private static void drawTexture(Graphics2D g) {
        Random random = new Random();

        for (int x = 0; x < WIDTH; x++) {
            g.setColor(new Color(0, 0, 0, random.nextInt(TEXTURE_STRENGTH)));
            g.drawLine(x, 0, x, HEIGHT);
        }
        for (int y = 0; y < HEIGHT; y++) {
            g.setColor(new Color(0, 0, 0, random.nextInt(TEXTURE_STRENGTH)));
            g.drawLine(0, y, WIDTH, y);
        }
    }

    private static int toPixels(double inches) {
        return (int) (inches*DPI + 0.5);
    }
}
