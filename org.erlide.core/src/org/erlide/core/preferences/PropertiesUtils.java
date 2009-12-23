/*******************************************************************************
 * Copyright (c) 2009 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available
 * at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.core.preferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.preferences.DependencyLocation.DependencyKind;
import org.erlide.jinterface.backend.RuntimeInfo;
import org.erlide.jinterface.backend.util.PreferencesUtils;
import org.erlide.jinterface.util.ErlLogger;
import org.erlide.runtime.backend.ErlideBackend;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public final class PropertiesUtils {

	public static ErlangProjectProperties convertOld(
			final OldErlangProjectProperties old) {
		ErlangProjectProperties result = new ErlangProjectProperties();
		result.setRequiredRuntimeVersion(old.getRuntimeVersion());
		if (!result.getRequiredRuntimeVersion().isDefined()) {
			final RuntimeInfo runtimeInfo = old.getRuntimeInfo();
			if (runtimeInfo != null) {
				result.setRequiredRuntimeVersion(runtimeInfo.getVersion());
			}
		}

		result.addSources(mkSources(old.getSourceDirs()));
		List<String> incs = old.getIncludeDirs();
		List<IPath> incsp = Lists.newArrayList();
		for (String inc : incs) {
			incsp.add(new Path(inc));
		}
		result.addIncludes(incsp);

		// now for the dependencies
		ErlideBackend b = ErlangCore.getBackendManager().getIdeBackend();
		final List<String> externalModules = ErlangCore.getExternalModules(b,
				"", ErlangCore.getExternal(old, ErlangCore.EXTERNAL_MODULES));
		final List<SourceLocation> sloc = makeExternalModules(externalModules);

		final List<String> externalIncludes = ErlangCore.getExternalModules(b,
				"", ErlangCore.getExternal(old, ErlangCore.EXTERNAL_INCLUDES));
		final List<IPath> iloc = makeExternalIncludes(externalIncludes);

		final LibraryLocation loc = new LibraryLocation(sloc, iloc, null,
				DependencyKind.COMPILE_RUN);
		ArrayList<DependencyLocation> locs = Lists.newArrayList();
		// TODO maybe group these according to path?
		if (sloc.size() > 0 || iloc.size() > 0) {
			locs.add(loc);
		}
		result.addDependencies(locs);
		// TODO add OTP dependency
		// TODO add also project dependencies
		return result;
	}

	private static List<IPath> makeExternalIncludes(List<String> exinc) {
		List<IPath> result = Lists.newArrayList();
		for (String str : exinc) {
			result.add(new Path(str));
		}
		return result;
	}

	private static List<SourceLocation> makeExternalModules(
			final List<String> externalModules) {
		final List<SourceLocation> result = Lists.newArrayList();

		final List<String> modules = Lists.newArrayList();
		for (final String mod : externalModules) {
			if (mod.endsWith(".erlidex")) {
				final List<String> mods = PreferencesUtils.readFile(mod);
				modules.addAll(mods);
			} else {
				modules.add(mod);
			}
		}

		final Map<IPath, List<String>> grouped = Maps.newHashMap();
		for (final String mod : modules) {
			final int i = mod.lastIndexOf('/');
			final String path = mod.substring(0, i);
			final String file = mod.substring(i + 1);

			ErlLogger.debug("FOUND: '" + path + "' '" + file + "'");
			List<String> pval = grouped.get(path);
			if (pval == null) {
				pval = Lists.newArrayList();
			}
			pval.add(file);
			grouped.put(new Path(path), pval);
		}
		ErlLogger.debug(grouped.toString());

		for (final Entry<IPath, List<String>> loc : grouped.entrySet()) {
			final SourceLocation location = new SourceLocation(new Path(loc
					.getKey()));
			result.add(location);
		}

		return result;
	}

	private static List<SourceLocation> mkSources(final Collection<IPath> list) {
		final List<SourceLocation> result = Lists.newArrayList();
		for (final IPath src : list) {
			result.add(new SourceLocation(new Path(src)));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <U, T extends U> Collection<T> filter(
			final Collection<U> list, final Class<T> class1) {
		final List<T> result = new ArrayList<T>();
		for (final U oo : list) {
			if (oo.getClass().equals(class1)) {
				result.add((T) oo);
			}
		}
		return Collections.unmodifiableCollection(result);
	}

	private PropertiesUtils() {
	}
}
