package com.teamten.typeset;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a page in the output medium.
 */
public class Page {
    private final List<Element> mElements;

    public Page(List<Element> elements) {
        mElements = elements;
    }

    public List<Element> getElements() {
        return mElements;
    }
}
