package org.erlide.cover.ui.views;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.statushandlers.StatusManager;
import org.erlide.cover.core.CoverBackend;
import org.erlide.cover.core.ICoverEvent;
import org.erlide.cover.core.ICoverObserver;
import org.erlide.cover.ui.Activator;
import org.erlide.cover.ui.actions.ClearCoverageAction;
import org.erlide.cover.ui.actions.HideCoverageAction;
import org.erlide.cover.ui.actions.HtmlReportAction;
import org.erlide.cover.ui.actions.OpenItemAction;
import org.erlide.cover.ui.actions.ShowCoverageAction;
import org.erlide.cover.ui.annotations.EditorTracker;
import org.erlide.cover.ui.views.helpers.StatsNameSorter;
import org.erlide.cover.ui.views.helpers.StatsViewContentProvider;
import org.erlide.cover.ui.views.helpers.StatsViewLabelProvider;
import org.erlide.cover.views.model.StatsTreeModel;

/**
 * View for coverage statistics
 * 
 * Aleksandra Lipiec <aleksandra.lipiec@erlang.solutions.com>
 */
public class CoverStatsView extends ViewPart implements ICoverObserver {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "org.erlide.eunit.core.views.TestResultView";

    private TreeViewer viewer;
    private DrillDownAdapter drillDownAdapter;
    
    private Action openItem;
    private Action showHtml;
    private Action save;
    private Action clear;
    private Action refresh;
    private Action restore;
    private Action doubleClickAction;
    private Action showCoverage;
    private Action hideCoverage;
    
    private final CoverBackend backend;
    private TreeColumn colName;
    private TreeColumn colLines;
    private TreeColumn colCovered;
    private TreeColumn colPercentage;

    private Logger log;         //logger
    
    /**
     * The constructor.
     */
    public CoverStatsView() {
        backend = CoverBackend.getInstance();
        backend.addListener(this);

        // TODO: find better place for this
        backend.addAnnotationMaker(EditorTracker.getInstance());
        log = Logger.getLogger(this.getClass());
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    @Override
    public void createPartControl(final Composite parent) {
        // layout
        final GridLayout containerLayout = new GridLayout(1, false);
        containerLayout.marginWidth = 0;
        containerLayout.marginHeight = 0;
        containerLayout.verticalSpacing = 3;
        parent.setLayout(containerLayout);

        viewer = new TreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL
                | SWT.V_SCROLL);
        drillDownAdapter = new DrillDownAdapter(viewer);
        viewer.setContentProvider(new StatsViewContentProvider(getViewSite()));
        viewer.setLabelProvider(new StatsViewLabelProvider());
        viewer.setSorter(new StatsNameSorter());
        viewer.setInput(getViewSite());
        viewer.getTree().setLayoutData(
                new GridData(SWT.FILL, SWT.FILL, true, true));

        createTableTree();
        viewer.setInput(StatsTreeModel.getInstance());

        viewer.refresh();

        // Create the help context id for the viewer's control
        PlatformUI.getWorkbench().getHelpSystem()
                .setHelp(viewer.getControl(), "org.erlide.eunit.core.viewer");

        makeActions();

        hookContextMenu();
        hookDoubleClickAction();
        contributeToActionBars();
    }

    private void createTableTree() {

        final Tree tree = viewer.getTree();

        tree.setLinesVisible(true);
        tree.setHeaderVisible(true);

        colName = new TreeColumn(tree, SWT.LEFT);
        colName.setText("Name");
        colName.setWidth(540);

        colLines = new TreeColumn(tree, SWT.RIGHT);
        colLines.setText("Total Lines");
        colLines.setWidth(150);

        colCovered = new TreeColumn(tree, SWT.RIGHT);
        colCovered.setText("Covered Lines");
        colCovered.setWidth(150);

        colPercentage = new TreeColumn(tree, SWT.RIGHT);
        colPercentage.setText("Coverage");
        colPercentage.setWidth(150);

    }

