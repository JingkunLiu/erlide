/*******************************************************************************
 * Copyright (c) 2004 Eric Merritt and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/

package org.erlide.core.preferences;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.erlide.core.ErlangPlugin;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.jinterface.backend.RuntimeInfo;
import org.erlide.jinterface.backend.RuntimeVersion;
import org.erlide.jinterface.util.ErlLogger;
import org.osgi.service.prefs.BackingStoreException;

import com.google.common.collect.Lists;

public final class OldErlangProjectProperties implements
        IPreferenceChangeListener {

    private IProject project;

    private Collection<IPath> sourceDirs = PathSerializer
            .unpackList(ProjectPreferencesConstants.DEFAULT_SOURCE_DIRS);
    private IPath outputDir = new Path(
            ProjectPreferencesConstants.DEFAULT_OUTPUT_DIR);
    private Collection<IPath> includeDirs = PathSerializer
            .unpackList(ProjectPreferencesConstants.DEFAULT_INCLUDE_DIRS);
    private String externalIncludesFile = ProjectPreferencesConstants.DEFAULT_EXTERNAL_INCLUDES;
    private String externalModulesFile = ProjectPreferencesConstants.DEFAULT_EXTERNAL_MODULES;
    private RuntimeVersion runtimeVersion = new RuntimeVersion(
            ProjectPreferencesConstants.DEFAULT_RUNTIME_VERSION);
    private String runtimeName = null;

    public OldErlangProjectProperties() {
    }

    public OldErlangProjectProperties(final IProject prj) {
        super();
        project = prj;
        final IEclipsePreferences root = new ProjectScope(project)
                .getNode(ErlangPlugin.PLUGIN_ID);
        // TODO load() should not be in constructor!
        load(root);
    }

    public OldErlangProjectProperties load(final IEclipsePreferences node) {
        if (project == null) {
            return this;
        }

        if ("true".equals(System.getProperty("erlide.newprops"))) {
            final ErlangProjectProperties npp = new ErlangProjectProperties();
            try {
                npp.load((IEclipsePreferences) node.node("test"));
                npp.store((IEclipsePreferences) node.node("new_test"));
            } catch (final BackingStoreException e) {
                e.printStackTrace();
            }
        }
        String sourceDirsStr = node.get(
                ProjectPreferencesConstants.SOURCE_DIRS,
                ProjectPreferencesConstants.DEFAULT_SOURCE_DIRS);
        sourceDirs = PathSerializer.unpackList(sourceDirsStr);
        String includeDirsStr = node.get(
                ProjectPreferencesConstants.INCLUDE_DIRS,
                ProjectPreferencesConstants.DEFAULT_INCLUDE_DIRS);
        includeDirs = PathSerializer.unpackList(includeDirsStr);
        outputDir = new Path(node.get(ProjectPreferencesConstants.OUTPUT_DIR,
                ProjectPreferencesConstants.DEFAULT_OUTPUT_DIR));
        runtimeVersion = new RuntimeVersion(node.get(
                ProjectPreferencesConstants.RUNTIME_VERSION, null));
        runtimeName = node.get(ProjectPreferencesConstants.RUNTIME_NAME, null);
        if (!runtimeVersion.isDefined()) {
            if (runtimeName == null) {
                runtimeVersion = new RuntimeVersion(
                        ProjectPreferencesConstants.DEFAULT_RUNTIME_VERSION);
            } else {
                final RuntimeInfo ri = ErlangCore.getRuntimeInfoManager()
                        .getRuntime(runtimeName);
                if (ri != null) {
                    runtimeVersion = new RuntimeVersion(ri.getVersion());
                } else {
                    runtimeVersion = new RuntimeVersion(
                            ProjectPreferencesConstants.DEFAULT_RUNTIME_VERSION);
                }
            }
        }
        externalModulesFile = node.get(
                ProjectPreferencesConstants.PROJECT_EXTERNAL_MODULES,
                ProjectPreferencesConstants.DEFAULT_EXTERNAL_MODULES);
        externalIncludesFile = node.get(
                ProjectPreferencesConstants.EXTERNAL_INCLUDES,
                ProjectPreferencesConstants.DEFAULT_EXTERNAL_INCLUDES);
        return this;
    }

    public void store(final IEclipsePreferences node) {
        if (project == null) {
            return;
        }

		node.removePreferenceChangeListener(this);

		try {
			String pp = System.getProperty("erlide.newprops");
			if ("true".equals(pp)) {
				System.out.println("Store new props  // test");
				final ErlangProjectProperties npp = PropertiesUtils
						.convertOld(this);
				try {
					npp.store(node.node("test"));
				} catch (final BackingStoreException e) {
				}
			}

            node.put(ProjectPreferencesConstants.SOURCE_DIRS,
                    PathSerializer.packList(sourceDirs));
            node.put(ProjectPreferencesConstants.INCLUDE_DIRS,
                    PathSerializer.packList(includeDirs));
            node.put(ProjectPreferencesConstants.OUTPUT_DIR, outputDir.toString());
            node.put(ProjectPreferencesConstants.EXTERNAL_INCLUDES,
                    externalIncludesFile);
            if (runtimeVersion.isDefined()) {
                node.put(ProjectPreferencesConstants.RUNTIME_VERSION,
                        runtimeVersion.asMinor().toString());
            } else {
                node.remove(ProjectPreferencesConstants.RUNTIME_VERSION);
            }
            if (runtimeName != null) {
                node.put(ProjectPreferencesConstants.RUNTIME_NAME, runtimeName);
            } else {
                node.remove(ProjectPreferencesConstants.RUNTIME_NAME);
            }
            // TODO remove these later
            node.remove("backend_cookie");
            node.remove("backend_node");
            // end todo
            node.put(ProjectPreferencesConstants.PROJECT_EXTERNAL_MODULES,
                    externalModulesFile);

            try {
                node.flush();
            } catch (final BackingStoreException e1) {
            }
        } finally {
            node.addPreferenceChangeListener(this);
        }
    }

    public Collection<IPath> getIncludeDirs() {
        return Collections.unmodifiableCollection(includeDirs);
    }

    public void setIncludeDirs(final Collection<IPath> includeDirs2) {
        includeDirs = Lists.newArrayList(includeDirs2);
    }

    public IPath getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(final IPath dir) {
        if (!outputDir.equals(dir)) {
            // try {
            // final Backend b = ErlangCore.getBackendManager()
            // .getBuildBackend(project);
            // String p =
            // project.getLocation().append(outputDir).toString();
            // b.removePath(getUsePathZ(), p);
            //
            // p = project.getLocation().append(dir).toString();
            // b.addPath(getUsePathZ(), p);

            outputDir = dir;
            // } catch (final BackendException e) {
            // ErlLogger.warn(e);
            // }
        }
    }

    public Collection<IPath> getSourceDirs() {
        return Collections.unmodifiableCollection(sourceDirs);
    }

    public void setSourceDirs(final Collection<IPath> sourceDirs2) {
        sourceDirs = Lists.newArrayList(sourceDirs2);
    }

    public String buildCommandLine() {
        if (project != null) {
            final String incs = buildIncludeDirs(getIncludeDirs());
            return " -pa " + project.getLocation().append(outputDir) + incs;
        }
        return "";
    }

    public String buildIncludeDirs(final Collection<IPath> list) {
        final StringBuilder incs = new StringBuilder();
        for (IPath element : list) {
            final IPath loc = project.getLocation();
            ErlLogger.debug("* " + element);
            if (!element.isAbsolute()) {
                ErlLogger.debug("  not abs!");
                element = loc.append(element);
                ErlLogger.debug("  " + element);
            }
            incs.append(" -I").append(element.toString());
        }
        return incs.toString();
    }

    public void copyFrom(final OldErlangProjectProperties bprefs) {
        includeDirs = bprefs.includeDirs;
        sourceDirs = bprefs.sourceDirs;
        outputDir = bprefs.outputDir;
    }

    public String getExternalIncludesFile() {
        return externalIncludesFile;
    }

    public void setExternalIncludesFile(final String file) {
        externalIncludesFile = file;
    }

    public void setExternalModulesFile(final String externalModules) {
        this.externalModulesFile = externalModules;
    }

    public String getExternalModulesFile() {
        return externalModulesFile;
    }

    public RuntimeInfo getRuntimeInfo() {
        final RuntimeInfo runtime = ErlangCore.getRuntimeInfoManager()
                .getRuntime(runtimeVersion, runtimeName);
        RuntimeInfo rt = null;
        if (runtime != null) {
            rt = RuntimeInfo.copy(runtime, false);
        }
        return rt;
    }

    public boolean hasSourceDir(final IPath fullPath) {
        final IPath f = fullPath.removeFirstSegments(1);
        for (final IPath s : getSourceDirs()) {
            if (s.equals(f)) {
                return true;
            }
            if (fullPath.segmentCount() == 1 && s.toString().equals(".")) {
                return true;
            }
        }
        return false;
    }

    public RuntimeVersion getRuntimeVersion() {
        return runtimeVersion;
    }

    public void preferenceChange(final PreferenceChangeEvent event) {

        System.out.println("PROP CHANGE DETECTED IN OLDPREFS "+event);

        final IEclipsePreferences root = new ProjectScope(project)
                .getNode(ErlangPlugin.PLUGIN_ID);
        load(root);
    }

    public void setRuntimeVersion(final RuntimeVersion runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("runtime    : ").append(runtimeVersion).append(" > ")
				.append(runtimeName).append("\n");
		result.append("sourceDirs : ").append(sourceDirs).append("\n");
		result.append("includeDirs: ").append(includeDirs).append("\n");
		result.append("output     : ").append(outputDir).append("\n");
		result.append("externalInc: ").append(externalIncludesFile).append(
				" ... dump content!").append("\n");
		result.append("externalMod: ").append(externalModulesFile).append(
				" ... dump content!").append("\n");
		return result.toString();
	}
}
