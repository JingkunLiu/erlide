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
package org.erlide.ui.console;

import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.internal.console.IOConsolePage;
import org.erlide.jinterface.backend.BackendShell;

@SuppressWarnings("restriction")
public class ErlangConsolePage extends IOConsolePage {
@SuppressWarnings("restriction")
	public static final String ID = "org.erlide.ui.views.console";

	private BackendShell shell;

	public ErlangConsolePage(ErlangConsole erlangConsole, IConsoleView view) {
		super(erlangConsole, view);
		shell = erlangConsole.getShell();
	}

	@Override
	public void dispose() {
		super.dispose();

		shell.dispose();
		shell = null;
		super.dispose();
	}

//	@Override
//	protected TextConsoleViewer createViewer(Composite parent) {
//		return new ErlangConsoleViewer(parent, (ErlangConsole) getConsole());
//	}

}
