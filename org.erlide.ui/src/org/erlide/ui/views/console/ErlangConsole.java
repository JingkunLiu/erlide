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
package org.erlide.ui.views.console;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.console.ConsoleColorProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.part.IPageBookViewPage;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.jinterface.backend.BackendException;
import org.erlide.jinterface.backend.BackendShell;
import org.erlide.jinterface.backend.BackendShellListener;
import org.erlide.jinterface.backend.ErlBackend;
import org.erlide.jinterface.backend.IDisposable;
import org.erlide.jinterface.backend.console.IoRequest;
import org.erlide.jinterface.util.ErlLogger;
import org.erlide.runtime.backend.ErlideBackend;
import org.erlide.ui.console.ErlangConsolePage;

import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;

public class ErlangConsole extends IOConsole implements BackendShellListener,
		IDisposable {

	private IOConsoleOutputStream stdout;
	private IOConsoleOutputStream stderr;
	private IOConsoleOutputStream output;
	BackendShell shell;

	public ErlangConsole(ErlideBackend backend, String name,
			ImageDescriptor descriptor) {
		super(name, descriptor);

		shell = backend.getShell("main");
		shell.addListener(this);

		// TODO use own color provider
		ConsoleColorProvider ccp = new ConsoleColorProvider();

		stdout = newOutputStream();
		// TODO use preferences for console colors
		stdout.setColor(ccp
				.getColor(IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM));
		stderr = newOutputStream();
		stderr.setColor(ccp
				.getColor(IDebugUIConstants.ID_STANDARD_ERROR_STREAM));
		output = newOutputStream();
		output.setColor(ccp
				.getColor(IDebugUIConstants.ID_STANDARD_OUTPUT_STREAM));

		IOConsoleInputStream inputStream = getInputStream();
		inputStream.setColor(ccp
				.getColor(IDebugUIConstants.ID_STANDARD_INPUT_STREAM));

		InputReadJob readJob = new InputReadJob(inputStream);
		readJob.setSystem(true);
		readJob.schedule();
	}

	// @Override
	// public IPageBookViewPage createPage(IConsoleView view) {
	// return new ErlangConsolePage(this, view);
	// }

	public void show() {
		IWorkbenchPage page = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		String id = IConsoleConstants.ID_CONSOLE_VIEW;
		IConsoleView view;
		try {
			view = (IConsoleView) page.showView(id);
			view.display(this);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}

	public void shellEvent(BackendShell aShell, IoRequest req) {
		if (aShell != shell || req == null || output == null) {
			return;
		}
		try {
			String message = req.getMessage();
			switch (req.getKind()) {
			case HEADER:
			case PROMPT:
			case OUTPUT:
				output.write(message);
				break;
			case STDOUT:
				stdout.write(message);
				break;
			case STDERR:
				stderr.write(message);
				break;
			case INPUT:
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void dispose() {
		ErlLogger.debug("dispose console " + this);
		stdout = null;
		stderr = null;
		output = null;
		shell = null;
		super.dispose();
	}

	private class InputReadJob extends Job {

		private final IOConsoleInputStream stream;

		InputReadJob(IOConsoleInputStream inputStream) {
			super("Erlang Console Input Job"); //$NON-NLS-1$
			this.stream = inputStream;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				byte[] b = new byte[1024];
				int read = 0;
				while (stream != null && read >= 0) {
					read = stream.read(b);
					if (read > 0) {
						String s = new String(b, 0, read);
						shell.input(s);
						shell.send(s);
					}
				}
			} catch (IOException e) {
				ErlLogger.error(e);
			}
			return Status.OK_STATUS;
		}
	}

	public BackendShell getShell() {
		return shell;
	}

	public static boolean isInputComplete(String input) {
		try {
			final String str = input + " ";
			final OtpErlangObject o = ErlBackend.parseConsoleInput(ErlangCore
					.getBackendManager().getIdeBackend(), str);
			if (o instanceof OtpErlangList && ((OtpErlangList) o).arity() == 0) {
				return false;
			}
			if (!(o instanceof OtpErlangList)) {
				return false;
			}
		} catch (final BackendException e) {
			return false;
		}
		return true;
	}

}
