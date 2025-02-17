/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.security.fat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * Some utility functions for FATs.
 */
public class SecurityFatUtils {

    /**
     * JakartaEE9 transform a list of applications. The applications are the simple app names and they must exist at '<server>/dropins/<appname>'.
     *
     * @param myServer The server to transform the applications on.
     * @param apps     The simple names of the applications to transform.
     */
    public static void transformApps(LibertyServer myServer, String... apps) {
        if (JakartaEEAction.isEE9OrLaterActive()) {
            for (String app : apps) {
                Path someArchive = Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + app);
                JakartaEEAction.transformApp(someArchive);
            }
        }
    }
}
