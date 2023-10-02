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

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Greeting Service in a synchronized format
 *
 * @author Preethum
 * @since 0.5
 * <p>
 * Modified by Antonio Macrì to perform tests against an embedded Jetty with the GWT servlet.
 */
@ExtendWith(ArquillianExtension.class)
public class GreetingServiceSyncTest extends RpcSyncTestBase<GreetingService> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(GreetingService.class, GreetingServiceImpl.class);
    }


    public GreetingServiceSyncTest() {
        super(GreetingService.class);
    }


    @Test
    public void testGreetingService() {
        String result = service.greetServer(GreetingService.NAME);
        assertTrue(result.contains(GreetingService.NAME));
    }

    @Test
    public void testGreetingService2() {
        T1 result = service.greetServer2(GreetingService.NAME);
        assertEquals(GreetingService.NAME, result.getText());
    }

    @Test
    public void testGreetingServiceArray() {
        ArrayList<String> result = service.greetServerArr(GreetingService.NAME);
        assertEquals(GreetingService.NAME, result.get(0));
    }
}