    private void hookContextMenu() {
        final MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(final IMenuManager manager) {
                CoverStatsView.this.fillContextMenu(manager);
            }
        });
        final Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    private void contributeToActionBars() {
        final IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(final IMenuManager manager) {
        manager.add(clear);
        manager.add(refresh);
        manager.add(new Separator());
        manager.add(restore);
        manager.add(save);
    }

    private void fillContextMenu(final IMenuManager manager) {
        manager.add(openItem);
        manager.add(showHtml);
        manager.add(showCoverage);
        manager.add(hideCoverage);
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void fillLocalToolBar(final IToolBarManager manager) {
        manager.add(clear);
        manager.add(refresh);
        manager.add(new Separator());
        manager.add(restore);
        manager.add(save);
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
    }

    private void makeActions() {
        makeClearAction();
        makeDoubleClickAction();
        makeOpenItemAction();
        makeRestoreAction();
        makeSaveAction();
        makeShowHtmlAction();
        makeRefreshAction();
        makeShowCoverageAction();
        makeHideCoverageAction();
    }

    private void makeShowCoverageAction() {
        showCoverage = new ShowCoverageAction(viewer);
        showCoverage.setText("Show coverage");
        showCoverage.setToolTipText("Shows item's coverage");
        showCoverage.setImageDescriptor(PlatformUI.getWorkbench()
                .getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
    }

    private void makeHideCoverageAction() {
        hideCoverage = new HideCoverageAction(viewer);
        hideCoverage.setText("Hide coverage");
        hideCoverage.setToolTipText("Hides item's coverage");
        hideCoverage.setImageDescriptor(PlatformUI.getWorkbench()
                .getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
    }

    private void makeOpenItemAction() {
        openItem = new OpenItemAction(viewer);
        openItem.setText("Open in editor");
        openItem.setToolTipText("Opens the including file in editor");
        openItem.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
    }

    private void makeShowHtmlAction() {
        log.debug(viewer.getSelection());
        showHtml = new HtmlReportAction(viewer);
        showHtml.setText("Show html report");
        showHtml.setToolTipText("Shows generated html report");
        showHtml.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
    }

    private void makeClearAction() {
        clear = new ClearCoverageAction();
        clear.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_ETOOL_CLEAR));
        clear.setToolTipText("Clear coverage marking from editor");
    }

    private void makeRestoreAction() {
        restore = new Action() {

            @Override
            public void run() {
                showMessage("Action save");
            }
        };
        restore.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));
        restore.setToolTipText("Restore previous results");
        restore.setEnabled(false);
    }

    private void makeSaveAction() {
        save = new Action() {

            @Override
            public void run() {
                showMessage("Action save");
            }
        };
        save.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT));
        save.setToolTipText("Save coverage results");
        save.setEnabled(false);
    }

    private void makeRefreshAction() {
        refresh = new Action() {

            @Override
            public void run() {
                showMessage("Action refresh");
            }
        };
        // TODO change image
        refresh.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_TOOL_REDO));
        refresh.setToolTipText("Refresh coverage statistics view");
        refresh.setEnabled(false);
    }

    private void makeDoubleClickAction() {
        // TODO: use it (open file in editor ?)
        doubleClickAction = new Action() {
            @Override
            public void run() {
                final ISelection selection = viewer.getSelection();
                final Object obj = ((IStructuredSelection) selection)
                        .getFirstElement();
                showMessage("Double-click detected on " + obj.toString());
            }
        };
        doubleClickAction.setEnabled(false);
    }

    private void hookDoubleClickAction() {
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(final DoubleClickEvent event) {
                doubleClickAction.run();
            }
        });
    }

    private void showMessage(final String message) {
        MessageDialog.openInformation(viewer.getControl().getShell(),
                "Coverage statistics", message);
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    public void eventOccured(final ICoverEvent e) {

        switch (e.getType()) {
        case UPDATE:

            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    viewer.refresh();
                }
            });
            break;
        case ERROR:
            final IStatus executionStatus = new Status(IStatus.ERROR,
                    Activator.PLUGIN_ID, e.getInfo(), null);
            StatusManager.getManager().handle(executionStatus,
                    StatusManager.SHOW);
            break;
        default:
            break;
        }
    }

}
