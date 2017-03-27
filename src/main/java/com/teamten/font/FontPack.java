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

package com.teamten.font;

/**
 * A collection of fonts for a paragraph.
 */
public class FontPack {
    private final SizedFont mRegularFont;
    private final SizedFont mBoldFont;
    private final SizedFont mItalicFont;
    private final SizedFont mBoldItalicFont;
    private final SizedFont mSmallCapsFont;
    private final SizedFont mCodeFont;

    private FontPack(SizedFont regularFont, SizedFont boldFont, SizedFont italicFont, SizedFont boldItalicFont,
                     SizedFont smallCapsFont, SizedFont codeFont) {

        mRegularFont = regularFont;
        mBoldFont = boldFont;
        mItalicFont = italicFont;
        mBoldItalicFont = boldItalicFont;
        mSmallCapsFont = smallCapsFont;
        mCodeFont = codeFont;
    }

    /**
     * Create a font pack from a description of the regular font and the code font. The other fonts are derived.
     */
    public static FontPack create(FontManager fontManager, TypefaceVariantSize regularFontDesc, TypefaceVariantSize codeFontDesc) {
        return new FontPack(
                fontManager.get(regularFontDesc),
                fontManager.get(regularFontDesc.withVariant(FontVariant.BOLD)),
                fontManager.get(regularFontDesc.withVariant(FontVariant.ITALIC)),
                fontManager.get(regularFontDesc.withVariant(FontVariant.BOLD_ITALIC)),
                fontManager.get(regularFontDesc.withVariant(FontVariant.SMALL_CAPS)),
                fontManager.get(codeFontDesc)
        );
    }

    public SizedFont getRegularFont() {
        return mRegularFont;
    }

    public SizedFont getBoldFont() {
        return mBoldFont;
    }

    public SizedFont getItalicFont() {
        return mItalicFont;
    }

    public SizedFont getBoldItalicFont() {
        return mBoldItalicFont;
    }

    public SizedFont getSmallCapsFont() {
        return mSmallCapsFont;
    }

    public SizedFont getCodeFont() {
        return mCodeFont;
    }

    /**
     * Returns a new font pack with all the fonts tracked as specified. See
     * {@link TrackingFont} for the parameters.
     */
    public FontPack withTracking(double tracking, double kerning) {
        return new FontPack(
                TrackingFont.create(mRegularFont, tracking, kerning),
                TrackingFont.create(mBoldFont, tracking, kerning),
                TrackingFont.create(mItalicFont, tracking, kerning),
                TrackingFont.create(mBoldItalicFont, tracking, kerning),
                TrackingFont.create(mSmallCapsFont, tracking, kerning),
                TrackingFont.create(mCodeFont, tracking, kerning)
        );
    }

    /**
     * Return a new font pack with all the fonts scaled by the specified amount.
     */
    public FontPack withScaledFont(double scale) {
        return new FontPack(
                mRegularFont.withScaledSize(scale),
                mBoldFont.withScaledSize(scale),
                mItalicFont.withScaledSize(scale),
                mBoldItalicFont.withScaledSize(scale),
                mSmallCapsFont.withScaledSize(scale),
                mCodeFont.withScaledSize(scale));
    }
}
