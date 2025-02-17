/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.apps.kafkanack;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractDeliveryBean;

import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class KafkaNackTestDeliveryBean extends AbstractDeliveryBean<String> {

    public static final String NACK_TEST_CHANNEL = "nack-test-channel";

    @Outgoing(NACK_TEST_CHANNEL)
    @Override
    public CompletionStage<Message<String>> getMessage() {
        return super.getMessage();
    }

}
