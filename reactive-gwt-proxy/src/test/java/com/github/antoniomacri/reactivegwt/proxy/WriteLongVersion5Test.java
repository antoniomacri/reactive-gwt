package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.impl.AbstractSerializationStreamWriter;
import com.google.gwt.user.server.rpc.impl.StandardSerializationPolicy;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Verifies that {@link SyncClientSerializationStreamWriter#writeLong(long)} produces
 * the correct double-pair encoding when using serialization stream version 5.
 * <p>
 * In version 5, a {@code long} is split into two 32-bit halves and written as two doubles
 * via {@link AbstractSerializationStreamWriter#makeLongComponents(int, int)}.
 * The reference implementation is {@link AbstractSerializationStreamWriter#getAsDoubleArray(long)},
 * which calls {@code makeLongComponents(lowBits, highBits)}.
 * <p>
 * This test was added to catch a bug where the arguments to {@code makeLongComponents} were swapped,
 * causing Date values (and any long) to be corrupted when sent from the proxy to a GWT 2.0.3 server.
 *
 * @see <a href="https://github.com/nickhudak/reactive-gwt/issues/XXX">GitHub issue</a>
 */
public class WriteLongVersion5Test {

    private static final int VERSION_5 = 5;

    /**
     * Creates a minimal writer configured for stream version 5.
     */
    private SyncClientSerializationStreamWriter createWriter() {
        StandardSerializationPolicy policy = new StandardSerializationPolicy(Map.of(), Map.of(), Map.of());
        return new SyncClientSerializationStreamWriter("http://test/", "test-policy", policy, null, VERSION_5);
    }

    /**
     * Extracts the serialized payload after writing, splitting by the RPC separator '|'.
     */
    private String[] getTokens(SyncClientSerializationStreamWriter writer) {
        // toString() produces: version|flags|stringTableSize|...payload...
        // We call prepareToWrite() which writes moduleBaseURL and policyName to the string table,
        // then writeLong() appends two doubles to the payload.
        return writer.toString().split("\\|");
    }

    @Test
    public void writeLong_version5_shouldMatchGwtReference() {
        // A typical Date epoch millis value that exercises both high and low 32-bit halves
        long dateMillis = 1775854800000L; // ~ 2026-04-08

        // Get the reference doubles from GWT's own implementation
        double[] reference = AbstractSerializationStreamWriter.getAsDoubleArray(dateMillis);

        // Write the same value using the proxy writer
        SyncClientSerializationStreamWriter writer = createWriter();
        writer.prepareToWrite();
        writer.writeLong(dateMillis);

        String[] tokens = getTokens(writer);
        // The last two payload tokens (before any trailing empty string) are the two doubles
        // written by writeLong. Payload is appended after the string table.
        // Format: version|flags|stringTableSize|str0|str1|double0|double1|
        int len = tokens.length;
        // Find the two doubles at the end of the token array
        double written0 = Double.parseDouble(tokens[len - 2]);
        double written1 = Double.parseDouble(tokens[len - 1]);

        assertThat(written0).as("First double (low bits as unsigned)")
                .isEqualTo(reference[0]);
        assertThat(written1).as("Second double (high bits * 2^32)")
                .isEqualTo(reference[1]);
    }

    @Test
    public void writeLong_version5_shouldRoundTripCorrectly() {
        long[] testValues = {
                0L,
                1L,
                -1L,
                Long.MAX_VALUE,
                Long.MIN_VALUE,
                System.currentTimeMillis(),
                1775854800000L,        // 2026-04-08 (a typical date)
                new Date().getTime(),  // current date
                4294967296L,           // exactly 2^32 (low bits = 0, high bits = 1)
                4294967295L,           // 2^32 - 1 (low bits = all ones, high bits = 0)
        };

        for (long value : testValues) {
            double[] reference = AbstractSerializationStreamWriter.getAsDoubleArray(value);

            SyncClientSerializationStreamWriter writer = createWriter();
            writer.prepareToWrite();
            writer.writeLong(value);

            String[] tokens = getTokens(writer);
            int len = tokens.length;
            double written0 = Double.parseDouble(tokens[len - 2]);
            double written1 = Double.parseDouble(tokens[len - 1]);

            // Verify the doubles match GWT's reference implementation
            assertThat(written0).as("First double for value %d", value)
                    .isEqualTo(reference[0]);
            assertThat(written1).as("Second double for value %d", value)
                    .isEqualTo(reference[1]);

            // Also verify round-trip: the GWT server reader does (long)d0 + (long)d1
            long reconstructed = (long) written0 + (long) written1;
            assertThat(reconstructed).as("Round-trip for value %d", value)
                    .isEqualTo(value);
        }
    }
}
