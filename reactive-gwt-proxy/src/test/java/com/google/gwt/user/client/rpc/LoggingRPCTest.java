/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.user.client.rpc;

import com.google.gwt.user.server.rpc.LoggingRPCTestServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for Generic Collections in GWT RPC.
 * <p>
 * Taken from GWT sources. Modified by Antonio Macrì to perform tests against
 * an embedded Jetty with the GWT servlet.
 */
public class LoggingRPCTest extends RpcAsyncTestBase<LoggingRPCTestService, LoggingRPCTestServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(LoggingRPCTestService.class, LoggingRPCTestServiceImpl.class);
    }


    public LoggingRPCTest() {
        super(LoggingRPCTestService.class);
    }


    private static final LogRecord expectedLogRecord = createLogRecord();


    public static boolean isValid(LogRecord value) {
        if (!expectedLogRecord.getLevel().toString()
                .equals(value.getLevel().toString())) {
            return false;
        }

        if (!expectedLogRecord.getMessage().equals(value.getMessage())) {
            return false;
        }

        if (expectedLogRecord.getMillis() != value.getMillis()) {
            return false;
        }

        if (!expectedLogRecord.getLoggerName().equals(value.getLoggerName())) {
            return false;
        }

        Throwable expectedCause = expectedLogRecord.getThrown();
        Throwable valueCause = value.getThrown();
        while (expectedCause != null) {
            if (valueCause == null) {
                return false;
            }

            if (!expectedCause.getMessage().equals(valueCause.getMessage())) {
                return false;
            }

            // Do not compare trace as it is not stable across RPC.

            expectedCause = expectedCause.getCause();
            valueCause = valueCause.getCause();
        }
        return valueCause == null;
    }

    private static LogRecord createLogRecord() {
        LogRecord result = new LogRecord(Level.INFO, "Test Log Record");

        // Only set serialized fields.

        result.setLoggerName("Test Logger Name");
        result.setMillis(1234567);

        Throwable thrown = new Throwable("Test LogRecord Throwable 1", new Throwable("Test LogRecord Throwable cause"));
        result.setThrown(thrown);

        return result;
    }


    @Test
    public void testLogRecord() {
        service.echoLogRecord(expectedLogRecord, waitedCallback(result -> {
            assertNotNull(result);
            assertTrue(isValid(result));
        }));
    }

    @Test
    @Disabled("it doesn't apply")
    public void testStackMapDeobfuscation() {
    }
}
