/*******************************************************************************
 * Copyright (c) 2008 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.jinterface.backend;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class RuntimeInfo {
    private static final String[] NOTHING = new String[0];
    static final String DEFAULT_MARKER = "*DEFAULT*";

    private String homeDir = "";
    private String args = "";
    private String name;
    private List<String> codePath;

    private String cookie = "";
    private String nodeName = "";
    private String workingDir = "";
    private boolean managed; // will it be started/stopped by us?
    private RuntimeVersion version;
    private String suffix = "";
    private boolean useLongName = true;
    private boolean startShell = false;
    private boolean console = true;
    private boolean loadAllNodes = false;

    private String[] wrapperScript = NOTHING;

    public RuntimeInfo() {
        super();
        codePath = Lists.newArrayList();
        codePath.add(DEFAULT_MARKER);
    }

    public static RuntimeInfo copy(final RuntimeInfo o, final boolean mkCopy) {
        if (o == null) {
            return null;
        }
        final RuntimeInfo rt = new RuntimeInfo();
        rt.name = o.name;
        if (mkCopy) {
            rt.name += "_copy";
        }
        rt.args = o.args;
        rt.codePath = new ArrayList<String>(o.codePath);
        rt.managed = o.managed;
        rt.homeDir = o.homeDir;
        rt.workingDir = o.workingDir;
        rt.nodeName = o.nodeName;
        rt.version = o.version;
        rt.useLongName = o.useLongName;
        rt.startShell = o.startShell;
        rt.loadAllNodes = o.loadAllNodes;
        rt.wrapperScript = o.wrapperScript;
        return rt;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(final String args) {
        this.args = args;
    }

    public String getCookie() {
        if ("".equals(cookie)) {
            cookie = null;
        }
        return cookie;
    }

    public void setCookie(final String cookie) {
        this.cookie = cookie;
    }

    public String getNodeName() {
        return nodeName + suffix;
    }

    public void setNodeNameSuffix(final String suffix) {
        this.suffix = suffix;
    }

    public void setNodeName(final String nodeName) {
        if (validateNodeName(nodeName)) {
            this.nodeName = nodeName;
        } else {
            // TODO this still can create a name that isn't valid
            this.nodeName = nodeName.replaceAll("[^a-zA-Z0-9_-]", "");
        }
    }

    public boolean isManaged() {
        return managed;
    }

    public void setManaged(final boolean managed) {
        this.managed = managed;
    }

    public List<String> getPathA() {
        return getPathA(DEFAULT_MARKER);
    }

    public List<String> getPathZ() {
        return getPathZ(DEFAULT_MARKER);
    }

    public String getWorkingDir() {
        return workingDir == null || workingDir.length() == 0 ? "."
                : workingDir;
    }

    public void setWorkingDir(final String workingDir) {
        this.workingDir = workingDir;
    }

    @Override
    public String toString() {
        return String.format("Backend<%s/%s (%s) %s [%s]>", getName(),
                getNodeName(), getOtpHome(), version, getArgs());
    }

    public String[] getCmdLine() {
        final List<String> result = getErlCmdLine();

        if (wrapperScript.length > 0) {
            // String erlCmd = Joiner.on(" ").join(result);
            final List<String> erlCmd = Lists.newArrayList(result);
            result.clear();
            for (final String c : wrapperScript) {
                result.add(c);
            }
            result.addAll(erlCmd);
        } else {
            String erl = getOtpHome() + "/bin/erl";
            if (erl.indexOf(' ') >= 0) {
                erl = "\"" + erl + "\"";
            }
            result.add(0, erl);
        }

        return result.toArray(new String[result.size()]);
    }

    private List<String> getErlCmdLine() {
        final List<String> result = new ArrayList<String>();

        for (final String pathA : getPathA()) {
            if (!empty(pathA)) {
                result.add("-pa");
                result.add(pathA);
            }
        }
        for (final String pathZ : getPathZ()) {
            if (!empty(pathZ)) {
                result.add("-pz");
                result.add(pathZ);
            }
        }
        final String gotArgs = getArgs();
        if (!empty(gotArgs)) {
            final String[] xargs = split(gotArgs);
            for (final String a : xargs) {
                result.add(a);
            }
        }

        if (!startShell) {
            result.add("-noshell");
        }

        final boolean globalLongName = System.getProperty("erlide.longname",
                "false").equals("true");
        final String nameTag = useLongName || globalLongName ? "-name"
                : "-sname";
        String nameOption = "";
        if (!getNodeName().equals("")) {
            nameOption = BackendUtil.buildLocalNodeName(getNodeName(),
                    useLongName);
            result.add(nameTag);
            result.add(nameOption);
            final String cky = getCookie();
            if (cky != null) {
                result.add("-setcookie");
                result.add(cky);
            }
        }
        return result;
    }

    /**
     * split on spaces but respect quotes
     * 
     * @param args
     * @return
     */
    private String[] split(final String args) {
        final Pattern p = Pattern.compile("(\"[^\"]*?\"|'[^']*?'|\\S+)");
        final Matcher m = p.matcher(args);
        final List<String> tokens = new ArrayList<String>();
        while (m.find()) {
            tokens.add(m.group(1));
        }
        return tokens.toArray(new String[tokens.size()]);
    }

    private boolean empty(final String str) {
        if (str == null || str.length() == 0) {
            return true;
        }
        return false;
    }

    public String getOtpHome() {
        return homeDir;
    }

    public void setOtpHome(final String otpHome) {
        homeDir = otpHome;
        version = RuntimeVersion.getVersion(otpHome);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<String> getCodePath() {
        return codePath;
    }

    public void setCodePath(final List<String> path) {
        codePath = path;
    }

    protected List<String> getPathA(final String marker) {
        if (codePath != null) {
            final List<String> list = codePath;
            final int i = list.indexOf(marker);
            if (i < 0) {
                return list;
            }
            return list.subList(0, i);
        }
        return Collections.emptyList();
    }

    protected List<String> getPathZ(final String marker) {
        if (codePath != null) {
            final List<String> list = codePath;
            final int i = list.indexOf(marker);
            if (i < 0) {
                return Collections.emptyList();
            }
            return list.subList(i + 1, codePath.size());
        }
        return Collections.emptyList();
    }

    public static boolean validateNodeName(final String name) {
        return name != null
                && name.matches("[a-zA-Z0-9_-]+(@[a-zA-Z0-9_.-]+)?");
    }

    public static boolean validateLocation(final String path) {
        final String v = RuntimeVersion.getRuntimeVersion(path);
        return v != null;
    }

    static String readstring(final InputStream is) {
        try {
            is.read();
            byte[] b = new byte[2];
            is.read(b);
            final int len = b[0] * 256 + b[1];
            b = new byte[len];
            is.read(b);
            final String s = new String(b);
            return s;
        } catch (final IOException e) {
            return null;
        }
    }

    public static boolean isValidOtpHome(final String otpHome) {
        // Check if it looks like a ERL_TOP location:
        if (otpHome == null) {
            return false;
        }
        if (otpHome.length() == 0) {
            return false;
        }
        final File d = new File(otpHome);
        if (!d.isDirectory()) {
            return false;
        }

        final boolean hasErl = hasExecutableFile(otpHome + "/bin/erl");

        final File lib = new File(otpHome + "/lib");
        final boolean hasLib = lib.isDirectory() && lib.exists();

        return hasErl && hasLib;
    }

    private static boolean hasExecutableFile(final String fileName) {
        final File simpleFile = new File(fileName);
        final File exeFile = new File(fileName + ".exe");
        return simpleFile.exists() || exeFile.exists();
    }

    public static boolean hasCompiler(final String otpHome) {
        // Check if it looks like a ERL_TOP location:
        if (otpHome == null) {
            return false;
        }
        if (otpHome.length() == 0) {
            return false;
        }
        final File d = new File(otpHome);
        if (!d.isDirectory()) {
            return false;
        }

        final boolean hasErlc = hasExecutableFile(otpHome + "/bin/erlc");
        return hasErlc;
    }

    protected static String cvt(final Collection<String> path) {
        final StringBuilder result = new StringBuilder();
        for (String s : path) {
            if (s.length() > 0) {
                if (s.contains(" ")) {
                    s = "\"" + s + "\"";
                }
                result.append(s).append(';');
            }
        }
        return result.toString();
    }

    public RuntimeVersion getVersion() {
        return version;
    }

    public void useLongName(final boolean longName) {
        useLongName = longName;
    }

    public boolean getLongName() {
        return useLongName;

    }

    public void setStartShell(final boolean startShell) {
        this.startShell = startShell;
    }

    public boolean isStartShell() {
        return startShell;
    }

    public void hasConsole(final boolean console) {
        this.console = console;
    }

    public boolean hasConsole() {
        return console;
    }

    public void setLoadAllNodes(final boolean loadAllNodes) {
        this.loadAllNodes = loadAllNodes;
    }

    public boolean loadOnAllNodes() {
        return loadAllNodes;
    }

    public void setWrapperScript(final String wrapper) {
        // XXX: can't have quoted args with spaces
        if (wrapper == null) {
            wrapperScript = NOTHING;
        } else {
            wrapperScript = wrapper.split(" ");
        }
    }

    /**
     * Wrapper script to start erl on remote nodes. Must have as last arg the
     * erlang executable to be called. Erl command line is appended by erlide to
     * the provided value. The script must output the hostname where the node
     * was started on the console and wait for it to end.
     * <p>
     * If this is to be used for other backends than "build", the remote system
     * must share filesystem with this one, so that it can access the project
     * resources.
     * <p>
     * Example: <code> ssh user@other_host /path/to/erl </code> will result in
     * 
     * <pre>
     * > ssh user@other_host /path/to/erl -name asd@other_host -noshell more args
     * other_host
     * </pre>
     */
    public String getWrapperScript() {
        return Joiner.on(" ").join(wrapperScript);
    }

}
