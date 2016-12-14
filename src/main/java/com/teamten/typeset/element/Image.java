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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.nio.file.Path;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Represents an image to be inserted into the document.
 */
public class Image extends Box {
    private final Path mImagePath;
    private final PDImageXObject mImageXObject;
    private final HBox mCaption;

    private Image(Path imagePath, PDImageXObject imageXObject, HBox caption, long width, long height, long depth) {
        super(width, height, depth);

        mImagePath = imagePath;
        mImageXObject = imageXObject;
        mCaption = caption;
    }

    /**
     * The caption as a horizontal box, or null if there's no caption.
     */
    public HBox getCaption() {
        return mCaption;
    }

    /**
     * Loads an image as a new element. The size of the box is the aspect ratio of the image zoomed to fit in
     * the specified max width and height. The image is entirely height (no depth). In other words, the baseline
     * is at the bottom of the image.
     */
    public static Image load(Path imagePath, long maxWidth, long maxHeight,
                             HBox caption, PDDocument pdDoc) throws IOException {

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
            // Top and bottom will touch.
            width = width*maxHeight/height;
            height = maxHeight;
        }

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
}
