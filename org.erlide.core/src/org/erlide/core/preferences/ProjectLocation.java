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

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.erlide.core.erlang.ErlangCore;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public final class ProjectLocation extends DependencyLocation {
	private IProject project;
	private final ErlangProjectProperties otherProps;

	public ProjectLocation(final IProject project, final DependencyKind kind) {
		super(kind);
		Assert.isLegal(project != null,
				"ProjectLocation requires a non-null project");
		this.project = project;
		otherProps = ErlangCore.getModel().getErlangProject(project.getName())
				.getNewProperties();
	}

	public IProject getProject() {
		return project;
	}

	@Override
	public void load(final Preferences root) {
		final String projectName = root.get(
				ProjectPreferencesConstants.PROJECT, null);
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		project = workspace.getRoot().getProject(projectName);
		Assert.isLegal(project != null,
				"ProjectLocation requires a non-null project");
	}

	@Override
	public void store(final Preferences root) throws BackingStoreException {
		clearAll(root);
		root.put(ProjectPreferencesConstants.PROJECT, project.getName());
	}

	@Override
	public Collection<IPath> getIncludes() {
		return otherProps.getIncludes();
	}

	@Override
	public Collection<DependencyLocation> getDependencies() {
		return otherProps.getDependencies();
	}

	@Override
	public IPath getOutput() {
		return otherProps.getOutput();
	}

	@Override
	public Collection<SourceLocation> getSources() {
		return otherProps.getSources();
	}

	@Override
	public String toString() {
		return "PRJ{" + project + "}";
	}

}
