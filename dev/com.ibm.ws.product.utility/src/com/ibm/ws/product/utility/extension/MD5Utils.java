/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.product.utility.extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {

    static final char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    static final MessageDigest messagedigest;

    static {
        try {
            messagedigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            //should not happen
            throw new RuntimeException(e);
        }
    }

    public static String getMD5String(String str) {
        messagedigest.update(str.getBytes());
        return byteArrayToHexString(messagedigest.digest());
    }

    public static String getFileMD5String(File file) throws IOException {
        InputStream fis = null;
        try {
            fis = new FileInputStream(file);

            byte[] buffer = new byte[1024];
            int numRead = 0;
            while ((numRead = fis.read(buffer)) > 0) {
                messagedigest.update(buffer, 0, numRead);
            }
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (Exception e) {
                }
        }

        return byteArrayToHexString(messagedigest.digest());
    }

    private static String byteArrayToHexString(byte[] byteArray) {

        StringBuffer stringbuffer = new StringBuffer(2 * byteArray.length);
        for (int i = 0; i < byteArray.length; i++) {
            char upper = hexDigits[(byteArray[i] & 0xf0) >> 4];
            char lower = hexDigits[byteArray[i] & 0xf];
            stringbuffer.append(upper);
            stringbuffer.append(lower);
        }
        return stringbuffer.toString();

    }

}
