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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.swt.widgets.Display;
import org.erlide.jinterface.backend.BackendShell;
import org.erlide.jinterface.backend.BackendShellListener;
import org.erlide.jinterface.backend.console.IoRequest;

public final class ErlConsoleDocument extends Document implements
		BackendShellListener {

	private final BackendShell shell;

	public ErlConsoleDocument(final BackendShell shell) {
		super();

		Assert.isNotNull(shell);
		this.shell = shell;
		shell.addListener(this);
		shellEvent(shell, null);
	}

	public void shellEvent(BackendShell aShell, IoRequest req) {
		if (aShell != shell) {
			return;
		}
		final String text = shell.getText();
		Display.getDefault().asyncExec(new Runnable() {

			public void run() {
				try {
					System.out.println("--+++---" + text);
					replace(0, getLength(), text);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public BackendShell getShell() {
		return shell;
	}

}
