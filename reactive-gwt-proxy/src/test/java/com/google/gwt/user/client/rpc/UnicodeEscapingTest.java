/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.client.rpc.UnicodeEscapingService.InvalidCharacterException;
import com.google.gwt.user.server.rpc.UnicodeEscapingServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test that any valid string can be sent via RPC in both directions.
 * <p>
 * TODO(jat): make unpaired surrogates work properly if it is possible to do so
 * on all browsers, then add them to this test.
 * <p>
 * Taken from GWT sources. Modified by Antonio Macrì to perform tests against
 * an embedded Jetty with the GWT servlet.
 */
public class UnicodeEscapingTest extends RpcAsyncTestBase<UnicodeEscapingService, UnicodeEscapingServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(UnicodeEscapingService.class, UnicodeEscapingServiceImpl.class, "unicodeEscape");
    }


    public UnicodeEscapingTest() {
        super(UnicodeEscapingService.class, "unicodeEscape");
    }


    /**
     * the size of a block of characters to test.
     */
    private static final int CHARACTER_BLOCK_SIZE = 64;

    /**
     * When doing the non-BMP test, we don't test every block of characters
     * because it takes too long - this is the increment to use. It is not a power
     * of two so we alter the alignment of the block of characters we skip.
     */
    private static final int NON_BMP_TEST_INCREMENT = 8192 + 64;


    /**
     * start of current block being tested.
     */
    protected int current;


    /**
     * Generate strings containing ranges of characters and sends them to the
     * server for verification. This ensures that client->server string escaping
     * properly handles all BMP characters.
     * <p>
     * Unpaired or improperly paired surrogates are not tested here, as some
     * browsers refuse to accept them. Properly paired surrogates are tested in
     * the non-BMP test.
     * <p>
     * Note that this does not test all possible combinations, which might be an
     * issue, particularly with combining marks, though they should be logically
     * equivalent in that case.
     *
     * @throws InvalidCharacterException
     */
    @Test
    public void testClientToServerBMPHigh() throws InvalidCharacterException {
        clientToServerVerifyRange(Character.MAX_SURROGATE + 1, Character.MIN_SUPPLEMENTARY_CODE_POINT,
                CHARACTER_BLOCK_SIZE, CHARACTER_BLOCK_SIZE);
    }

    /**
     * Generate strings containing ranges of characters and sends them to the
     * server for verification. This ensures that client->server string escaping
     * properly handles all BMP characters.
     * <p>
     * Unpaired or improperly paired surrogates are not tested here, as some
     * browsers refuse to accept them. Properly paired surrogates are tested in
     * the non-BMP test.
     * <p>
     * Note that this does not test all possible combinations, which might be an
     * issue, particularly with combining marks, though they should be logically
     * equivalent in that case.
     *
     * @throws InvalidCharacterException
     */
    @Test
    public void testClientToServerBMPLow() throws InvalidCharacterException {
        clientToServerVerifyRange(Character.MIN_CODE_POINT, Character.MIN_SURROGATE,
                CHARACTER_BLOCK_SIZE, CHARACTER_BLOCK_SIZE);
    }

    /**
     * Generate strings containing ranges of characters and sends them to the
     * server for verification. This ensures that client->server string escaping
     * properly handles all non-BMP characters.
     * <p>
     * Note that this does not test all possible combinations, which might be an
     * issue, particularly with combining marks, though they should be logically
     * equivalent in that case.
     *
     * @throws InvalidCharacterException
     */
    @Test
    public void testClientToServerNonBMP() throws InvalidCharacterException {
        clientToServerVerifyRange(Character.MIN_SUPPLEMENTARY_CODE_POINT, Character.MAX_CODE_POINT + 1,
                CHARACTER_BLOCK_SIZE, NON_BMP_TEST_INCREMENT);
    }

    /**
     * Requests strings of CHARACTER_RANGE_SIZE from the server and validates that
     * the returned string length matches CHARACTER_RANGE_SIZE and that all of the
     * characters remain intact.
     * <p>
     * Note that this does not test all possible combinations, which might be an
     * issue, particularly with combining marks, though they should be logically
     * equivalent in that case. Surrogate characters are also not tested here,
     * see {@link #disabled_testServerToClientBMPSurrogates()}.
     */
    @Test
    public void testServerToClientBMP() {
        serverToClientVerify(Character.MIN_CODE_POINT, Character.MIN_SURROGATE,
                CHARACTER_BLOCK_SIZE, CHARACTER_BLOCK_SIZE);
    }

    /**
     * Requests strings of CHARACTER_RANGE_SIZE from the server and validates that
     * the returned string length matches CHARACTER_RANGE_SIZE and that all of the
     * characters remain intact.  This test checks the range of surrogate
     * characters, which are used to encode non-BMP characters as pairs of UTF16
     * characters.
     * <p>
     * Note that this does not test all possible combinations.
     */
    @Test
    @DoNotRunWith(Platform.HtmlUnitBug)
    // TODO(jat): decide if we really want to specify this behavior since some
    // browsers and OOPHM plugins have issues with it -- disabled for now
    public void disabled_testServerToClientBMPSurrogates() {
        serverToClientVerify(Character.MIN_SURROGATE, Character.MIN_SUPPLEMENTARY_CODE_POINT,
                CHARACTER_BLOCK_SIZE, CHARACTER_BLOCK_SIZE);
    }

    /**
     * Requests strings of CHARACTER_RANGE_SIZE from the server and validates that
     * the returned string length matches CHARACTER_RANGE_SIZE and that all of the
     * characters remain intact. Note that this test verifies non-BMP characters
     * (ie, those which are represented as pairs of surrogates).
     * <p>
     * Note that this does not test all possible combinations, which might be an
     * issue, particularly with combining marks, though they should be logically
     * equivalent in that case.
     */
    @Test
    public void testServerToClientNonBMP() {
        serverToClientVerify(Character.MIN_SUPPLEMENTARY_CODE_POINT, Character.MAX_CODE_POINT + 1,
                CHARACTER_BLOCK_SIZE, NON_BMP_TEST_INCREMENT);
    }


    protected void clientToServerVerifyRange(final int start, final int end, final int size, final int step) throws InvalidCharacterException {
        current = start;
        int blockEnd = Math.min(end, current + size);
        service.verifyStringContainingCharacterRange(current, blockEnd, getStringContainingCharacterRange(start, blockEnd), waitedCallback(new AsyncCallback<>() {
            final List<Throwable> fails = new ArrayList<>();

            @Override
            public void onFailure(Throwable caught) {
                fails.add(caught);
                onSuccess(false);
            }

            @Override
            public void onSuccess(Boolean ignored) {
                current += step;
                if (current < end) {
                    int blockEnd = Math.min(end, current + size);
                    try {
                        service.verifyStringContainingCharacterRange(current,
                                blockEnd, getStringContainingCharacterRange(current, blockEnd), this);
                    } catch (InvalidCharacterException e) {
                        fails.add(e);
                    }
                } else if (!fails.isEmpty()) {
                    StringBuilder msg = new StringBuilder();
                    for (Throwable t : fails) {
                        msg.append(t.getMessage()).append("\n");
                    }
                    throw new RuntimeException(msg.toString());
                }
            }
        }));
    }

    protected void serverToClientVerify(final int start, final int end, final int size, final int step) {
        current = start;
        service.getStringContainingCharacterRange(start, Math.min(end, current + size), waitedCallback(new AsyncCallback<>() {
            final List<Throwable> fails = new ArrayList<>();

            @Override
            public void onFailure(Throwable caught) {
                fails.add(caught);
                nextBatch();
            }

            @Override
            public void onSuccess(String str) {
                try {
                    verifyStringContainingCharacterRange(current, Math.min(end, current + size), str);
                } catch (InvalidCharacterException e) {
                    fails.add(e);
                }
                nextBatch();
            }

            private void nextBatch() {
                current += step;
                if (current < end) {
                    service.getStringContainingCharacterRange(current, Math.min(end, current + size), this);
                } else if (!fails.isEmpty()) {
                    StringBuilder msg = new StringBuilder();
                    for (Throwable t : fails) {
                        msg.append(t.getMessage()).append("\n");
                    }
                    throw new RuntimeException(msg.toString());
                }
            }
        }));
    }


    /**
     * Test that a NUL character followed by an octal character is encoded
     * correctly.  Encoding the NUL character simply as "\0" in this case
     * would cause the recipient to see "\07" as a single octal escape sequence,
     * rather than two separate characters.
     */
    @Test
    public void testEscapeNull() {
        echoVerify("\u0000" + "7"); // split to emphasize two characters
    }

    /**
     * Test that HTML special characters are encoded correctly.
     */
    @Test
    public void testEscapeHtml() {
        echoVerify("<img src=x onerror=alert(1)>");
    }

    /**
     * Verify that string encoding/decoding is lossless.
     */
    private void echoVerify(final String str) {
        service.echo(str, waitedCallback(result -> {
            assertEquals(str, result);
        }));
    }


    /**
     * Generates a string containing a sequence of code points.
     *
     * @param start first code point to include in the string
     * @param end   one past the last code point to include in the string
     * @return a string containing all the requested code points
     */
    public static String getStringContainingCharacterRange(int start, int end) {
        StringBuilder buf = new StringBuilder();
        for (int codePoint = start; codePoint < end; ++codePoint) {
            if (Character.isSupplementaryCodePoint(codePoint)) {
                buf.append(Character.toChars(codePoint));
            } else {
                buf.append((char) codePoint);
            }
        }

        return buf.toString();
    }

    /**
     * Verifies that the supplied string includes the requested code points.
     *
     * @param start first code point to include in the string
     * @param end   one past the last code point to include in the string
     * @param str   the string to test
     * @throws InvalidCharacterException if a character doesn't match
     * @throws RuntimeException          if the string is too long
     */
    public static void verifyStringContainingCharacterRange(int start, int end, String str) throws InvalidCharacterException {
        if (str == null) {
            throw new NullPointerException("String is null");
        }
        int expectedLen = end - start;
        int strLen = str.codePointCount(0, str.length());
        for (int i = 0, codePoint = start; i < strLen; i = Character.offsetByCodePoints(str, i, 1)) {
            int strCodePoint = str.codePointAt(i);
            if (strCodePoint != codePoint) {
                throw new InvalidCharacterException(i, codePoint, strCodePoint);
            }
            ++codePoint;
        }
        if (strLen < expectedLen) {
            throw new InvalidCharacterException(strLen, start + strLen, -1);
        } else if (expectedLen != strLen) {
            throw new RuntimeException(
                    "Too many characters returned on block from U+"
                            + Integer.toHexString(start) + " to U+"
                            + Integer.toHexString(end) + ": expected=" + expectedLen
                            + ", actual=" + strLen);
        }
    }
}
