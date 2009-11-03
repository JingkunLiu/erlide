/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.erlide.ui.views.console;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.internal.console.IOConsoleViewer;

/**
 * Viewer used to display an Erlang console
 * 
 */
@SuppressWarnings("restriction")
public class ErlangConsoleViewer extends IOConsoleViewer {
	final class HistoryContentProvider implements IStructuredContentProvider {
		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getElements(Object inputElement) {
			return history.getAll().toArray();
		}
	}

	public static final String OUTPUT_PARTITION_TYPE = ConsolePlugin
			.getUniqueIdentifier()
			+ ".io_console_output_partition_type"; //$NON-NLS-1$
	public static final String INPUT_PARTITION_TYPE = ConsolePlugin
			.getUniqueIdentifier()
			+ ".io_console_input_partition_type"; //$NON-NLS-1$

	final ConsoleHistory history;
	final ErlangConsole console;

	public ErlangConsoleViewer(Composite parent, ErlangConsole erlangConsole) {
		super(parent, erlangConsole);
		console = erlangConsole;
		history = new ConsoleHistory();
	}

	@Override
	protected void createControl(Composite parent, int styles) {
		super.createControl(parent, styles);
		Control control = getControl();
		control.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				final IDocument doc = getDocument();
				final boolean isHistoryCommand = ((e.stateMask & SWT.CTRL) == SWT.CTRL)
						&& ((e.keyCode == SWT.ARROW_UP) || (e.keyCode == SWT.ARROW_DOWN));

				try {
					int length = doc.getLength();
					ITypedRegion partition = doc.getPartition(length - 1);
					String oldText = doc.get(partition.getOffset(), partition
							.getLength());
					final boolean isInputLast = partition.getType().equals(
							INPUT_PARTITION_TYPE);
					if (e.keyCode == 13) {
						history.add(oldText);
					} else if (e.keyCode == 27 && isInputLast) {
						doc.replace(partition.getOffset(), partition
								.getLength(), "");
					} else if (isHistoryCommand) {
						showHistoryDialog(doc);
						e.doit = false;
					}
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	@Override
	protected void handleVisibleDocumentChanged(DocumentEvent event) {
		// TODO Auto-generated method stub
		super.handleVisibleDocumentChanged(event);
		StyledText text = (StyledText) getControl();
		text.setCaretOffset(text.getCharCount());
	}

	protected void showHistoryDialog(final IDocument doc) {
		if (history.isEmpty()) {
			return;
		}

		final StyledText parent = getTextWidget();
		final Shell container = new Shell(parent.getShell(), SWT.MODELESS);
		container.setLayout(new FillLayout());

		Point point = parent.getLocationAtOffset(doc.getLength());
		final int b = point.x;
		final Point screenPos = parent.toDisplay(b, 0);
		container.setLocation(screenPos);
		final Rectangle rect = parent.getClientArea();
		container.setSize(rect.width - b, rect.height);

		ListViewer viewer = new ListViewer(container, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL);
		viewer.setContentProvider(new HistoryContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.setInput(history);

		final List list = viewer.getList();
		list.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(final FocusEvent e) {
				container.close();
			}
		});

		list.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == 13) {
					int i = 0;
					for (String s : list.getSelection()) {
						try {
							if (i == list.getSelectionCount() - 1) {
								s = s.trim();
							}
							doc.replace(doc.getLength(), 0, s);
						} catch (BadLocationException e1) {
							e1.printStackTrace();
						}
						i++;
					}
					container.close();
					parent.setCaretOffset(doc.getLength());
					e.doit = false;
				} else {
					super.keyPressed(e);
				}
			}
		});

		container.setVisible(true);
		list.setFocus();
		list.setSelection(list.getItemCount() - 1);
	}

}
