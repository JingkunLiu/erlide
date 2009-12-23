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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;

public abstract class DependencyLocation extends CodePathEntry {
	public static enum DependencyKind {
		COMPILE, RUN, COMPILE_RUN
	}

	private final DependencyKind kind;
	public abstract Collection<SourceLocation> getSources();
	public abstract Collection<IPath> getIncludes();
	/**
	 * May be null, meaning that result is included by other means (OTP
	 * libraries, for example).
	 * 
	 * @return
	 */
	public abstract IPath getOutput();
	public abstract Collection<DependencyLocation> getDependencies();

	public DependencyLocation() {
		this(DependencyKind.RUN);
	}

	public DependencyLocation(DependencyKind kind) {
		Assert.isLegal(kind != null);
		this.kind = kind;
	}

	public boolean isRunTime() {
		return kind == DependencyKind.RUN || kind == DependencyKind.COMPILE_RUN;
	}

	public boolean isCompileTime() {
		return kind == DependencyKind.COMPILE
				|| kind == DependencyKind.COMPILE_RUN;
	}

}
