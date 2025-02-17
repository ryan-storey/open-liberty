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
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.artifacts;

import java.util.List;

import javax.batch.api.chunk.listener.AbstractItemWriteListener;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

/**
 * Appends # of items written to step exit status.
 * Results in comma-separated sequence.
 */
public class CountingWriteListener extends AbstractItemWriteListener {

    @Inject
    StepContext stepCtx;

    @Override
    public void afterWrite(List<Object> items) throws Exception {
        String es = stepCtx.getExitStatus();
        String chunkSize = Integer.toString(items.size());

        if (es == null) {
            stepCtx.setExitStatus(chunkSize);
        } else {
            stepCtx.setExitStatus(es + "," + chunkSize);
        }
    }
}
