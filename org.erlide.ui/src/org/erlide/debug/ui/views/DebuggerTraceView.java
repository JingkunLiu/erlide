package org.erlide.debug.ui.views;

import java.util.List;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlModel;
import org.erlide.core.erlang.IErlModule;
import org.erlide.runtime.debug.ErlangDebugTarget;
import org.erlide.runtime.debug.ErlangDebugTarget.TraceChangedEventData;
import org.erlide.ui.ErlideUIPlugin;
import org.erlide.ui.editors.erl.ErlangEditor;
import org.erlide.ui.editors.util.EditorUtility;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangRangeException;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class DebuggerTraceView extends ViewPart implements
		IDebugEventSetListener {

	// -record(ieval,
	// {level = 1, % Current call level
	// line = -1, % Current source code line (of module)
	// module, % MFA which called the currently
	// function, % interpreted function
	// arguments, %
	// last_call = false % True if current expression is
	// }). % the VERY last to be evaluated
	// % (ie at all, not only in a clause)

	public class ColumnLabelProvider extends CellLabelProvider {

		@Override
		public void update(final ViewerCell cell) {
			final Object element = cell.getElement();
			if (element instanceof OtpErlangTuple) {
				final OtpErlangTuple t = (OtpErlangTuple) element;
				final OtpErlangTuple t2 = (OtpErlangTuple) t.elementAt(1);
				final int columnIndex = cell.getColumnIndex();
				String s;
				switch (columnIndex) {
				case 0:
					final OtpErlangAtom w = (OtpErlangAtom) t.elementAt(0);
					final String what = w.atomValue();
					s = what;
					break;
				case 1:
					final OtpErlangTuple ieval = (OtpErlangTuple) t2
							.elementAt(0);
					final OtpErlangAtom mod = (OtpErlangAtom) ieval
							.elementAt(3);
					final String module = mod.atomValue();
					final OtpErlangLong lin = (OtpErlangLong) ieval
							.elementAt(2);
					s = module;
					try {
						final int line = lin.intValue();
						s = s += ":" + line;
					} catch (final OtpErlangRangeException e) {
					}
					break;
				case 2:
				default:
					final OtpErlangObject o = t2.elementAt(1);
					s = o.toString();
					break;
				}
				cell.setText(s);
			}
		}

	}

	protected static final Object[] NO_CHILDREN = new Object[0];
	TreeViewer viewer;
	ErlangDebugTarget debugTarget;

	public DebuggerTraceView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(final Composite parent) {
		parent.setLayout(new FillLayout());

		viewer = new TreeViewer(new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.MULTI | SWT.FULL_SELECTION));
		viewer.getTree().setLinesVisible(true);
		viewer.setUseHashlookup(true);

		createColumns();

		viewer.setContentProvider(getContentProvider());
		viewer.setLabelProvider(new ColumnLabelProvider());
		getSite().setSelectionProvider(viewer);

		viewer.setInput(debugTarget);
		DebugPlugin.getDefault().addDebugEventListener(this);

		// viewer.getTree().addTreeListener(new TreeAdapter() {
		// /*
		// * (non-Javadoc)
		// *
		// * @see
		// * org.eclipse.swt.events.TreeAdapter#treeCollapsed(org.eclipse.
		// * swt.events.TreeEvent)
		// */
		// @Override
		// public void treeCollapsed(final TreeEvent e) {
		// removeExpandedCategory((MarkerCategory) e.item.getData());
		// }
		//
		// /*
		// * (non-Javadoc)
		// *
		// * @see
		// * org.eclipse.swt.events.TreeAdapter#treeExpanded(org.eclipse.swt
		// * .events.TreeEvent)
		// */
		// @Override
		// public void treeExpanded(final TreeEvent e) {
		// addExpandedCategory((MarkerCategory) e.item.getData());
		// }
		// });

		// // Set help on the view itself
		// viewer.getControl().addHelpListener(new HelpListener() {
		// /*
		// * (non-Javadoc)
		// *
		// * @see
		// * org.eclipse.swt.events.HelpListener#helpRequested(org.eclipse
		// * .swt.events.HelpEvent)
		// */
		// public void helpRequested(HelpEvent e) {
		// Object provider = getAdapter(IContextProvider.class);
		// if (provider == null) {
		// return;
		// }
		//
		// IContext context = ((IContextProvider) provider)
		// .getContext(viewer.getControl());
		// PlatformUI.getWorkbench().getHelpSystem().displayHelp(context);
		// }
		//
		// });

		viewer.getTree().addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
			 * .swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final Object o = getSelectedInTree();
				final String msg = o == null ? "" : o.toString();
				getViewSite().getActionBars().getStatusLineManager()
						.setMessage(msg);

			}
		});

		viewer.getTree().addMouseListener(new MouseListener() {

			public void mouseDoubleClick(final MouseEvent e) {
				final Object o = getSelectedInTree();
				if (o instanceof OtpErlangTuple) {
					final OtpErlangTuple t = (OtpErlangTuple) o;
					final OtpErlangTuple t2 = (OtpErlangTuple) t.elementAt(1);
					final OtpErlangTuple ieval = (OtpErlangTuple) t2
							.elementAt(0);
					final OtpErlangAtom mod = (OtpErlangAtom) ieval
							.elementAt(3);
					final String module = mod.atomValue();
					final OtpErlangLong lin = (OtpErlangLong) ieval
							.elementAt(2);
					try {
						final int line = lin.intValue();
						gotoModuleLine(module, line);
					} catch (final OtpErlangRangeException e1) {
					}

				}
			}

			public void mouseDown(final MouseEvent e) {
				// TODO Auto-generated method stub

			}

			public void mouseUp(final MouseEvent e) {
				// TODO Auto-generated method stub

			}

		});
		// PlatformUI.getWorkbench().getWorkingSetManager()
		// .addPropertyChangeListener(getWorkingSetListener());

		// registerContextMenu();
		// initDragAndDrop();

	}

	protected void gotoModuleLine(final String module, final int line) {
		final IWorkbenchWindow dwindow = ErlideUIPlugin
				.getActiveWorkbenchWindow();
		if (dwindow == null) {
			return;
		}
		final IWorkbenchPage page = dwindow.getActivePage();
		if (page == null) {
			return;
		}

		IEditorPart part = null;
		final IErlModel model = ErlangCore.getModel();
		final IErlModule m = model.getModule(module);
		IEditorInput input = null;
		try {
			input = EditorUtility.getEditorInput(m);
		} catch (final ErlModelException e1) {
		}
		if (input != null) {
			final String editorId = EditorUtility.getEditorID(input, m);
			if (editorId != null) {
				try {
					part = page.openEditor(input, editorId);
				} catch (final PartInitException e) {
					DebugUIPlugin
							.errorDialog(
									dwindow.getShell(),
									ActionMessages.OpenBreakpointMarkerAction_Go_to_Breakpoint_1,
									ActionMessages.OpenBreakpointMarkerAction_Exceptions_occurred_attempting_to_open_the_editor_for_the_breakpoint_resource_2,
									e); // 
				}
			}
		}
		if (part instanceof ErlangEditor) {
			part.setFocus();
			final ErlangEditor ee = (ErlangEditor) part;
			final IDocument d = ee.getDocument();
			int lineStart, lineLength;
			try {
				lineStart = d.getLineOffset(line - 1);
				lineLength = d.getLineLength(line - 1);
				EditorUtility.revealInEditor(ee, lineStart, lineLength - 1);
			} catch (final BadLocationException e) {
				e.printStackTrace();
			}
		}
	}

	private IContentProvider getContentProvider() {
		return new ITreeContentProvider() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
			 */
			public void dispose() {

			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.jface.viewers.ILazyTreeContentProvider#updateChildCount
			 * (java.lang.Object, int)
			 */
			// public void updateChildCount(Object element, int
			// currentChildCount) {
			//
			// int length;
			// if (element instanceof MarkerItem)
			// length = ((MarkerItem) element).getChildren().length;
			// else
			// // If it is not a MarkerItem it is the root
			// length = ((CachedMarkerBuilder) element).getElements().length;
			//
			// int markerLimit = MarkerSupportInternalUtilities
			// .getMarkerLimit();
			// length = markerLimit > 0 ? Math.min(length, markerLimit)
			// : length;
			// if (currentChildCount == length)
			// return;
			// viewer.setChildCount(element, length);
			//
			// }
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.jface.viewers.ILazyTreeContentProvider#updateElement
			 * (java.lang.Object, int)
			 */
			// public void updateElement(Object parent, int index) {
			// MarkerItem newItem;
			//
			// if (parent instanceof MarkerItem)
			// newItem = ((MarkerItem) parent).getChildren()[index];
			// else
			// newItem = ((CachedMarkerBuilder) parent).getElements()[index];
			//
			// viewer.replace(parent, index, newItem);
			// updateChildCount(newItem, -1);
			//
			// if (!newItem.isConcrete()
			// && categoriesToExpand
			// .contains(((MarkerCategory) newItem).getName())) {
			// viewer.expandToLevel(newItem, 1);
			// categoriesToExpand.remove(newItem);
			// }
			//
			// }
			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java
			 * .lang.Object)
			 */
			public Object[] getChildren(final Object parentElement) {
				return NO_CHILDREN;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.jface.viewers.IStructuredContentProvider#getElements
			 * (java.lang.Object)
			 */
			public Object[] getElements(final Object inputElement) {
				if (debugTarget == null) {
					return NO_CHILDREN;
				}
				final List<OtpErlangTuple> traceList = debugTarget
						.getTraceList();
				if (traceList == null) {
					return NO_CHILDREN;
				}
				return traceList.toArray(new Object[traceList.size()]);
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.jface.viewers.ILazyTreeContentProvider#getParent(
			 * java.lang.Object)
			 */
			public Object getParent(final Object element) {
				return null;
				// final Object parent = ((MarkerSupportItem)
				// element).getParent();
				// if (parent == null) {
				// return builder;
				// }
				// return parent;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java
			 * .lang.Object)
			 */
			public boolean hasChildren(final Object element) {
				return false; // ((MarkerSupportItem)
				// element).getChildren().length > 0;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse
			 * .jface.viewers.Viewer, java.lang.Object, java.lang.Object)
			 */
			public void inputChanged(final Viewer viewer,
					final Object oldInput, final Object newInput) {

			}
		};
	}

	private void createColumns() {

		final Tree tree = viewer.getTree();
		final TableLayout layout = new TableLayout();
		TreeViewerColumn column;
		final String[] names = { "Kind", "Function", "Args" };
		for (final String name : names) {
			column = new TreeViewerColumn(viewer, SWT.NONE);
			final TreeColumn treeColumn = column.getColumn();
			treeColumn.setResizable(true);
			treeColumn.setMoveable(true);
			treeColumn.addSelectionListener(new SelectionListener() {

				public void widgetDefaultSelected(final SelectionEvent e) {
					// TODO Auto-generated method stub

				}

				public void widgetSelected(final SelectionEvent e) {
					// TODO Auto-generated method stub

				}
			});

			// column.getColumn().setData(MARKER_FIELD, markerField);
			// Show the help in the first column
			column.setLabelProvider(new ColumnLabelProvider());
			treeColumn.setText(name);
			treeColumn.setToolTipText(name);
		}
		// column = new TreeViewerColumn(viewer, SWT.NONE);
		// treeColumn = column.getColumn();
		// treeColumn.setResizable(true);
		// treeColumn.setMoveable(true);
		// column.setLabelProvider(new ColumnLabelProvider());
		// column.getColumn().setImage(markerField.getColumnHeaderImage());

		// final EditingSupport support = markerField
		// .getEditingSupport(viewer);
		// if (support != null) {
		// column.setEditingSupport(support);
		// }

		// if (builder.getPrimarySortField().equals(markerField)) {
		// updateDirectionIndicator(column.getColumn(), markerField);
		// }

		int columnWidth = -1;

		for (int i = 0; i < names.length; ++i) {
			if (i == 0) {
				// Compute and store a font metric
				final GC gc = new GC(tree);
				gc.setFont(tree.getFont());
				final FontMetrics fontMetrics = gc.getFontMetrics();
				gc.dispose();
				columnWidth = Math.max(100,
						fontMetrics.getAverageCharWidth() * 20);
			}

			// if (columnWidths != null) {
			// final Integer value = columnWidths.getInteger(getFieldId(column
			// .getColumn()));
			//
			// // Make sure we get a useful value
			// if (value != null && value.intValue() > 0) {
			// columnWidth = value.intValue();
			// }
			// }

			// // Take trim into account if we are using the default value, but
			// not
			// // if it is restored.
			// if (columnWidth < 0) {
			// layout.addColumnData(new ColumnPixelData(markerField
			// .getDefaultColumnWidth(tree), true, true));
			// } else {
			layout.addColumnData(new ColumnPixelData(columnWidth, true));
			// }
		}
		// }

		// Remove extra columns
		// if (currentColumns.length > fields.length) {
		// for (int i = fields.length; i < currentColumns.length; i++) {
		// currentColumns[i].dispose();
		//
		// }
		// }

		viewer.getTree().setLayout(layout);
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		tree.layout(true);

	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	public void handleDebugEvents(final DebugEvent[] events) {
		for (final DebugEvent event : events) {
			if (event.getKind() == DebugEvent.MODEL_SPECIFIC
					&& event.getDetail() == ErlangDebugTarget.TRACE_CHANGED) {
				final Object source = event.getSource();
				if (source instanceof ErlangDebugTarget) {
					final TraceChangedEventData data = (TraceChangedEventData) event
							.getData();
					traceChanged(data, source);
				}
			}
		}
	}

	private void traceChanged(final TraceChangedEventData data,
			final Object source) {
		if (viewer == null || viewer.getControl().isDisposed()) {
			return;
		}
		final Display display = viewer.getControl().getDisplay();
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				public void run() {
					if (viewer == null || viewer.getControl().isDisposed()) {
						return;
					}
					if (viewer.getInput() != source) {
						viewer.setInput(source);
						viewer.refresh();
					} else {
						if (data.getWhat() == TraceChangedEventData.ADDED) {
							viewer.add(source, data.getObjects());
						}
					}
				}
			});
		}
	}

	Object getSelectedInTree() {
		final ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection ss = (IStructuredSelection) selection;
			if (ss.size() == 1) {
				return ss.getFirstElement();
			}
		}
		return null;
	}
}