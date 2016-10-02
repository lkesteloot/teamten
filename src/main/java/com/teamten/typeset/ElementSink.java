package com.teamten.typeset;

/**
 * Receives Element objects one at a time.
 */
public interface ElementSink {
    /**
     * Add one Element to the receiver.
     */
    void addElement(Element element);
}
