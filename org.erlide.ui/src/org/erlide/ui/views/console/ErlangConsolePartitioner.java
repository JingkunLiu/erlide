package org.erlide.ui.views.console;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitionerExtension;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.ui.console.IConsoleDocumentPartitioner;
import org.erlide.jinterface.backend.BackendShell;
import org.erlide.jinterface.backend.console.IoRequest;
import org.erlide.jinterface.backend.console.IoRequest.IoRequestKind;
import org.erlide.ui.console.IoRequestScanner;

public class ErlangConsolePartitioner implements IConsoleDocumentPartitioner,
		IDocumentPartitionerExtension {
	private static String[] LEGAL_CONTENT_TYPES = null;
	private BackendShell shell;

	private class IoRequestRegion implements ITypedRegion {
		private final IoRequest req;

		public IoRequestRegion(IoRequest req) {
			this.req = req;
		}

		public String getType() {
			return req.getKind().name();
		}

		public int getLength() {
			return req.getLength();
		}

		public int getOffset() {
			return req.getStart();
		}

	}

	public ErlangConsolePartitioner() {
		if (LEGAL_CONTENT_TYPES == null) {
			IoRequestKind[] values = IoRequestKind.values();
			LEGAL_CONTENT_TYPES = new String[values.length];
			for (int i = 0; i < LEGAL_CONTENT_TYPES.length; i++) {
				LEGAL_CONTENT_TYPES[i] = values[i].name();
			}
		}
	}

	public StyleRange[] getStyleRanges(int offset, int length) {
		// TODO Auto-generated method stub
		return new StyleRange[0];
	}

	public boolean isReadOnly(int offset) {
		IoRequest req = shell.findAtPos(offset);
		return false;
	}

	public ITypedRegion[] computePartitioning(int offset, int length) {
		return new ITypedRegion[] {};
	}

	public void connect(IDocument document) {
		document.setDocumentPartitioner(this);
	}

	public void disconnect() {
		shell = null;
	}

	public void documentAboutToBeChanged(DocumentEvent event) {
	}

	public boolean documentChanged(DocumentEvent event) {
		return documentChanged2(event) != null;
	}

	public String getContentType(int offset) {
		return getPartition(offset).getType();
	}

	public String[] getLegalContentTypes() {
		return LEGAL_CONTENT_TYPES;
	}

	public ITypedRegion getPartition(int offset) {
		return new IoRequestRegion(shell.findAtPos(offset));
	}

	private IPartitionTokenScanner createScanner() {
		return new IoRequestScanner(shell);
	}

	public IRegion documentChanged2(DocumentEvent event) {
		System.out.println("DOC changed 2" + event);
		// TODO Auto-generated method stub
		return null;
	}

}
