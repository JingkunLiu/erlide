/*******************************************************************************
 * Copyright (c) 2009-2013 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available
 * at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.backend.internal;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.jdt.annotation.NonNull;
import org.erlide.backend.api.BackendData;
import org.erlide.backend.api.IBackendManager;
import org.erlide.runtime.api.IOtpNodeProxy;
import org.erlide.util.ErlLogger;

public class ExternalBackend extends Backend {

    public ExternalBackend(final BackendData data, final @NonNull IOtpNodeProxy runtime,
            final IBackendManager backendManager) {
        super(data, runtime, backendManager);
        assignStreamProxyListeners();
    }

    @Override
    public synchronized void dispose() {
        try {
            final ILaunch launch = getData().getLaunch();
            if (!launch.isTerminated()) {
                launch.terminate();
            }
        } catch (final DebugException e) {
            ErlLogger.error(e);
        }
        super.dispose();
    }

    @Override
    protected IStreamsProxy getStreamsProxy() {
        final IProcess p = getErtsProcess();
        if (p == null) {
            return null;
        }
        return p.getStreamsProxy();
    }

    private IProcess getErtsProcess() {
        final IProcess[] ps = getData().getLaunch().getProcesses();
        if (ps == null || ps.length == 0) {
            return null;
        }
        return ps[0];
    }

}
