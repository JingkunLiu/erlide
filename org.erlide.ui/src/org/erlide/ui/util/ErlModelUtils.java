/*******************************************************************************
 * Copyright (c) 2005 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.ui.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlElement;
import org.erlide.core.erlang.IErlElement.Kind;
import org.erlide.core.erlang.IErlFunction;
import org.erlide.core.erlang.IErlImport;
import org.erlide.core.erlang.IErlModel;
import org.erlide.core.erlang.IErlModule;
import org.erlide.core.erlang.IErlPreprocessorDef;
import org.erlide.core.erlang.IErlProject;
import org.erlide.core.erlang.IErlTypespec;
import org.erlide.core.erlang.util.ContainerFilter;
import org.erlide.core.erlang.util.ErlangFunction;
import org.erlide.core.erlang.util.ErlangIncludeFile;
import org.erlide.core.erlang.util.ErlideUtil;
import org.erlide.core.erlang.util.ModelUtils;
import org.erlide.core.erlang.util.PluginUtils;
import org.erlide.core.erlang.util.ResourceUtil;
import org.erlide.jinterface.backend.Backend;
import org.erlide.jinterface.util.ErlLogger;
import org.erlide.ui.editors.util.EditorUtility;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangTuple;

import erlang.ErlideOpen;

public class ErlModelUtils {

	private static final ArrayList<OtpErlangObject> NO_IMPORTS = new ArrayList<OtpErlangObject>(
			0);

	public static List<String> getExternalModules(final Backend b,
			final String prefix, final String externalModules) {
		return ErlideOpen.getExternalModules(b, prefix, externalModules,
				ErlangCore.getModel().getPathVars());
	}

	public static IErlModule getModule(final IEditorPart editor) {
		if (editor == null || !(editor instanceof AbstractDecoratedTextEditor)) {
			return null;
		}
		final AbstractDecoratedTextEditor adte = (AbstractDecoratedTextEditor) editor;
		return getModule(editor.getEditorInput(), adte.getDocumentProvider());
	}

	public static List<IErlPreprocessorDef> getPreprocessorDefs(
			final Backend b, final IProject project, final IErlModule module,
			final IErlElement.Kind kind, final String externalIncludes) {
		final List<IErlPreprocessorDef> res = new ArrayList<IErlPreprocessorDef>();
		final List<IErlModule> modulesFound = new ArrayList<IErlModule>(1);
		List<IErlModule> modulesWithIncludes = modulesFound;
		try {
			modulesWithIncludes = getModulesWithIncludes(b, project, module,
					externalIncludes, modulesFound);
		} catch (final CoreException e) {
			ErlLogger.warn(e);
		}
		for (final IErlModule m : modulesWithIncludes) {
			res.addAll(m.getPreprocessorDefs(kind));
		}
		return res;
	}

	public static List<OtpErlangObject> getImportsAsList(final IErlModule mod) {
		if (mod == null) {
			return NO_IMPORTS;
		}
		final Collection<IErlImport> imports = mod.getImports();
		if (imports.isEmpty()) {
			return NO_IMPORTS;
		}
		final List<OtpErlangObject> result = new ArrayList<OtpErlangObject>(
				imports.size());
		for (final IErlImport i : imports) {
			final List<ErlangFunction> functions = i.getFunctions();
			final OtpErlangObject funsT[] = new OtpErlangObject[functions
					.size()];
			int j = 0;
			for (final ErlangFunction f : functions) {
				funsT[j] = f.getNameArityTuple();
				j++;
			}
			final OtpErlangTuple modFunsT = new OtpErlangTuple(
					new OtpErlangObject[] {
							new OtpErlangAtom(i.getImportModule()),
							new OtpErlangList(funsT) });
			result.add(modFunsT);
		}
		return result;
	}

	public static IErlPreprocessorDef findPreprocessorDef(final Backend b,
			final IProject project, final IErlModule module,
			final String definedName, final IErlElement.Kind kind,
			final String externalIncludes) {
		String unquoted = ErlideUtil.unquote(definedName);
		final Set<String> names = new HashSet<String>(3);
		if (kind == Kind.RECORD_DEF) {
			while (names.add(unquoted)) {
				unquoted = checkMacroValue(unquoted, module);
			}
		} else {
			names.add(unquoted);
		}
		names.add(definedName);
		for (final String name : names) {
			try {
				final IErlPreprocessorDef pd = findPreprocessorDef(b, project,
						module, name, kind, externalIncludes,
						new ArrayList<IErlModule>());
				if (pd != null) {
					return pd;
				}
			} catch (final CoreException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * @param project
	 * @param m
	 * @param definedName
	 * @param type
	 * @param externalIncludes
	 *            TODO
	 * @param modulesDone
	 * @return
	 * @throws CoreException
	 */
	private static IErlPreprocessorDef findPreprocessorDef(final Backend b,
			final IProject project, IErlModule module, final String name,
			final IErlElement.Kind kind, final String externalIncludes,
			final List<IErlModule> modulesDone) throws CoreException {
		if (module == null) {
			return null;
		}
		modulesDone.add(module);
		module.open(null);
		final IErlPreprocessorDef pd = module.findPreprocessorDef(name, kind);
		if (pd != null) {
			return pd;
		}
		final Collection<ErlangIncludeFile> includes = module
				.getIncludedFiles();
		for (final ErlangIncludeFile element : includes) {
			IResource re = ResourceUtil
					.recursiveFindNamedResourceWithReferences(project, element
							.getFilenameLastPart(), PluginUtils
							.getIncludePathFilter(project, module.getResource()
									.getParent()));
			if (re == null) {
				try {
					String s = element.getFilename();
					if (element.isSystemInclude()) {
						s = ErlideOpen.getIncludeLib(b, s);
					} else {
						s = ModelUtils.findIncludeFile(project, s,
								externalIncludes);
					}
					re = ResourceUtil.openExternal(s);
				} catch (final Exception e) {
					ErlLogger.warn(e);
				}
			}
			if (re != null && re instanceof IFile) {
				module = ModelUtils.getModule((IFile) re);
				if (module != null && !modulesDone.contains(module)) {
					final IErlPreprocessorDef pd2 = findPreprocessorDef(b,
							project, module, name, kind, externalIncludes,
							modulesDone);
					if (pd2 != null) {
						return pd2;
					}
				}
			}
		}
		return null;
	}

	/**
	 * @param b
	 * @param project
	 * @param m
	 * @param modulesFound
	 * @return
	 * @throws CoreException
	 */
	private static List<IErlModule> getModulesWithIncludes(final Backend b,
			final IProject project, final IErlModule m,
			final String externalIncludes, final List<IErlModule> modulesFound)
			throws CoreException {
		if (m == null) {
			return null;
		}
		modulesFound.add(m);
		m.open(null);
		final Collection<ErlangIncludeFile> includes = m.getIncludedFiles();
		for (final ErlangIncludeFile element : includes) {
			IResource re = ResourceUtil
					.recursiveFindNamedResourceWithReferences(project, element
							.getFilenameLastPart(), PluginUtils
							.getIncludePathFilter(project, m.getResource()
									.getParent()));
			if (re == null) {
				try {
					String s = element.getFilename();
					if (element.isSystemInclude()) {
						s = ErlideOpen.getIncludeLib(b, s);
					} else {
						s = ModelUtils.findIncludeFile(project, s,
								externalIncludes);
					}
					re = ResourceUtil.openExternal(s);
				} catch (final Exception e) {
					ErlLogger.warn(e);
				}
			}
			if (re != null && re instanceof IFile) {
				final IErlModule included = ModelUtils.getModule((IFile) re);
				if (included != null && !modulesFound.contains(included)) {
					getModulesWithIncludes(b, project, included,
							externalIncludes, modulesFound);
				}
			}
		}
		return modulesFound;
	}

	public static boolean openPreprocessorDef(final Backend b,
			final IProject project, final IWorkbenchPage page,
			final IErlModule module, final String name,
			final IErlElement.Kind kind, final String externalIncludes)
			throws PartInitException, ErlModelException, CoreException {
		return openPreprocessorDef(b, project, page, module, name, kind,
				externalIncludes, new ArrayList<IErlModule>());
	}

	/**
	 * @param b
	 * @param project
	 * @param page
	 * @param module
	 * @param definedName
	 * @param type
	 * @param findPreprocessorDef
	 *            TODO
	 * @throws CoreException
	 * @throws ErlModelException
	 * @throws PartInitException
	 */
	private static boolean openPreprocessorDef(final Backend b,
			final IProject project, final IWorkbenchPage page,
			final IErlModule module, final String definedName,
			final IErlElement.Kind type, final String externalIncludes,
			final List<IErlModule> modulesDone) throws CoreException,
			ErlModelException, PartInitException {
		if (module == null) {
			return false;
		}
		modulesDone.add(module);
		module.open(null);
		final IErlPreprocessorDef pd = findPreprocessorDef(b, project, module,
				definedName, type, externalIncludes);
		if (pd == null) {
			final Collection<ErlangIncludeFile> includes = module
					.getIncludedFiles();
			for (final ErlangIncludeFile element : includes) {
				final String filenameLastPart = element.getFilenameLastPart();
				final IResource resource = module.getResource();
				final IContainer parent = resource.getParent();
				final ContainerFilter includePathFilter = PluginUtils
						.getIncludePathFilter(project, parent);
				IResource re = ResourceUtil
						.recursiveFindNamedResourceWithReferences(project,
								filenameLastPart, includePathFilter);
				if (re == null) {
					try {
						String s = element.getFilename();
						if (element.isSystemInclude()) {
							s = ErlideOpen.getIncludeLib(b, s);
						} else {
							s = ModelUtils.findIncludeFile(project, s,
									externalIncludes);
						}
						re = ResourceUtil.openExternal(s);
					} catch (final Exception e) {
						ErlLogger.warn(e);
					}
				}
				if (re != null && re instanceof IFile) {
					final IErlModule m2 = ModelUtils.getModule((IFile) re);
					if (m2 != null && !modulesDone.contains(m2)) {
						if (openPreprocessorDef(b, project, page, m2,
								definedName, type, externalIncludes,
								modulesDone)) {
							return true;
						}
					}
				}
			}
		}
		if (pd != null) {
			final IEditorPart editor = EditorUtility.openInEditor(pd
					.getModule());
			EditorUtility.revealInEditor(editor, pd);
			return true;
		}
		return false;
	}

	public static String checkMacroValue(final String definedName,
			final IErlModule m) {
		if ("?MODULE".equals(definedName)) {
			return m.getModuleName();
		}
		final IErlPreprocessorDef def = m.findPreprocessorDef(
				withoutInterrogationMark(definedName), Kind.MACRO_DEF);
		if (def != null) {
			final String extra = def.getExtra();
			final int p = extra.indexOf(',');
			if (p != -1) {
				final String s = extra.substring(p + 1).trim();
				if (s.length() > 0) {
					return s;
				}
			}
		}
		return definedName;
	}

	private static String withoutInterrogationMark(final String definedName) {
		if (definedName.startsWith("?")) {
			return definedName.substring(1);
		}
		return definedName;
	}

	/**
	 * Open an editor on the given module and select the given erlang function
	 * 
	 * @param mod
	 *            module name (without .erl)
	 * @param fun
	 *            function name
	 * @param arity
	 *            function arity
	 * @param path
	 *            path to module (including .erl)
	 * @param checkAllProjects
	 *            if true, check all projects in workspace, otherwise only
	 *            consider projects referred from project
	 * @throws CoreException
	 */
	public static boolean openExternalFunction(final String mod,
			final ErlangFunction function, final String path,
			final IErlModule module, final IProject project,
			final boolean checkAllProjects) throws CoreException {
		final IResource r = findExternalModule(mod, path, module, project,
				checkAllProjects);
		if (r != null && r instanceof IFile) {
			final IFile f = (IFile) r;
			try {
				final IEditorPart editor = EditorUtility.openInEditor(f);
				return openFunctionInEditor(function, editor);
			} catch (final PartInitException e) {
				ErlLogger.warn(e);
			} catch (final ErlModelException e) {
				ErlLogger.warn(e);
			}
		}
		return false;
	}

	public static boolean openElement(final IErlElement element)
			throws CoreException {
		final IEditorPart editor = EditorUtility.openInEditor(element);
		return EditorUtility.revealInEditor(editor, element);
	}

	public static boolean openExternalType(final String mod, final String type,
			final String path, final IErlModule module, final IProject project,
			final boolean checkAllProjects) throws CoreException {
		final IResource r = findExternalModule(mod, path, module, project,
				checkAllProjects);
		if (r != null && r instanceof IFile) {
			final IFile f = (IFile) r;
			try {
				final IEditorPart editor = EditorUtility.openInEditor(f);
				return openTypeInEditor(type, editor);
			} catch (final PartInitException e) {
				ErlLogger.warn(e);
			} catch (final ErlModelException e) {
				ErlLogger.warn(e);
			}
		}
		return false;
	}

	public static IResource findExternalModule(String mod, final String path,
			final IErlModule module, final IProject project,
			final boolean checkAllProjects) throws CoreException {
		if (mod.startsWith("?")) {
			mod = checkMacroValue(mod, module);
		}
		final String modFileName = mod + ".erl";
		IResource r = null;
		if (project != null) {
			r = ResourceUtil.recursiveFindNamedResourceWithReferences(project,
					modFileName, PluginUtils.getSourcePathFilter(project));

			if (r == null) {
				try {
					r = ResourceUtil.openExternal(path);
				} catch (final Exception e) {
					ErlLogger.warn(e);
				}
				if (r != null && !PluginUtils.isOnSourcePath(r.getParent())) {
					r = null;
				}
			}
		}
		if (r == null) {
			ErlLogger.debug(
					"findExternalModule not found yet, checkAllProjects %b",
					checkAllProjects);
		}
		if (r == null && checkAllProjects) {
			final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace()
					.getRoot();
			final IProject[] projects = workspaceRoot.getProjects();
			for (final IProject p : projects) {
				if (ErlideUtil.hasErlangNature(p)) {
					ErlLogger.debug("searching project %s", p.getName());
					r = ResourceUtil.recursiveFindNamedResource(p, modFileName,
							PluginUtils.getSourcePathFilter(p));
					if (r != null) {
						ErlLogger.debug("found %s", r);
						break;
					}
				}
			}
		}
		if (r == null) {
			try {
				r = ResourceUtil.openExternal(path);
			} catch (final Exception e) {
				ErlLogger.warn(e);
			}
		}
		return r;
	}

	public static IErlModule getExternalModule(final String mod,
			final String externalModules) throws CoreException {
		final String path = ErlideOpen.getExternalModule(ErlangCore
				.getBackendManager().getIdeBackend(), mod, externalModules,
				ErlangCore.getModel().getPathVars());
		if (path != null) {
			final IFile f = ResourceUtil.openExternal(path);
			return ModelUtils.getModule(f);
		}
		return null;
	}

	/**
	 * Activate editor and select erlang function
	 * 
	 * @param fun
	 * @param arity
	 * @param editor
	 * @throws ErlModelException
	 */
	public static boolean openFunctionInEditor(
			final ErlangFunction erlangFunction, final IEditorPart editor)
			throws ErlModelException {
		final IErlModule module = getModule(editor);
		if (module == null) {
			return false;
		}
		module.open(null);
		final IErlFunction function = ModelUtils.findFunction(module,
				erlangFunction);
		if (function == null) {
			return false;
		}
		EditorUtility.revealInEditor(editor, function);
		return true;
	}

	public static boolean openTypeInEditor(final String typeName,
			final IEditorPart editor) throws ErlModelException {
		final IErlModule module = getModule(editor);
		if (module == null) {
			return false;
		}
		module.open(null);
		final IErlTypespec typespec = ModelUtils.findTypespec(module, typeName);
		if (typespec == null) {
			return false;
		}
		EditorUtility.revealInEditor(editor, typespec);
		return true;
	}

	public static List<IErlModule> getModulesWithReferencedProjects(
			final IErlProject project) {
		final IErlModel model = ErlangCore.getModel();
		final List<IErlModule> result = new ArrayList<IErlModule>();
		if (project == null) {
			return result;
		}
		try {
			project.open(null);
			result.addAll(project.getModules());
			for (final IProject p : project.getProject()
					.getReferencedProjects()) {
				final IErlProject ep = model.findProject(p);
				if (ep != null) {
					ep.open(null);
					result.addAll(ep.getModules());
				}
			}
		} catch (final ErlModelException e) {
			ErlLogger.error(e);
		} catch (final CoreException e) {
			ErlLogger.error(e);
		}
		return result;
	}

	public static IErlModule getModule(final IEditorInput editorInput,
			final IDocumentProvider documentProvider) {
		if (editorInput == null) {
			return null;
		}
		if (editorInput instanceof IFileEditorInput) {
			final IFileEditorInput input = (IFileEditorInput) editorInput;
			return ModelUtils.getModule(input.getFile());
		}
		if (editorInput instanceof IStorageEditorInput) {
			final IStorageEditorInput sei = (IStorageEditorInput) editorInput;
			final IDocument doc = documentProvider.getDocument(editorInput);
			try {
				final IPath p = sei.getStorage().getFullPath();
				final String path = (p == null) ? "" : p.toString();
				return ErlangCore.getModelManager().getModuleFromFile(
						editorInput.getName(), doc.get(), path, editorInput);
			} catch (final CoreException e) {
				ErlLogger.warn(e);
			}
		}
		if (editorInput instanceof IURIEditorInput) {
			final IURIEditorInput ue = (IURIEditorInput) editorInput;
			final String path = ue.getURI().getPath();
			final IDocument doc = documentProvider.getDocument(editorInput);
			return ErlangCore.getModelManager().getModuleFromFile(
					editorInput.getName(), doc.get(), path, editorInput);
		}
		return null;
	}

	public static String[] getPredefinedMacroNames() {
		return new String[] { "MODULE", "LINE", "FILE" };
	}

	public static void openMFA(String module, String function, int arity)
			throws CoreException {
		ErlModelUtils.openExternalFunction(module, new ErlangFunction(function,
				arity), null, ErlangCore.getModel().findModule(module), null,
				true);
	}

	public static void openMFA(String module, String function)
			throws CoreException {
		openMFA(module, function, ErlangFunction.ANY_ARITY);
	}

}
