/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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
package com.ibm.ws.logging.json.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Enable JSON logging in messages.log with environment variables in server.env
 */
@RunWith(FATRunner.class)
public class JsonConfigBootstrapTest {

    protected static final Class<?> c = JsonConfigBootstrapTest.class;

    @Server("com.ibm.ws.logging.json.JsonConfigServer")
    public static LibertyServer server;

    @Server("com.ibm.ws.logging.json.JsonConfigServer2")
    public static LibertyServer appsWriteJsonServer;

    public static final String APP_NAME = "LogstashApp";
    public static final String SERVER_XML_CLLERROR = "consoleLogLevelError.xml";
    public static final String SERVER_XML_CLLWARNING = "consoleLogLevelWarning.xml";
    public static final String SERVER_XML_CLLINFO_BASIC_CONSOLE = "consoleLogLevelInfoBasicConsole.xml";
    public static final String SERVER_XML_BASIC_CONSOLE_JSON_MESSAGE = "basicConsoleJsonMessage.xml";
    public static final String SERVER_XML_JSON_CONSOLE_JSON_MESSAGE = "jsonConsoleJsonMessage.xml";
    public static final String SERVER_XML_BASIC_CONSOLE_BASIC_MESSAGE = "basicConsoleBasicMessage.xml";
    public static final String SERVER_XML_BASIC_CONSOLE_JSON_MESSAGE_MAXFILESIZE = "basicConsoleJsonMessageMaxFileSize.xml";
    public static final String SERVER_XML_BASIC_CONSOLE_JSON_MESSAGE_ACCESSTRACE = "basicConsoleJsonMessageTraceAccess.xml";
    public static final String SERVER_XML_JSON_CONSOLE_BASIC_MESSAGE = "jsonConsoleLogServer.xml";
    public static final String SERVER_XML_JSON_CONSOLE_JSON_MESSAGE_TRACE_ENABLED = "jsonConsoleJsonMessageTraceEnabled.xml";
    public static final String SERVER_XML_JSON_MESSAGE_MAXFILESIZE = "jsonMessageMaxFileSize.xml";
    public static final String SERVER_XML_TRACE_CONSOLE_ACCESSTRACE_MESSAGE = "traceConsoleAccessTraceMessage.xml";
    public static final String SERVER_XML_CLEAR_LOGGING_SOURCES = "clearLoggingSources.xml";
    public static final String SERVER_XML_JSON_CONSOLE_SOURCE_CASE_INSENSITIVE = "jsonConsoleSourceCaseInsensitive.xml";
    public static final String SERVER_XML_BASIC = "basicServer.xml";
    public static final String SERVER_XML_JSON_SOURCE_MESSAGETRACEACCESS = "jsonSourceMessageTraceAccess.xml";
    public static final String SERVER_XML_JSON_MESSAGE_ACCESS = "jsonMessageSourceAccessLog.xml";
    public static final String SERVER_XML_JSON_MESSAGE_ACCESS_CASE_INSENSITIVE = "jsonMessageSourceAccessLogCaseInsensitive.xml";
    public static final String SERVER_XML_JSON_CONFIG_FIELD_EXT = "jsonConfigFieldExt.xml";
    public static final String SERVER_XML_APPS_WRITE_JSON_ENABLED = "appsWriteJsonEnabled.xml";

    private static final String SIMPLE_FORMAT = "simple";
    private static final String JSON_FORMAT = "json";
    private static final String[] EXPECTED_FAILURES = { "SRVE0777E", "SRVE0315E", "CWWKG0032W", "CWWKG0075E" };
    private static final long BytesInMB = 1048576;

    ArrayList<String> ALL_SOURCE_LIST = new ArrayList<String>(Arrays.asList("message", "trace", "accesslog", "ffdc"));

    @BeforeClass
    public static void setUpClass() throws Exception {
        ShrinkHelper.defaultApp(appsWriteJsonServer, APP_NAME, "com.ibm.logs");
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.logs");

        // Preserve the original server configuration
        server.saveServerConfiguration();
        appsWriteJsonServer.saveServerConfiguration();
    }

