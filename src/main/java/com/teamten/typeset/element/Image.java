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

package com.teamten.typeset.element;

import com.teamten.font.FontManager;
import com.teamten.hyphen.HyphenDictionary;
import com.teamten.image.ImageUtils;
import com.teamten.markdown.Block;
import com.teamten.markdown.BlockType;
import com.teamten.typeset.Config;
import com.teamten.typeset.HorizontalList;
import com.teamten.typeset.OutputShape;
import com.teamten.typeset.ParagraphStyle;
import com.teamten.typeset.Typesetter;
import com.teamten.typeset.VerticalList;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.teamten.typeset.SpaceUnit.IN;
import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Represents an image to be inserted into the document.
 */
public class Image extends Box {
    private static final Path CACHE_DIR = Paths.get("image_cache");
    // Lulu recommends 300 DPI for images.
    private static final int IDEAL_DPI = 300;
    private static final double CAPTION_FONT_SCALE = 1.0;
    private final Path mImagePath;
    private final PDImageXObject mImageXObject;
    private final Box mCaption;

    private Image(Path imagePath, PDImageXObject imageXObject, Box caption, long width, long height, long depth) {
        super(width, height, depth);

        mImagePath = imagePath;
        mImageXObject = imageXObject;
        mCaption = caption;
    }

    /**
     * The caption as a box, or null if there's no caption.
     */
    public Box getCaption() {
        return mCaption;
    }

    /**
     * Loads an image as a new element. The size of the box is the aspect ratio of the image zoomed to fit in
     * the specified max width and height. The image is entirely height (no depth). In other words, the baseline
     * is at the bottom of the image.
     */
    public static Image load(Path imagePath, long maxWidth, long maxHeight,
                             Block block, Config config, FontManager fontManager,
                             HyphenDictionary hyphenDictionary,
                             PDDocument pdDoc) throws IOException {

        // Deal with the image.
        PDImageXObject imageXObject = PDImageXObject.createFromFileByExtension(imagePath.toFile(), pdDoc);

        // Get the native size of the image.
        long width = PT.toSp(imageXObject.getWidth());
        long height = PT.toSp(imageXObject.getHeight());

        // Fit in specified box.
        if (width*maxHeight > maxWidth*height) {
            // Sides will touch.
            height = height*maxWidth/width;
            width = maxWidth;
        } else {
            // Top and bottom will touch. Note that this will result in an HBox
            // that's too narrow and we'll get a warning. We don't currently handle
            // this because none of our images are tall enough to trigger it.
            width = width*maxHeight/height;
            height = maxHeight;
        }

        // Warn if the image is too low-res.
        int dpi = (int) (imageXObject.getWidth()/IN.fromSp(width) + 0.5);
        if (dpi < IDEAL_DPI) {
            System.out.println("Warning: Image " + imagePath.getFileName() + " is " + dpi +
                    " DPI, should be at least " + IDEAL_DPI + " DPI");
        }

        // Deal with the caption. Check type of block.
        if (block.getBlockType() != BlockType.CAPTION) {
            throw new IllegalArgumentException("Image caption block must be of type CAPTION");
        }

        ParagraphStyle paragraphStyle = ParagraphStyle.forBlock(block, null, config, fontManager);

        // Substitute the footnote font.
        paragraphStyle = paragraphStyle.withScaledFont(CAPTION_FONT_SCALE);

        // Make a vertical list for the footnote.
        VerticalList verticalList = new VerticalList();

        // Set the distance between baselines based on the paragraph's main font.
        verticalList.setBaselineSkip(paragraphStyle.getLeading());

        // Create a horizontal list for this paragraph.
        HorizontalList horizontalList = Typesetter.makeHorizontalListFromBlock(block, paragraphStyle, null, config,
                fontManager, hyphenDictionary, 0);

        // Break the horizontal list into HBox elements, adding them to the vertical list.
        long bodyWidth = config.getBodyWidth();
        OutputShape outputShape = OutputShape.fixed(bodyWidth);

        horizontalList.format(verticalList, outputShape);

        VBox caption = new VBox(verticalList.getElements());

        return new Image(imagePath, imageXObject, caption, width, height, 0);
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        contents.drawImage(mImageXObject,
                PT.fromSpAsFloat(x), PT.fromSpAsFloat(y),
                PT.fromSpAsFloat(getWidth()), PT.fromSpAsFloat(getHeight()));
        return super.layOutHorizontally(x, y, contents);
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        float height = PT.fromSpAsFloat(getHeight());
        contents.drawImage(mImageXObject,
                PT.fromSpAsFloat(x), PT.fromSpAsFloat(y) - height,
                PT.fromSpAsFloat(getWidth()), height);
        return super.layOutVertically(x, y, contents);
    }

    @Override
    public String toString() {
        return "Image{" + mImagePath.getFileName() + '}';
    }

    /**
     * Make a copy of this image, ensuring that its resolution is not unnecessarily high. Note that the
     * cached file will be recomputed if it's missing or old, but not if the resolution (width
     * or height) change. In that case the cache directory should be deleted.
     *
     * The cache is based on the filename. Two different files with the same filename will clash.
     *
     * @param origPath the path of the original (possibly high-res) image.
     * @param maxWidth the width of the space this image will fit in.
     * @param maxHeight the height of the space this image will fit in.
     * @return a path to the image to load instead (possibly equal to origPath).
     * @throws IOException if something goes wrong with processing the image.
     */
    public static Path preprocessImage(Path origPath, long maxWidth, long maxHeight) throws IOException {
        // Filename of the image.
        Path fileName = origPath.getFileName();

        // Find the cache file.
        Path newPath = CACHE_DIR.resolve(fileName);

        // See if cache file exists and is newer than the original.
        if (!Files.exists(newPath) || Files.getLastModifiedTime(origPath).compareTo(Files.getLastModifiedTime(newPath)) > 0) {
            // Make sure the cache directory exists.
            Files.createDirectories(CACHE_DIR);

            // Load the original image.
            BufferedImage origImage = ImageUtils.load(origPath.toString());

            // Convert to BGR for later processing.
            origImage = ImageUtils.convertType(origImage, BufferedImage.TYPE_3BYTE_BGR);

            // Convert our units to pixels.
            int maxWidthPixels = (int) (IN.fromSp(maxWidth)*IDEAL_DPI + 0.5);
            int maxHeightPixels = (int) (IN.fromSp(maxHeight)*IDEAL_DPI + 0.5);

            // Shrink to fit. If it's smaller, leave it.
            BufferedImage newImage = ImageUtils.shrinkToFit(origImage, maxWidthPixels, maxHeightPixels);

            // Convert to black and white so that we don't depend on the printer doing it.
            newImage = ImageUtils.convertType(newImage, BufferedImage.TYPE_BYTE_GRAY);

            System.out.printf("Resized image %s from %dx%d to %dx%d%n",
                    fileName,
                    origImage.getWidth(), origImage.getHeight(),
                    newImage.getWidth(), newImage.getHeight());

            // Save resized version.
            ImageUtils.save(newImage, newPath.toString());
        }

        return newPath;
    }
}
