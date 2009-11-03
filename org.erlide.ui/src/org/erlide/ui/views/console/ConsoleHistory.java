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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ConsoleHistory {
	private final List<String> history;
	int crt = 0;

	public ConsoleHistory() {
		history = new ArrayList<String>();
	}

	public void reset() {
		crt = history.size() - 1;
	}

	public void add(final String in) {
		history.add(in);
		if (history.size() > 50) {
			history.remove(0);
		}
		reset();
	}

	public boolean isEmpty() {
		return history.size() == 0;
	}

	public String current() {
		if (crt < 0) {
			return "";
		}
		return history.get(crt);
	}

	public void next() {
		if (crt < history.size() - 1) {
			crt++;
		}
	}

	public void prev() {
		if (crt > 0) {
			crt--;
		}
	}

	public Collection<String> getAll() {
		return Collections.unmodifiableCollection(history);
	}

	@Override
	public String toString() {
		String res = "[";
		for (int i = 0; i < crt; i++) {
			res += history.get(i) + ", ";
		}
		res += "*";
		for (int i = crt; i < history.size(); i++) {
			res += history.get(i) + ", ";
		}
		res += "]";
		return res;
	}
}