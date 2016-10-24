package com.teamten.typeset.element;

import com.teamten.typeset.element.Element;

/**
 * The subset of Elements that are not discarded at the beginning of a new line. Also we can only break
 * at glue that immediately follows a non-discardable element.
 */
public abstract class NonDiscardableElement extends Element {
    // No fields or methods.
}
