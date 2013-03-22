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
 * Make a business card.
 */
public class Card {
    private static final int DPI = 350;
    private static final boolean DRAW_BLEED = false;
    private static final double WIDTH_IN = 3.60;
    private static final double HEIGHT_IN = 2.10;
    private static final Color BACKGROUND_COLOR = new Color(204, 108, 5);
    private static final double BLEED_IN = 0.05;
    private static final Color BLEED_COLOR = Color.GRAY;
    private static final double MARGIN_IN = 0.16;
    private static final double RIBBON_TOP_IN = 0.5;
    private static final double RIBBON_BOTTOM_IN = 1.3;
    private static final double RIBBON_SHADOW_IN = 0.10;
    private static final Color RIBBON_COLOR = new Color(3, 79, 128);
    private static final Color RIBBON_SHADOW_COLOR =
        ImageUtils.interpolateColor(RIBBON_COLOR, Color.BLACK, 0.4);
    private static final boolean DRAW_DIAGRAM = true;
    private static final double DIAGRAM_X_IN = 1.4;
    private static final double DIAGRAM_Y_IN = -0.2;
    private static final Color DIAGRAM_COLOR = new Color(169, 58, 0);
    private static final double DIAGRAM_SCALE = 0.3;
    private static final double PERSON_NAME_PT = 80/300.;
    private static final double PERSON_NAME_POS_IN = RIBBON_TOP_IN + 0.30;
    private static final Color PERSON_NAME_TEXT_COLOR = Color.WHITE;
    private static final double COMPANY_PT = 50/300.;
    private static final double COMPANY_POS_IN = RIBBON_TOP_IN + 0.515;
    private static final Color COMPANY_TEXT_COLOR = PERSON_NAME_TEXT_COLOR;
    private static final double EMAIL_PT = 40/300.;
    private static final double EMAIL_POS_IN = RIBBON_TOP_IN + 0.70;
    private static final Color EMAIL_TEXT_COLOR = PERSON_NAME_TEXT_COLOR;
    private static final double CODE_PT = 0.08;
    private static final double CODE_X_IN = MARGIN_IN + 0.05;
    private static final double CODE_Y_IN = -0.00;
    private static final double CODE_LEADING_IN = 0.11;
    private static final Color CODE_TEXT_COLOR = new Color(0, 0, 0, 20);
    private static final double GEARS_SCALE = 0.008;
    private static final double GEARS_OFFSET_X_IN = 3.5;
    private static final double GEARS_OFFSET_Y_IN = 0.7;
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
        drawFront();
        drawBack();
    }

    private static void drawFront() throws Exception {
        BufferedImage image = ImageUtils.make(WIDTH, HEIGHT, BACKGROUND_COLOR);

        // Front.
        Graphics2D g = ImageUtils.createGraphics(image);

        // Gears, assembly, diagram, and texture.
        drawBackground(g);

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

        // Person name.
        Font font = ImageUtils.getFont(Typeface.HELVETICA, true, false, false, PERSON_NAME_PT*DPI);
        g.setColor(PERSON_NAME_TEXT_COLOR);
        g.setFont(font);
        TextLayout textLayout = new TextLayout("Lawrence Kesteloot", font, g.getFontRenderContext());
        int width = (int) textLayout.getBounds().getWidth();
        textLayout.draw(g, WIDTH - MARGIN - width, toPixels(PERSON_NAME_POS_IN));

        // Company.
        font = ImageUtils.getFont("Helvetica Neue LT Std/HelveticaNeueLTStd-Th.otf",
                COMPANY_PT*DPI);
        g.setColor(COMPANY_TEXT_COLOR);
        g.setFont(font);
        textLayout = new TextLayout("HeadCode", font, g.getFontRenderContext());
        width = (int) textLayout.getBounds().getWidth();
        textLayout.draw(g, WIDTH - MARGIN - width, toPixels(COMPANY_POS_IN));

        // Email address.
        font = ImageUtils.getFont("Helvetica Neue LT Std/HelveticaNeueLTStd-Th.otf",
                EMAIL_PT*DPI);
        g.setColor(EMAIL_TEXT_COLOR);
        g.setFont(font);
        textLayout = new TextLayout("lk@headcode.com", font, g.getFontRenderContext());
        width = (int) textLayout.getBounds().getWidth();
        textLayout.draw(g, WIDTH - MARGIN - width, toPixels(EMAIL_POS_IN));

        // Bleed.
        drawBleed(g);

        g.dispose();

        ImageUtils.save(image, "front.png");
    }

    private static void drawBack() throws Exception {
        // drawBackOption1();
        drawBackOption2();
    }

    private static void drawBackOption1() throws Exception {
        BufferedImage image = ImageUtils.make(WIDTH, HEIGHT, BACKGROUND_COLOR);

        // Front.
        Graphics2D g = ImageUtils.createGraphics(image);

        // Gears, assembly, diagram, and texture.
        drawBackground(g);

        // Ribbon.
        g.setColor(RIBBON_COLOR);
        int ribbonTop = toPixels(RIBBON_TOP_IN);
        int ribbonBottom = toPixels(RIBBON_BOTTOM_IN);
        g.fillRect(0, 0, WIDTH, ribbonTop);
        g.fillRect(0, ribbonBottom, WIDTH, HEIGHT - ribbonBottom);
        int shadowSize = toPixels(RIBBON_SHADOW_IN);
        for (int i = 0; i < shadowSize; i++) {
            double shadow = (double) i / shadowSize;
            shadow = Math.pow(shadow, 0.5);
            g.setColor(ImageUtils.interpolateColor(RIBBON_SHADOW_COLOR, RIBBON_COLOR, shadow));
            g.drawLine(0, ribbonTop - i, WIDTH, ribbonTop - i);
            g.drawLine(0, ribbonBottom + i, WIDTH, ribbonBottom + i);
        }

        // Bleed.
        drawBleed(g);

        g.dispose();

        ImageUtils.save(image, "back.png");
    }

    private static void drawBackOption2() throws Exception {
        BufferedImage image = ImageUtils.make(WIDTH, HEIGHT, BACKGROUND_COLOR);

        // Front.
        Graphics2D g = ImageUtils.createGraphics(image);

        // Background.
        g.setColor(RIBBON_COLOR);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Gears.
        drawGears(g, "gears.raw", true);

        // Bleed.
        drawBleed(g);

        // Flip horizontally to match the front.
        image = ImageUtils.flipHorizontally(image);

        g.dispose();

        ImageUtils.save(image, "back.png");
    }

    private static void drawBackground(Graphics2D g) throws Exception {
        // Gears.
        drawGears(g, "gears.raw", false);

        // Assembly language.
        drawAssembly(g);

        // Diagram.
        if (DRAW_DIAGRAM) {
            drawDragram(g, "ge.model.png");
        }

        // Texture.
        drawTexture(g);
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

    private static void drawGears(Graphics2D g, String filename, boolean backgroundOnly)
        throws IOException {

        int colorIndex = 0;

        double sin = Math.sin(GEARS_ROTATE_DEG*Math.PI/180);
        double cos = Math.cos(GEARS_ROTATE_DEG*Math.PI/180);

        for (String line : Files.readLines(new File(filename))) {
            String[] fields = line.split(" ");

            if (fields[0].equals("circle")) {
                double ox = Double.parseDouble(fields[1])*GEARS_SCALE;
                double oy = Double.parseDouble(fields[2])*GEARS_SCALE;
                double r = Double.parseDouble(fields[3])*GEARS_SCALE;
                double x = ox*cos + oy*sin + GEARS_OFFSET_X_IN;
                double y = ox*sin - oy*cos + GEARS_OFFSET_Y_IN;
                g.setColor(BACKGROUND_COLOR);
                if (backgroundOnly) {
                    g.drawArc(toPixels(x - r), toPixels(y - r), toPixels(r*2), toPixels(r*2), 0, 360);
                } else {
                    g.fillArc(toPixels(x - r), toPixels(y - r), toPixels(r*2), toPixels(r*2), 0, 360);
                }
            } else if (fields[0].equals("polyline")) {
                int numPoints = (fields.length - 1)/2;
                int[] x = new int[numPoints];
                int[] y = new int[numPoints];

                for (int i = 0; i < numPoints; i++) {
                    double ox = Double.parseDouble(fields[i*2 + 1])*GEARS_SCALE;
                    double oy = Double.parseDouble(fields[i*2 + 2])*GEARS_SCALE;
                    x[i] = toPixels(ox*cos + oy*sin + GEARS_OFFSET_X_IN);
                    y[i] = toPixels(ox*sin - oy*cos + GEARS_OFFSET_Y_IN);
                }

                g.setColor(GEAR_COLORS[colorIndex++ % GEAR_COLORS.length]);
                if (backgroundOnly) {
                    g.drawPolygon(x, y, numPoints);
                } else {
                    g.fillPolygon(x, y, numPoints);
                }
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

    private static void drawBleed(Graphics2D g) {
        if (DRAW_BLEED) {
            int bleed = toPixels(BLEED_IN);
            g.setColor(BLEED_COLOR);
            g.drawRect(bleed, bleed, WIDTH - 2*bleed, HEIGHT - 2*bleed);
        }
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