    /*
     * Check if precedence of appsWriteJson is honored
     * WLP_LOGGING_APPS_WRITE_JSON=true in server.env
     * com.ibm.ws.logging.apps.write.json=false in bootstrap.properties
     * appsWriteJson=true in server.xml
     */
    @Test
    public void testAppsWriteJsonPrecedence() throws Exception {
        RemoteFile bootstrapFile = appsWriteJsonServer.getServerBootstrapPropertiesFile();
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);
        try {
            // Set appsWriteJson to false in bootstrap.properties
            setInBootstrapPropertiesFile(appsWriteJsonServer, bootstrapFile, "com.ibm.ws.logging.apps.write.json", "false");
            appsWriteJsonServer.startServer();

            RemoteFile consoleLogFile = appsWriteJsonServer.getConsoleLogFile();
            RemoteFile messageLogFile = appsWriteJsonServer.getDefaultLogFile();

            runApplication(appsWriteJsonServer, consoleLogFile);

            //check output are in Liberty JSON format
            checkLine(appsWriteJsonServer, "\\{.*\"message\":\".*key.*value.*\".*\\}", messageLogFile);
            checkLine(appsWriteJsonServer, "\\{.*\"message\":\".*key.*value.*,.*loglevel.*System.err.*\".*\\}", messageLogFile);
            checkLine(appsWriteJsonServer, "\\{.*\"message\":\".*\".*\\}", messageLogFile);
            checkLine(appsWriteJsonServer, "\\{.*\"message\":\".*key.*value.*\".*\\}", consoleLogFile);
            checkLine(appsWriteJsonServer, "\\{.*\"message\":\".*key.*value.*,.*loglevel.*System.err.*\".*\\}", consoleLogFile);
            checkLine(appsWriteJsonServer, "\\{.*\"message\":\".*\".*\\}", consoleLogFile);

            //set appsWriteJson=true in server.xml
            //something wrong with updating this
            setServerConfig(appsWriteJsonServer, SERVER_XML_APPS_WRITE_JSON_ENABLED);

            runApplication(appsWriteJsonServer, consoleLogFile);

            checkLine(appsWriteJsonServer, "\\{\"key\":\"value\"\\}", messageLogFile);
            checkLine(appsWriteJsonServer, "\\{\"key\":\"value\",\"loglevel\":\"System.err\"\\}", messageLogFile);
            checkLine(appsWriteJsonServer, "\\{\\}", messageLogFile);
            checkLine(appsWriteJsonServer, "\\{\"key\":\"value\"\\}", consoleLogFile);
            checkLine(appsWriteJsonServer, "\\{\"key\":\"value\",\"loglevel\":\"System.err\"\\}", consoleLogFile);
            checkLine(appsWriteJsonServer, "\\{\\}", consoleLogFile);

        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }

    }

    /*
     * This test verifies messageFormat when the attribute is set in all server.env, bootstrap.properties and server.xml
     * server.env: WLP_LOGGING_MESSAGE_FORMAT=json
     * bootstrap.properties: com.ibm.ws.logging.message.format=simple
     * server.xml: <logging messageFormat="json"/>
     */
    @Test
    public void testMessageFormatInEnvPropertiesXML() throws Exception {

        // Get the bootstrap.properties file and store the original content
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);

        try {
            // Stop server, if running...
            // Set messageFormat to simple in bootstrap.properties
            // Start the server with the server.env file configured with the consoleFormat=json
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.message.format", SIMPLE_FORMAT);
            server.startServer();
            RemoteFile consoleLogFile = server.getConsoleLogFile();
            runApplication(server, consoleLogFile);

            // Check in messages.log file to see if the message is formatted in the simple format
            ArrayList<String> messageSourceList = new ArrayList<String>(Arrays.asList("message"));
            checkMessageLogUpdate(false, messageSourceList, "");

            // Set messageFormat to JSON in server.xml
            setServerConfig(server, SERVER_XML_BASIC_CONSOLE_JSON_MESSAGE);

            runApplication(server, consoleLogFile);

            // Check in messages.log file to see if the message is formatted in the json format
            checkMessageLogUpdate(true, messageSourceList, "");

        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }
    }

    /*
     * This test verifies messageSource when the attribute is set in all server.env, bootstrap.properties and server.xml
     * server.env: WLP_LOGGING_MESSAGE_SOURCE=accesslog
     * bootstrap.properties: com.ibm.ws.logging.message.source=message
     * server.xml: <logging messageFormat="json" messageSource="accesslog"/>
     */
    @Test
    public void testMessageSourceInEnvPropertiesXML() throws Exception {

        // Get the bootstrap.properties file and store the original content
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);

        try {

            // Set messageSource to message in bootstrap.properties
            // Start the server with the server.env file configured with the messageSource=message,trace
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.message.format", JSON_FORMAT);
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.message.source", "message");
            server.startServer();
            RemoteFile consoleLogFile = server.getConsoleLogFile();
            runApplication(server, consoleLogFile);

            // Check in messages.log file to see if only message is enabled
            ArrayList<String> messageSourceList = new ArrayList<String>(Arrays.asList("message"));
            checkMessageLogUpdate(true, messageSourceList, "");

            // Set messageSource to accessLogging in server.xml
            setServerConfig(server, SERVER_XML_JSON_MESSAGE_ACCESS);

            runApplication(server, consoleLogFile);

            // Check in messages.log file to see if the format is in json format and has accessLogging enabled
            messageSourceList = new ArrayList<String>(Arrays.asList("accesslog"));
            checkMessageLogUpdate(true, messageSourceList, "");

        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }
    }

    /*
     * This test verifies messageSource when the attribute's values are case insensitive
     * bootstrap.properties: com.ibm.ws.logging.message.source=MESSAGE
     * server.xml: <logging messageFormat="json" messageSource="ACCESSlog"/>
     */
    @Test
    public void testMessageSourceValuesCaseInsensitive() throws Exception {

        // Get the bootstrap.properties file and store the original content
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);

        try {

            // Set messageSource to message in bootstrap.properties
            // Start the server with the server.env file configured with the messageSource=message,trace
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.message.format", JSON_FORMAT);
            // Set message source value to all upper case letters
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.message.source", "MESSAGE");
            server.startServer();
            RemoteFile consoleLogFile = server.getConsoleLogFile();
            runApplication(server, consoleLogFile);

            // Check in messages.log file to see if only message is enabled
            ArrayList<String> messageSourceList = new ArrayList<String>(Arrays.asList("message"));
            checkMessageLogUpdate(true, messageSourceList, "");

            // Set messageSource to accessLogging in server.xml
            setServerConfig(server, SERVER_XML_JSON_MESSAGE_ACCESS_CASE_INSENSITIVE);

            runApplication(server, consoleLogFile);

            // Check in messages.log file to see if the format is in json format and has accessLogging enabled
            messageSourceList = new ArrayList<String>(Arrays.asList("accesslog"));
            checkMessageLogUpdate(true, messageSourceList, "");

        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }
    }

    /*
     * This test verifies consoleSource when the attribute is set in all server.env, bootstrap.properties and server.xml
     * server.env: WLP_LOGGING_CONSOLE_SOURCE=trace
     * bootstrap.properties: com.ibm.ws.logging.console.source=message
     * server.xml: <logging consoleFormat="json" consoleSource="trace"/>
     */
    @Test
    public void testConsoleSourceInEnvPropertiesXML() throws Exception {

        // Get the bootstrap.properties file and store the original content
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);

        try {
            // Set the consoleSource to message in bootstrap.properties
            // Start the server with the server.env file configured with the consoleSource=message,trace
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.console.format", JSON_FORMAT);
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.console.source", "message");
            server.startServer();
            RemoteFile consoleLogFile = server.getConsoleLogFile();
            runApplication(server, consoleLogFile);

            // Check in console.log file to see if the format is in json format and has message enabled
            ArrayList<String> consolesourceList = new ArrayList<String>(Arrays.asList("message"));
            checkConsoleLogUpdate(true, consoleLogFile, "INFO", consolesourceList, "");

            // Set the consoleFormat to JSON in server.xml
            setServerConfig(server, SERVER_XML_CLEAR_LOGGING_SOURCES);

            runApplication(server, consoleLogFile);

            // Check in console.log file to see if the format is in json format and has trace enabled
            consolesourceList = new ArrayList<String>(Arrays.asList("trace"));
            checkConsoleLogUpdate(true, consoleLogFile, "INFO", consolesourceList, "");

            //set server.xml to a basic config so that when server stops, it can successfully check for CWWKE0036I in console log
            setServerConfig(server, SERVER_XML_BASIC);

        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }
    }

    /*
     * This test verifies consoleSource when the attribute's values are case insensitive
     * bootstrap.properties: com.ibm.ws.logging.console.source=MESSAGE
     * server.xml: <logging consoleFormat="json" consoleSource="tRaCe"/>
     */
    @Test
    public void testConsoleSourceValuesCaseInsensitive() throws Exception {

        // Get the bootstrap.properties file and store the original content
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);

        try {
            // Set the consoleSource to message in bootstrap.properties
            // Start the server with the server.env file configured with the consoleSource=message,trace
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.console.format", JSON_FORMAT);
            // Set console source value to all upper case letters
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.console.source", "MESSAGE");
            server.startServer();
            RemoteFile consoleLogFile = server.getConsoleLogFile();
            runApplication(server, consoleLogFile);

            // Check in console.log file to see if the format is in json format and has message enabled
            ArrayList<String> consolesourceList = new ArrayList<String>(Arrays.asList("message"));
            checkConsoleLogUpdate(true, consoleLogFile, "INFO", consolesourceList, "");

            // Set the consoleFormat to JSON in server.xml
            setServerConfig(server, SERVER_XML_JSON_CONSOLE_SOURCE_CASE_INSENSITIVE);

            runApplication(server, consoleLogFile);

            // Check in console.log file to see if the format is in json format and has trace enabled
            consolesourceList = new ArrayList<String>(Arrays.asList("trace"));
            checkConsoleLogUpdate(true, consoleLogFile, "INFO", consolesourceList, "");

            //set server.xml to a basic config so that when server stops, it can successfully check for CWWKE0036I in console log
            setServerConfig(server, SERVER_XML_BASIC);

        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }
    }

    /*
     * This test verifies consoleLogLevel when the attribute is set in all server.env, bootstrap.properties and server.xml
     * server.env: WLP_LOGGING_CONSOLE_LOGLEVEL=INFO
     * bootstrap.properties: com.ibm.ws.logging.console.log.level=INFO
     * server.xml: <logging consoleLogLevel="ERROR" />
     */
    @Test
    public void testConsoleLogLevelInEnvPropertiesXML() throws Exception {

        // Get the bootstrap.properties file and store the original content
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);

        try {
            // Set consoleLogLevel to WARNING in bootstrap.properties
            // Start the server with the server.env file configured with the consoleLogLevel=INFO
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.console.log.level", "INFO");
            server.startServer();
            RemoteFile consoleLogFile = server.getConsoleLogFile();
            runApplication(server, consoleLogFile);

            // Check in console.log file to see consoleLogLevel is set to INFO
            checkConsoleLogUpdate(true, consoleLogFile, "INFO", ALL_SOURCE_LIST, "");

            // Set consoleLogLevel to ERROR in server.xml
            setServerConfig(server, SERVER_XML_CLLERROR);

            runApplication(server, consoleLogFile);

            // Check in console.log file to see consoleLogLevel is set to WARNING
            checkConsoleLogUpdate(true, consoleLogFile, "ERROR", ALL_SOURCE_LIST, "");

            //set server.xml to a basic config so that when server stops, it can successfully check for CWWKE0036I in console log
            setServerConfig(server, SERVER_XML_BASIC);

        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }
    }

    /*
     * This test verifies maxFileSize when the attribute is set in both bootstrap.properties and server.xml
     * server.xml: <logging messageFormat="json" maxFileSize="1"/>
     * bootstrap.properties: com.ibm.ws.logging.max.file.size=100
     */
    @Test
    public void testMaxFileSizeLevelInPropertiesXML() throws Exception {

        // Get the bootstrap.properties file and store the original content
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);

        try {
            // Set maxFileSize to 100 in bootstrap.properties
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.max.file.size", "100");
            server.startServer();
            // Set maxFileSize to 1 in server.xml
            setServerConfig(server, SERVER_XML_JSON_MESSAGE_MAXFILESIZE);

            RemoteFile consoleLogFile = server.getConsoleLogFile();
            runApplication(server, consoleLogFile);

            RemoteFile messageLogFile = server.getDefaultLogFile();
            long logFileSize = messageLogFile.length();
            assertTrue("The maxFileSize for messages.log is greater than 1", logFileSize <= BytesInMB);

        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }
    }

    /*
     * Test enabling com.ibm.ws.logging.apps.write.json in bootstrap.properties
     */
    @Test
    public void testEnableAppsWriteJsonInProperties() throws Exception {

        // Get the bootstrap.properties file and store the original content
        RemoteFile bootstrapFile = server.getServerBootstrapPropertiesFile();
        FileInputStream in = getFileInputStreamForRemoteFile(bootstrapFile);
        Properties initialBootstrapProps = loadProperties(in);

        try {
            // Set appsWriteJson to true in bootstrap.properties
            setInBootstrapPropertiesFile(server, bootstrapFile, "com.ibm.ws.logging.apps.write.json", "true");
            server.startServer();

            RemoteFile consoleLogFile = server.getConsoleLogFile();
            RemoteFile messageLogFile = server.getDefaultLogFile();
            runApplication(server, consoleLogFile);

            //check output are in application's JSON format
            checkLine("\\{\"key\":\"value\"\\}", messageLogFile);
            checkLine("\\{\"key\":\"value\",\"loglevel\":\"System.err\"\\}", messageLogFile);
            checkLine("\\{\"key\":\"value\"\\}", consoleLogFile);
            checkLine("\\{\"key\":\"value\",\"loglevel\":\"System.err\"\\}", consoleLogFile);

        } finally {
            // Restore the initial contents of bootstrap.properties
            FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, false);
            writeProperties(initialBootstrapProps, out);
        }
    }

    /*
     * Test enabling WLP_LOGGING_APPS_WRITE_JSON in environment
     */
    @Test
    public void testEnableAppsWriteJsonEnv() throws Exception {

        appsWriteJsonServer.startServer();

        RemoteFile consoleLogFile = appsWriteJsonServer.getConsoleLogFile();
        RemoteFile messageLogFile = appsWriteJsonServer.getDefaultLogFile();

        runApplication(appsWriteJsonServer, consoleLogFile);

        //check output are in application's JSON format
        checkLine(appsWriteJsonServer, "\\{\"key\":\"value\"\\}", messageLogFile);
        checkLine(appsWriteJsonServer, "\\{\"key\":\"value\",\"loglevel\":\"System.err\"\\}", messageLogFile);
        checkLine(appsWriteJsonServer, "\\{\"key\":\"value\"\\}", consoleLogFile);
        checkLine(appsWriteJsonServer, "\\{\"key\":\"value\",\"loglevel\":\"System.err\"\\}", consoleLogFile);

    }

    private void checkMessageLogUpdate(boolean isJson, ArrayList<String> sourceList, String traceSpec) throws Exception {
        if (isJson) {
            if (sourceList.contains("trace") && traceSpec.equals("finest")) {
                checkLine("\\{.*\"loglevel\":\"ENTRY\".*\\}");
                checkLine("\\{.*\"loglevel\":\"CONFIG\".*\\}");
                checkLine("\\{.*\"loglevel\":\"FINE\".*\\}");
                checkLine("\\{.*\"loglevel\":\"FINER\".*\\}");
                checkLine("\\{.*\"loglevel\":\"FINEST\".*\\}");
                checkLine("\\{.*\"loglevel\":\"EXIT\".*\\}");
            }
            if (sourceList.contains("message")) {
                checkLine("\\{.*\"loglevel\":\"SEVERE\".*\\}");
                checkLine("\\{.*\"loglevel\":\"WARNING\".*\\}");
                checkLine("\\{.*\"loglevel\":\"SystemOut\".*\\}");
                checkLine("\\{.*\"loglevel\":\"SystemErr\".*\\}");
            }
            if (sourceList.contains("accesslog")) {
                checkLine("\\{.*\"type\":\"liberty_accesslog\".*\\}");
            }
        } else if (!isJson) {
            checkLine("E severe message");
            checkLine("W warning message");
            checkLine("I info message");
            checkLine("O System.out.println");
            checkLine("R System.err.println");
        }
    }

    private void checkConsoleLogUpdate(boolean isJson, RemoteFile consoleLogFile, String consoleLogLevel, ArrayList<String> sourceList,
                                       String traceSpec) throws Exception {
        //runApp
        if (isJson) {
            if (sourceList.contains("trace") && traceSpec.equals("finest")) {
                checkLine("\\{.*\"loglevel\":\"ENTRY\".*\\}");
            }
            if (sourceList.contains("message")) {
                checkLine("\\{.*\"loglevel\":\"SEVERE\".*\\}", consoleLogFile);
                //check consolelogLevel
                if (consoleLogLevel.equals("WARNING") || consoleLogLevel.equals("INFO")) {
                    checkLine("\\{.*\"loglevel\":\"WARNING\".*\\}", consoleLogFile);
                }
                checkLine("\\{.*\"loglevel\":\"SystemOut\".*\\}", consoleLogFile);
                checkLine("\\{.*\"loglevel\":\"SystemErr\".*\\}", consoleLogFile);
            }
            if (sourceList.contains("trace") && traceSpec.equals("finest")) {
                checkLine("\\{.*\"loglevel\":\"CONFIG\".*\\}");
                checkLine("\\{.*\"loglevel\":\"FINE\".*\\}");
                checkLine("\\{.*\"loglevel\":\"FINER\".*\\}");
                checkLine("\\{.*\"loglevel\":\"FINEST\".*\\}");
                checkLine("\\{.*\"loglevel\":\"EXIT\".*\\}");
            }
            if (sourceList.contains("accesslog")) {
                checkLine("\\{.*\"type\":\"liberty_accesslog\".*\\}", consoleLogFile);
            }
        } else if (!isJson) {
            //loglevel
            checkLine("\\[ERROR   \\] severe message", consoleLogFile);
            //check console log level
            if (consoleLogLevel.equals("WARNING") || consoleLogLevel.equals("INFO")) {
                checkLine("\\[WARNING \\] warning message", consoleLogFile);
            }
            checkLine("\\[INFO    \\] info message", consoleLogFile);
            checkLine("System.out.println", consoleLogFile);
            checkLine("\\[err\\] System.err.println", consoleLogFile);
        }
    }

    private FileInputStream getFileInputStreamForRemoteFile(RemoteFile bootstrapPropFile) throws Exception {
        FileInputStream input = null;
        try {
            input = (FileInputStream) bootstrapPropFile.openForReading();
        } catch (Exception e) {
            throw new Exception("Error while getting the FileInputStream for the remote bootstrap properties file.");
        }
        return input;
    }

    private Properties loadProperties(FileInputStream input) throws IOException {
        Properties props = new Properties();
        try {
            props.load(input);
        } catch (IOException e) {

            throw new IOException("Error while loading properties from the remote bootstrap properties file.");
        } finally {
            try {
                input.close();
            } catch (IOException e1) {
                throw new IOException("Error while closing the input stream.");
            }
        }
        return props;
    }

    private FileOutputStream getFileOutputStreamForRemoteFile(RemoteFile bootstrapPropFile, boolean append) throws Exception {
        // Open the remote file for writing with append as false
        FileOutputStream output = null;
        try {
            output = (FileOutputStream) bootstrapPropFile.openForWriting(append);
        } catch (Exception e) {
            throw new Exception("Error while getting FileOutputStream for the remote bootstrap properties file.");
        }
        return output;
    }

    private void writeProperties(Properties props, FileOutputStream output) throws Exception {
        // Write the properties to remote bootstrap properties file
        try {
            props.store(output, null);
        } catch (IOException e) {
            throw new Exception("Error while writing to the remote bootstrap properties file.");
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                throw new IOException("Error while closing the output stream.");
            }
        }
    }

    private void setInBootstrapPropertiesFile(LibertyServer server, RemoteFile bootstrapFile, String key, String value) throws Exception {
        // Set server.xml to basic
        server.setServerConfigurationFile(SERVER_XML_BASIC);

        // Update bootstrap.properties file with consoleFormat=simple
        Properties newBootstrapProps = new Properties();
        newBootstrapProps.put(key, value);

        FileOutputStream out = getFileOutputStreamForRemoteFile(bootstrapFile, true);
        writeProperties(newBootstrapProps, out);
    }

    private void checkLine(String message, RemoteFile remoteFile) throws Exception {
        String line = server.waitForStringInLog(message, remoteFile);
        assertNotNull("Cannot find" + message + "from messages.log", line);
    }

    private void checkLine(String message) throws Exception {
        String line = server.waitForStringInLog(message, server.getDefaultLogFile());
        assertNotNull("Cannot find" + message + "from JsonConfigTest.log", line);
    }

    private void checkLine(LibertyServer server, String message, RemoteFile remoteFile) throws Exception {
        String line = server.waitForStringInLog(message, remoteFile);
        assertNotNull("Cannot find" + message + "from messages.log", line);
    }

    private void runApplication(LibertyServer server, RemoteFile consoleLogFile) throws Exception {
        server.setMarkToEndOfLog(consoleLogFile);
        TestUtils.runApp(server, "logServlet");
    }

    private static void setServerConfig(LibertyServer server, String fileName) throws Exception {
        RemoteFile log = server.getDefaultTraceFile();
        server.setMarkToEndOfLog(log);
        server.setServerConfigurationFile(fileName);
        Assert.assertNotNull(server.waitForStringInLog("CWWKG0017I.*|CWWKG0018I.*", log));
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }

        if (appsWriteJsonServer != null && appsWriteJsonServer.isStarted()) {
            appsWriteJsonServer.stopServer(EXPECTED_FAILURES);
        }
    }

    @AfterClass
    public static void tearDownClass() {
        if ((server != null) && (server.isStarted())) {
            try {
                server.stopServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (appsWriteJsonServer != null && appsWriteJsonServer.isStarted()) {
            try {
                appsWriteJsonServer.stopServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}