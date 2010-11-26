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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public final class LibraryLocation extends DependencyLocation {
	private List<SourceLocation> sources = new ArrayList<SourceLocation>();
	private List<IPath> includes = new ArrayList<IPath>();
	private final IPath output;

	/**
	 * 
	 * @param sources
	 * @param includes
	 * @param output
	 *            location of binaries. May be null, meaning that the location
	 *            is included in code path by some external means.
	 * @param kind
	 */
	public LibraryLocation(final List<SourceLocation> sources,
			final List<IPath> includes, final IPath output,
			final DependencyKind kind) {
		super(kind);
		if (sources != null) {
			this.sources = sources;
		}
		if (includes != null) {
			this.includes = includes;
		}
		this.output = output;
	}

	@Override
	public Collection<SourceLocation> getSources() {
		return Collections.unmodifiableCollection(sources);
	}

	@Override
	public Collection<IPath> getIncludes() {
		return Collections.unmodifiableCollection(includes);
	}

	@Override
	public IPath getOutput() {
		return output;
	}

	@Override
	public Collection<DependencyLocation> getDependencies() {
		return Collections.emptyList();
	}

	@Override
	public void load(final Preferences root) {
		// TODO implement
	}

	@Override
	public void store(final Preferences root) throws BackingStoreException {
		clearAll(root);
		root.put(ProjectPreferencesConstants.OUTPUT, output.toPortableString());
		final IEclipsePreferences node = (IEclipsePreferences) root
				.node(ProjectPreferencesConstants.SOURCES);
		for (final SourceLocation loc : sources) {
			loc.store((IEclipsePreferences) node.node(loc.getDirectory()
					.toString()));
		}
		root.put(ProjectPreferencesConstants.INCLUDES, PathSerializer
				.packCollection(includes));
		root.flush();
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("LIB{").append(sources).append(", ").append(includes)
				.append(", ").append(output).append("}");
		return result.toString();
	}

}
