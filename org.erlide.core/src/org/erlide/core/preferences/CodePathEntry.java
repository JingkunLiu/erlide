/*******************************************************************************
 * Copyright (c) 2008 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.core.preferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public abstract class CodePathEntry {
	private static int gid = 1;

	public static synchronized int newId() {
		return gid++;
	}

	private final int id;

	public CodePathEntry() {
		id = newId();
	}

	public int getId() {
		return id;
	}

	public abstract void load(Preferences root) throws BackingStoreException;

	public abstract void store(Preferences root) throws BackingStoreException;

	public static void clearAll(final Preferences root)
			throws BackingStoreException {
		root.clear();
		for (final String n : root.childrenNames()) {
			root.node(n).removeNode();
		}
	}

}
