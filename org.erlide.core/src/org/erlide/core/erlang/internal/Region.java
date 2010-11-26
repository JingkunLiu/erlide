/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.erlide.core.erlang.internal;

import java.util.ArrayList;

import org.erlide.core.erlang.IErlElement;
import org.erlide.core.erlang.IParent;
import org.erlide.core.erlang.IRegion;

/**
 * @see IRegion
 */

public class Region implements IRegion {

    /**
     * A collection of the top level elements that have been added to the region
     */
    protected ArrayList<IErlElement> fRootElements;

    /**
     * Creates an empty region.
     * 
     * @see IRegion
     */
    public Region() {
        fRootElements = new ArrayList<IErlElement>(1);
    }

    /**
     * @see IRegion#add(IErlElement)
     */
    public void add(final IErlElement element) {
        if (!contains(element)) {
            // "new" element added to region
            removeAllChildren(element);
            fRootElements.add(element);
            fRootElements.trimToSize();
        }
    }

    /**
     * @see IRegion
     */
    public boolean contains(final IErlElement element) {

        final int size = fRootElements.size();
        final ArrayList<IErlElement> parents = getAncestors(element);

        for (int i = 0; i < size; i++) {
            final IErlElement aTop = fRootElements.get(i);
            if (aTop.equals(element)) {
                return true;
            }
            for (int j = 0, pSize = parents.size(); j < pSize; j++) {
                if (aTop.equals(parents.get(j))) {
                    // an ancestor is already included
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a collection of all the parents of this element in bottom-up
     * order.
     * 
     */
    private ArrayList<IErlElement> getAncestors(final IErlElement element) {
        final ArrayList<IErlElement> parents = new ArrayList<IErlElement>();
        IErlElement parent = element.getParent();
        while (parent != null) {
            parents.add(parent);
            parent = parent.getParent();
        }
        parents.trimToSize();
        return parents;
    }

    /**
     * @see IRegion
     */
    public IErlElement[] getElements() {
        final int size = fRootElements.size();
        final IErlElement[] roots = new IErlElement[size];
        for (int i = 0; i < size; i++) {
            roots[i] = fRootElements.get(i);
        }

        return roots;
    }

    /**
     * @see IRegion#remove(IErlElement)
     */
    public boolean remove(final IErlElement element) {

        removeAllChildren(element);
        return fRootElements.remove(element);
    }

    /**
     * Removes any children of this element that are contained within this
     * region as this parent is about to be added to the region.
     * 
     * <p>
     * Children are all children, not just direct children.
     */
    private void removeAllChildren(final IErlElement element) {
        if (element instanceof IParent) {
            final ArrayList<IErlElement> newRootElements = new ArrayList<IErlElement>();
            for (int i = 0, size = fRootElements.size(); i < size; i++) {
                final IErlElement currentRoot = fRootElements.get(i);
                // walk the current root hierarchy
                IErlElement parent = currentRoot.getParent();
                boolean isChild = false;
                while (parent != null) {
                    if (parent.equals(element)) {
                        isChild = true;
                        break;
                    }
                    parent = parent.getParent();
                }
                if (!isChild) {
                    newRootElements.add(currentRoot);
                }
            }
            fRootElements = newRootElements;
        }
    }

    /**
     * Returns a printable representation of this region.
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        final IErlElement[] roots = getElements();
        buffer.append('[');
        for (int i = 0; i < roots.length; i++) {
            buffer.append(roots[i].getName());
            if (i < roots.length - 1) {
                buffer.append(", "); //$NON-NLS-1$
            }
        }
        buffer.append(']');
        return buffer.toString();
    }
}