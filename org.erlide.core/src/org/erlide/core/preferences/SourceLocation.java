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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public final class SourceLocation extends CodePathEntry {
	private IPath directory;

	// TODO add compiler options

	public SourceLocation(final IPath directory) {
		super();
		Assert.isLegal(directory != null,
				"SourceLocation requires a non-null directory");
		this.directory = directory;
	}

	public SourceLocation(final IEclipsePreferences sn) {
		super();
		load(sn);
	}

	public IPath getDirectory() {
		return directory;
	}

	@Override
	public void load(final Preferences root) {
		directory = new Path(root.get(ProjectPreferencesConstants.DIRECTORY,
				null));
		Assert.isLegal(directory != null,
				"SourceLocation requires a non-null directory");
	}

	@Override
	public void store(final Preferences root) throws BackingStoreException {
		clearAll(root);
		root.put(ProjectPreferencesConstants.DIRECTORY, directory.toString());
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("SRC{").append(directory).append("}");
		return result.toString();
	}
}
