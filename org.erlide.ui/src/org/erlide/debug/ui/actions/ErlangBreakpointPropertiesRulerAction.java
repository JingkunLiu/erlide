package org.erlide.debug.ui.actions;

import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.actions.RulerBreakpointAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.erlide.runtime.debug.IErlangBreakpoint;

/**
 * Presents the standard properties dialog to configure the attibutes of a Java
 * Breakpoint from the ruler popup menu of a text editor.
 */
public class ErlangBreakpointPropertiesRulerAction extends
        RulerBreakpointAction implements IUpdate {

    private IBreakpoint fBreakpoint;

    /**
     * Creates the action to enable/disable breakpoints
     */
    public ErlangBreakpointPropertiesRulerAction(final ITextEditor editor,
            final IVerticalRulerInfo info) {
        super(editor, info);
        setText(org.erlide.ui.actions.ActionMessages.ErlangBreakpointPropertiesRulerAction_Breakpoint__Properties_1);
    }

    /**
     * @see Action#run()
     */
    @Override
    public void run() {
        if (getBreakpoint() != null) {
            final PropertyDialogAction action = new PropertyDialogAction(
                    getEditor().getEditorSite(), new ISelectionProvider() {
                        public void addSelectionChangedListener(
                                final ISelectionChangedListener listener) {
                        }

                        @SuppressWarnings("synthetic-access")
                        public ISelection getSelection() {
                            return new StructuredSelection(getBreakpoint());
                        }

                        public void removeSelectionChangedListener(
                                final ISelectionChangedListener listener) {
                        }

                        public void setSelection(final ISelection selection) {
                        }
                    });
            action.run();
        }
    }

    /**
     * @see IUpdate#update()
     */
    public void update() {
        fBreakpoint = null;
        final IBreakpoint breakpoint = getBreakpoint();
        if (breakpoint != null && breakpoint instanceof IErlangBreakpoint) {
            fBreakpoint = breakpoint;
        }
        setEnabled(fBreakpoint != null);
    }
}
