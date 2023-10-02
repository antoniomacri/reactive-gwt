/**
 * Dec 31, 2014 Copyright Blue Esoteric Web Development, LLC
 * Contact: P.Prith@BlueEsoteric.com
 */
package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.RpcSyncTestBase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the LargePayload Service in a synchronized format
 *
 * @author Preethum
 * @since 0.5
 * <p>
 * Modified by Antonio Macrì to perform tests against an embedded Jetty with the GWT servlet.
 */
@ExtendWith(ArquillianExtension.class)
public class LargePayloadSyncTest extends RpcSyncTestBase<LargePayloadService> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(LargePayloadService.class, LargePayloadServiceImpl.class);
    }


    protected LargePayloadSyncTest() {
        super(LargePayloadService.class);
    }


    @Test
    public void testLargeResponseArray() {
        int[] result = service.testLargeResponseArray();
        assertThat(result.length).as("Wrong array size").isEqualTo(LargePayloadService.ARRAY_SIZE);
    }

    @Test
    public void testLargeResponsePayload() {
        List<UserInfo> result = service.testLargeResponsePayload();
        assertThat(result.size()).as("Wrong list size").isEqualTo(LargePayloadService.PAYLOAD_SIZE);
    }
}
