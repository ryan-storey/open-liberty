/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
@Version(Constants.OSGI_VERSION)
@TraceOptions(traceGroup = Constants.TRACE_GROUP, messageBundle = Constants.TRACE_OPENAPI)
package com.ibm.ws.microprofile.openapi.servlet.filter;

import org.osgi.annotation.versioning.Version;

import com.ibm.websphere.ras.annotation.TraceOptions;

import io.openliberty.microprofile.openapi20.internal.utils.Constants;
