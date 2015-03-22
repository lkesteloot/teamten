// Copyright 2015 Lawrence Kesteloot

import com.teamten.image.ImageUtils;
import com.teamten.util.Files;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Make a header background for the clock blog post.
 */
public class Card {
    private static final int DPI = 350;
    private static final int WIDTH = 1400;
    private static final int HEIGHT = 300;
    private static final Color BACKGROUND_COLOR = new Color(204, 108, 5);
    private static final double GEARS_SCALE = 0.008;
    private static final double GEARS_OFFSET_X_IN = 3.5;
    private static final double GEARS_OFFSET_Y_IN = -0.7;
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

    public static void main(String[] args) throws Exception {
        drawFront();
    }

    private static void drawFront() throws Exception {
        BufferedImage image = ImageUtils.make(WIDTH, HEIGHT, BACKGROUND_COLOR);

        // Front.
        Graphics2D g = ImageUtils.createGraphics(image);

        // Gears, assembly, diagram, and texture.
        drawBackground(g);

        g.dispose();

        ImageUtils.save(image, "cover.jpg");
    }

    private static void drawBackground(Graphics2D g) throws Exception {
        // Gears.
        drawGears(g, "gears.raw", false);

        // Texture.
        drawTexture(g);
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
