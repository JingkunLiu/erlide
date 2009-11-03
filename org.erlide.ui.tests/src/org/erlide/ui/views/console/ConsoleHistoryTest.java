/*******************************************************************************
 * Copyright (c) 2008 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution.
 * 
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.ui.views.console;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConsoleHistoryTest {

	private ConsoleHistory history;

	@Before
	public void setup() {
		history = new ConsoleHistory();
		history.reset();
	}

	@After
	public void finish() {
		history = null;
	}

	@Test
	public void empty() {
		Assert.assertTrue(history.isEmpty());
		Assert.assertEquals("", history.current());
	}

	@Test
	public void empty_next() {
		history.next();
		Assert.assertEquals("", history.current());
	}

	@Test
	public void empty_prev() {
		history.prev();
		Assert.assertEquals("", history.current());
	}

	@Test
	public void one() {
		history.add("111");
		Assert.assertEquals("111", history.current());
	}

	@Test
	public void one_prev() {
		history.add("111");
		history.prev();
		Assert.assertEquals("111", history.current());
	}

	@Test
	public void one_prev2() {
		history.add("111");
		history.prev();
		history.prev();
		Assert.assertEquals("111", history.current());
	}

	@Test
	public void one_prevnext() {
		history.add("111");
		history.prev();
		Assert.assertEquals("111", history.current());
	}

	@Test
	public void one_prevnext2() {
		history.add("111");
		history.prev();
		history.next();
		history.next();
		Assert.assertEquals("111", history.current());
	}

	@Test
	public void two() {
		history.add("111");
		history.add("222");
		Assert.assertEquals("222", history.current());
	}

	@Test
	public void two_prev() {
		history.add("111");
		history.add("222");
		history.prev();
		Assert.assertEquals("111", history.current());
	}

	@Test
	public void two_prev2() {
		history.add("111");
		history.add("222");
		history.prev();
		history.prev();
		Assert.assertEquals("111", history.current());
	}

	@Test
	public void two_prev3() {
		history.add("111");
		history.add("222");
		history.prev();
		history.prev();
		history.prev();
		Assert.assertEquals("111", history.current());
	}

	@Test
	public void two_prevnextprev() {
		history.add("111");
		history.add("222");
		history.prev();
		history.next();
		history.prev();
		Assert.assertEquals("111", history.current());
	}

	@Test
	public void many_1() {
		history.add("111");
		history.add("222");
		history.add("333");
		history.add("444");
		Assert.assertEquals("444", history.current());
		history.prev();
		Assert.assertEquals("333", history.current());
		history.prev();
		Assert.assertEquals("222", history.current());
		history.prev();
		Assert.assertEquals("111", history.current());
		history.prev();
		Assert.assertEquals("111", history.current());
		history.next();
		Assert.assertEquals("222", history.current());
	}

	@Test
	public void many_2() {
		history.add("111");
		history.add("222");
		history.add("333");
		history.add("444");
		Assert.assertEquals("444", history.current());
		history.prev();
		Assert.assertEquals("333", history.current());
		history.next();
		Assert.assertEquals("444", history.current());
		history.next();
		Assert.assertEquals("444", history.current());
		history.prev();
		Assert.assertEquals("333", history.current());
	}

}
