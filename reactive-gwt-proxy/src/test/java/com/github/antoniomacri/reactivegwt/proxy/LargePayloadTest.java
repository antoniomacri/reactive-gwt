package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.RpcTestBase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * POJ Test of the LargePayload Service.
 *
 * @author Preethum
 * @since 0.5
 * <p>
 * Modified by Antonio Macrì to perform tests against an embedded Jetty with the GWT servlet.
 */
public class LargePayloadTest extends RpcTestBase<LargePayloadService, LargePayloadServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(LargePayloadService.class, LargePayloadServiceImpl.class);
    }


    public LargePayloadTest() {
        super(LargePayloadService.class);
    }


    @Test
    public void testLargeResponseArray() {
        service.testLargeResponseArray(createCallback(result ->
                assertThat(result.length).as("Wrong array size").isEqualTo(LargePayloadService.ARRAY_SIZE)));
    }

    @Test
    public void testLargeResponsePayload() {
        service.testLargeResponsePayload(createCallback(result ->
                assertThat(result.size()).as("Wrong list size").isEqualTo(LargePayloadService.PAYLOAD_SIZE)));
    }
}
