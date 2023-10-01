/**
 * Dec 31, 2014 Copyright Blue Esoteric Web Development, LLC
 * Contact: P.Prith@BlueEsoteric.com
 */
package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.RpcTestBase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * POJ Test of the Greeting Service.
 *
 * @author Preethum
 * @since 0.5
 * <p>
 * Modified by Antonio Macrì to perform tests against an embedded Jetty with the GWT servlet.
 */
public class GreetingServiceTest extends RpcTestBase<GreetingService, GreetingServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(GreetingService.class, GreetingServiceImpl.class);
    }


    public GreetingServiceTest() {
        super(GreetingService.class);
    }


    /**
     * This test will send a request to the server using the greetServer method
     * in GreetingService and verify the response.
     */
    @Test
    public void testGreetingService() {
        service.greetServer(GreetingService.NAME, createCallback(result ->
                assertTrue(result.contains(GreetingService.NAME))));
    }

    @Test
    public void testGreetingService2() {
        service.greetServer2(GreetingService.NAME, createCallback(result ->
                assertEquals(GreetingService.NAME, result.getText())));
    }

    @Test
    public void testGreetingServiceArray() {
        service.greetServerArr(GreetingService.NAME, createCallback(result ->
                assertEquals(GreetingService.NAME, result.get(0))));
    }
}
