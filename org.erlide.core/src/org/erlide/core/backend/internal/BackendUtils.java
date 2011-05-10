package org.erlide.core.backend.internal;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.erlide.core.backend.BackendCore;
import org.erlide.core.common.SourcePathProvider;
import org.erlide.jinterface.ErlLogger;

import com.google.common.collect.Lists;

public class BackendUtils {

    public static Collection<SourcePathProvider> getSourcePathProviders()
            throws CoreException {
        // TODO should be cached and listening to plugin changes?
        final List<SourcePathProvider> result = Lists.newArrayList();
        final IConfigurationElement[] elements = BackendCore
                .getSourcepathConfigurationElements();
        for (final IConfigurationElement element : elements) {
            final SourcePathProvider provider = (SourcePathProvider) element
                    .createExecutableExtension("class");
            result.add(provider);
        }
        return result;
    }

    public static String getErlideNodeNameTag() {
        String fUniqueId;
        final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        final String location = root.getLocation().toPortableString();
        final String user = System.getProperty("user.name");
        final String timestamp = Long
                .toHexString(System.currentTimeMillis() & 0xFFFFFF);
        fUniqueId = Long.toHexString(location.hashCode() & 0xFFFFF) + "_"
                + user + "_" + timestamp;
        return fUniqueId.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    public static String getBeamModuleName(final String path) {
        return getBeamModuleName(new Path(path));
    }

    public static String getBeamModuleName(final IPath path) {
        if (path.getFileExtension() != null
                && "beam".equals(path.getFileExtension())) {
            return path.removeFileExtension().lastSegment();
        }
        return null;
    }

    public static Collection<String> getExtraSourcePaths(final IProject project) {
        final List<String> result = Lists.newArrayList();
        Collection<SourcePathProvider> spps;
        try {
            spps = getSourcePathProviders();
            for (final SourcePathProvider spp : spps) {
                final Collection<IPath> paths = spp
                        .getSourcePathsForModel(project);
                for (final IPath p : paths) {
                    result.add(p.toString());
                }
            }
        } catch (final CoreException e) {
            ErlLogger.error(e);
        }
        return result;
    }

    public static Collection<String> getExtraSourcePaths() {
        final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
                .getProjects();
        final List<String> result = Lists.newArrayList();
        for (final IProject project : projects) {
            result.addAll(getExtraSourcePaths(project));
        }
        return result;
    }

}
