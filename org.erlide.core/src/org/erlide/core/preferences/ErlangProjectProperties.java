package org.erlide.core.preferences;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.erlide.jinterface.backend.RuntimeVersion;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.google.common.collect.Sets;

/**
 * Project properties.
 * 
 */
public final class ErlangProjectProperties {

	private RuntimeVersion requiredRuntimeVersion;

	private final Set<SourceLocation> sources = Sets.newHashSet();
	private Set<IPath> includes = Sets.newHashSet();
	private IPath output;
	private final Set<SourceLocation> testSources = Sets.newHashSet();
	private final Set<DependencyLocation> dependencies = Sets.newHashSet();
	private final Set<CodePathLocation> codePathOrder = Sets.newHashSet();
	// TODO add compiler options

	public ErlangProjectProperties() {
	}

	public Collection<SourceLocation> getSources() {
		return Collections.unmodifiableCollection(sources);
	}

	public Collection<SourceLocation> getTestSources() {
		return Collections.unmodifiableCollection(testSources);
	}

	public Collection<IPath> getIncludes() {
		return Collections.unmodifiableCollection(includes);
	}

	public IPath getOutput() {
		return output;
	}

	public void setOutput(IPath output) {
		this.output = output;
	}

	public Collection<DependencyLocation> getDependencies() {
		return Collections.unmodifiableCollection(dependencies);
	}

	public void addDependencies(Collection<DependencyLocation> locations) {
		dependencies.addAll(locations);
	}

	public void removeDependencies(Collection<DependencyLocation> locations) {
		dependencies.removeAll(locations);
	}

	public Collection<ProjectLocation> getProjectDependencies() {
		return PropertiesUtils.filter(dependencies, ProjectLocation.class);
	}

	public Collection<LibraryLocation> getLibraryDependencies() {
		return PropertiesUtils.filter(dependencies, LibraryLocation.class);
	}

	public RuntimeVersion getRequiredRuntimeVersion() {
		return requiredRuntimeVersion;
	}

	public void load(final Preferences root) throws BackingStoreException {
		output = new Path(root.get(ProjectPreferencesConstants.OUTPUT, "ebin"));
		requiredRuntimeVersion = new RuntimeVersion(root.get(
				ProjectPreferencesConstants.REQUIRED_BACKEND_VERSION, null));
		Collection<IPath> incs = PathSerializer.unpackCollection(root.get(
				ProjectPreferencesConstants.INCLUDES, "")));
		includes = Sets.newHashSet(incs);
		final Preferences srcNode = root
				.node(ProjectPreferencesConstants.SOURCES);
		sources.clear();
		for (final String src : srcNode.childrenNames()) {
			final IEclipsePreferences sn = (IEclipsePreferences) srcNode
					.node(src);
			final SourceLocation loc = new SourceLocation(sn);
			sources.add(loc);
		}
		final Preferences tstSrcNode = root
				.node(ProjectPreferencesConstants.TEST_SOURCES);
		testSources.clear();
		for (final String src : tstSrcNode.childrenNames()) {
			final IEclipsePreferences sn = (IEclipsePreferences) srcNode
					.node(src);
			final SourceLocation loc = new SourceLocation(sn);
			testSources.add(loc);
		}
	}

	public void store(final Preferences root) throws BackingStoreException {
		CodePathEntry.clearAll(root);
		root.put(ProjectPreferencesConstants.OUTPUT, output.toPortableString());
		if (requiredRuntimeVersion != null) {
			root.put(ProjectPreferencesConstants.REQUIRED_BACKEND_VERSION,
					requiredRuntimeVersion.toString());
		}
		root.put(ProjectPreferencesConstants.INCLUDES, PathSerializer
				.packCollection(includes));
		final Preferences srcNode = root
				.node(ProjectPreferencesConstants.SOURCES);
		for (final SourceLocation loc : sources) {
			loc.store(srcNode.node(Integer.toString(loc.getId())));
		}
		final Preferences tstSrcNode = root
				.node(ProjectPreferencesConstants.TEST_SOURCES);
		for (final SourceLocation loc : testSources) {
			loc.store(tstSrcNode.node(Integer.toString(loc.getId())));
		}

		root.flush();
	}

	public void setRequiredRuntimeVersion(RuntimeVersion runtimeVersion) {
		requiredRuntimeVersion = runtimeVersion;
	}

	public void addSources(Collection<SourceLocation> newSources) {
		sources.addAll(newSources);
	}

	public void addIncludes(Collection<IPath> newIncludes) {
		includes.addAll(newIncludes);
	}

	public void removeSources(Collection<SourceLocation> newSources) {
		sources.removeAll(newSources);
	}

	public void removeIncludes(Collection<IPath> newIncludes) {
		includes.removeAll(newIncludes);
	}
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("runtime    : ").append(requiredRuntimeVersion).append(
				"\n");
		result.append("sourceDirs  : ").append(sources).append("\n");
		result.append("includeDirs : ").append(includes).append("\n");
		result.append("output      : ").append(output).append("\n");
		result.append("testSources : ").append(testSources).append("\n");
		result.append("dependencies: ").append(dependencies).append("\n");
		return result.toString();
	}
}
