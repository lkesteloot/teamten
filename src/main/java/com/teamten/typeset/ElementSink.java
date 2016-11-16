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

package com.teamten.typeset;

import com.teamten.typeset.element.Element;

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
