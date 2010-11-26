/*******************************************************************************
 * Copyright (c) 2010 György Orosz.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     György Orosz - initial API and implementation
 ******************************************************************************/
package org.erlide.wrangler.refactoring.selection.internal;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlModel;
import org.erlide.core.erlang.IErlProject;
import org.erlide.core.preferences.OldErlangProjectProperties;
import org.erlide.wrangler.refactoring.selection.IErlSelection;

import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangString;

/**
 * Abstract class which represents an Erlang selection.
 * 
 * @author Gyorgy Orosz
 * @version %I%, %G%
 */
public abstract class AbstractErlSelection implements IErlSelection {

	protected IFile file;

	public boolean isEmpty() {
		return false;
	}

	public OtpErlangList getSearchPath() {
		IProject project = file.getProject();
		IErlModel model = ErlangCore.getModel();
		IErlProject actualProject = model.getErlangProject(project.getName());
		OldErlangProjectProperties prop = actualProject.getOldProperties();
		IPath projectLocation = actualProject.getProject().getLocation();

		Collection<IPath> sourcDirs = prop.getSourceDirs();
		OtpErlangString[] searchPath = new OtpErlangString[sourcDirs.size()];
		int i = 0;
		for (IPath src : sourcDirs) {
			searchPath[i++] = new OtpErlangString(projectLocation.append(src)
					.toOSString());
		}
		return new OtpErlangList(searchPath);
	}

	public String getFilePath() {
		return file.getLocation().toOSString();
	}

	public IFile getFile() {
		return file;
	}

}
