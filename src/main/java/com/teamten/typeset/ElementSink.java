package com.teamten.typeset;

import java.util.List;

/**
 * Receives Element objects one at a time.
 */
public interface ElementSink {
    /**
     * Add one Element to the receiver.
     */
    void addElement(Element element);

    /**
     * Convenience method for making a sink that writes to a list.
     * @param list the list to write elements to.
     * @param elementClass the class of the elements being put into the list.
     */
    @SuppressWarnings("unchecked") // See comment below.
    static <T extends Element> ElementSink listSink(List<T> list, Class<T> elementClass) {
        return element -> {
            if (elementClass.isInstance(element)) {
                // This gives a warning, but in fact we've checked that it's the right class.
                list.add((T) element);
            } else {
                throw new IllegalArgumentException("element " + element + " is not of correct class");
            }
        };
    }
}
