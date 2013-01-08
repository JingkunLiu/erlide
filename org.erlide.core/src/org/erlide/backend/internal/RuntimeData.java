package org.erlide.backend.internal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.annotation.Nullable;
import org.erlide.launch.ErlLaunchAttributes;
import org.erlide.launch.debug.ErlDebugConstants;
import org.erlide.runtime.HostnameUtils;
import org.erlide.runtime.IRuntimeData;
import org.erlide.runtime.InitialCall;
import org.erlide.runtime.runtimeinfo.RuntimeInfo;
import org.erlide.runtime.runtimeinfo.RuntimeInfoCatalog;
import org.erlide.utils.Asserts;
import org.erlide.utils.ErlLogger;
import org.erlide.utils.SystemConfiguration;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class RuntimeData implements IRuntimeData {
    protected String cookie;
    protected boolean managed;
    protected boolean restartable;
    protected boolean startShell;
    protected boolean console;
    protected List<String> interpretedModules;
    protected String runtimeName;
    protected String nodeName;
    protected boolean longName;
    protected String extraArgs;
    protected String workingDir;
    protected Map<String, String> env;
    protected InitialCall initialCall;
    protected int debugFlags;
    protected boolean loadOnAllNodes;
    protected boolean internal;
    protected RuntimeInfo runtimeInfo;
    protected boolean debug;
    protected boolean reportErrors = false;

    public RuntimeData() {
        cookie = "";
        managed = true;
        restartable = false;
        startShell = true;
        console = true;
        interpretedModules = Lists.newArrayList();
        runtimeName = "";
        nodeName = "";
        longName = true;
        extraArgs = "";
        workingDir = ErlLaunchAttributes.DEFAULT_WORKING_DIR;
        env = Maps.newHashMap();
        initialCall = null;
        debugFlags = ErlDebugConstants.DEFAULT_DEBUG_FLAGS;
        loadOnAllNodes = false;
        internal = false;
        interpretedModules = Lists.newArrayList();
        runtimeInfo = null;
        debug = false;
    }

    public RuntimeData(final RuntimeInfo runtime,
            final ILaunchConfiguration config, final String mode) {
        this();
        this.runtimeInfo = runtime;
    }

    public RuntimeData(final RuntimeInfoCatalog runtimeInfoManager,
            final RuntimeInfo info, final String defaultWorkingDir) {
        this();
        Asserts.isNotNull(info, "Can't create backend with no runtime info");

        runtimeInfo = info;
        setRuntimeName(info.getName());
        setCookie("erlide");
        setLongName(true);

        setWorkingDir(defaultWorkingDir);
        setExtraArgs(info.getArgs());

        setConsole(true);
        setLoadAllNodes(false);
    }

    @Override
    public String getCookie() {
        return cookie;
    }

    @Override
    public void setCookie(final String cookie) {
        this.cookie = cookie.trim();
    }

    @Override
    public boolean isManaged() {
        return managed;
    }

    @Override
    public void setManaged(final boolean managed) {
        this.managed = managed;
    }

    @Override
    public boolean isRestartable() {
        return restartable;
    }

    @Override
    public void setRestartable(final boolean restartable) {
        this.restartable = restartable;
    }

    @Override
    public boolean useStartShell() {
        return startShell;
    }

    @Override
    public void setUseStartShell(final boolean shell) {
        this.startShell = shell;
    }

    @Override
    public boolean hasConsole() {
        return console;
    }

    @Override
    public void setConsole(final boolean console) {
        this.console = console;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    @Override
    public List<String> getInterpretedModules() {
        return interpretedModules;
    }

    @Override
    public void setInterpretedModules(final List<String> interpretedModules) {
        this.interpretedModules = interpretedModules;
    }

    @Override
    public String getRuntimeName() {
        return runtimeName;
    }

    @Override
    public void setRuntimeName(final String name) {
        this.runtimeName = name;
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

    @Override
    public void setNodeName(String nodeName) {
        if (!validateNodeName(nodeName)) {
            // TODO this still can create a name that isn't valid
            nodeName = nodeName.replaceAll("[^a-zA-Z0-9_-]", "");
        }
        this.nodeName = nodeName;
    }

    public static boolean validateNodeName(final String name) {
        return name != null
                && name.matches("[a-zA-Z0-9_-]+(@[a-zA-Z0-9_.-]+)?");
    }

    @Override
    public boolean isLongName() {
        return longName;
    }

    @Override
    public void setLongName(final boolean longname) {
        this.longName = longname;
    }

    @Override
    public String getExtraArgs() {
        return extraArgs;
    }

    @Override
    public void setExtraArgs(final String xtra) {
        this.extraArgs = xtra;
    }

    @Override
    public String getWorkingDir() {
        return workingDir;
    }

    @Override
    public void setWorkingDir(final String dir) {
        this.workingDir = dir;
    }

    @Override
    public Map<String, String> getEnv() {
        return env;
    }

    protected InitialCall getInitialCall(final ILaunchConfiguration config)
            throws CoreException {
        final String module = config.getAttribute(ErlLaunchAttributes.MODULE,
                "");
        final String function = config.getAttribute(
                ErlLaunchAttributes.FUNCTION, "");
        final String args = config.getAttribute(ErlLaunchAttributes.ARGUMENTS,
                "");
        return new InitialCall(module, function, args);
    }

    @Override
    @Nullable
    public InitialCall getInitialCall() {
        return initialCall;
    }

    @Override
    public RuntimeInfo getRuntimeInfo() {
        return runtimeInfo;
    }

    @Override
    public int getDebugFlags() {
        return debugFlags;
    }

    @Override
    public boolean shouldLoadOnAllNodes() {
        return loadOnAllNodes;
    }

    @Override
    public void setLoadAllNodes(final boolean load) {
        this.loadOnAllNodes = load;
    }

    @Override
    public boolean isInternal() {
        return internal;
    }

    @Override
    public void setInternal(final boolean internal) {
        this.internal = internal;
    }

    @Override
    public String[] getCmdLine() {
        final RuntimeInfo r = getRuntimeInfo();
        final List<String> result = new ArrayList<String>();

        if (hasDetachedConsole() && !isInternal()) {
            if (SystemConfiguration.getInstance().isOnWindows()) {
                result.add("cmd.exe");
                result.add("/c");
                result.add("start");
            } else {
                final String command = System.getenv().get("TERM");
                result.add(command);
                result.add("-e");
            }
        }

        String erl = r.getOtpHome() + "/bin/erl";
        if (erl.indexOf(' ') >= 0) {
            erl = "\"" + erl + "\"";
        }
        result.add(erl);
        for (final String path : r.getCodePath()) {
            if (!Strings.isNullOrEmpty(path)) {
                result.add("-pa");
                result.add(path);
            }
        }
        if (!useStartShell()) {
            result.add("-noshell");
        }

        if (!getNodeName().equals("")) {
            final String nameTag = isLongName() ? "-name" : "-sname";
            String nameOption = getNodeName();
            if (!nameOption.contains("@")) {
                nameOption += "@"
                        + HostnameUtils.getErlangHostName(isLongName());
            }
            result.add(nameTag);
            result.add(nameOption);
            final String cky = getCookie();
            if (!Strings.isNullOrEmpty(cky)) {
                result.add("-setcookie");
                result.add(cky);
            }
        }
        final String gotArgs = r.getArgs();
        if (!Strings.isNullOrEmpty(gotArgs)) {
            result.addAll(splitQuoted(gotArgs));
        }
        return result.toArray(new String[result.size()]);
    }

    private boolean hasDetachedConsole() {
        // TODO add GUI for "detached console"
        return "true".equals(System.getProperty("erlide.backend.detached"));
    }

    /**
     * split on spaces but respect quotes
     * 
     * @param theArgs
     * @return
     */
    private Collection<String> splitQuoted(final String theArgs) {
        final Pattern p = Pattern.compile("(\"[^\"]*?\"|'[^']*?'|\\S+)");
        final Matcher m = p.matcher(theArgs);
        final List<String> tokens = new ArrayList<String>();
        while (m.find()) {
            tokens.add(m.group(1));
        }
        return tokens;
    }

    @Override
    public String getQualifiedNodeName() {
        final String erlangHostName = HostnameUtils
                .getErlangHostName(isLongName());
        final String name = getNodeName();
        final boolean hasHost = name.contains("@");
        return hasHost ? name : name + "@" + erlangHostName;
    }

    @Override
    public boolean isReportErrors() {
        return reportErrors;
    }

    @Override
    public void setReportErrors(final boolean value) {
        reportErrors = value;
    }

    public void debugPrint() {
        ErlLogger.info("Data:: " + getClass().getName());
        for (final Field field : getAllPrivateFields(getClass())) {
            try {
                final boolean access = field.isAccessible();
                field.setAccessible(true);
                try {
                    ErlLogger.info("%-20s: %s", field.getName(),
                            field.get(this));
                } finally {
                    field.setAccessible(access);
                }
            } catch (final Exception e) {
                ErlLogger.info("Could not read %s! %s", field.getName(),
                        e.getMessage());
            }
        }
        ErlLogger.info("---------------");
    }

    protected List<Field> getAllPrivateFields(final Class<?> type) {
        final List<Field> result = new ArrayList<Field>();

        Class<?> cls = type;
        while (cls != null && cls != Object.class) {
            for (final Field field : cls.getDeclaredFields()) {
                if (!field.isSynthetic()) {
                    result.add(field);
                }
            }
            cls = cls.getSuperclass();
        }

        return result;
    }
}
