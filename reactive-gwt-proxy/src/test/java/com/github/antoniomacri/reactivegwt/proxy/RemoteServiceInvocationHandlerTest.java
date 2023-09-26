/**
 * Dec 30, 2014 Copyright Blue Esoteric Web Development, LLC
 * Contact: P.Prith@BlueEsoteric.com
 */
package com.github.antoniomacri.reactivegwt.proxy;

import com.github.antoniomacri.reactivegwt.proxy.test.MissingTestServiceAsync;
import com.github.antoniomacri.reactivegwt.proxy.test.TestService;
import com.github.antoniomacri.reactivegwt.proxy.test.TestServiceAsync;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 * @author Preethum
 * @since 0.5
 */
public class RemoteServiceInvocationHandlerTest {

    private class TestMissingProxyAsync implements MissingTestServiceAsync {
    }

    private class TestProxy implements TestService {
    }

    private class TestProxyAsync implements TestServiceAsync {
    }

    @Test
    public void testDetermineProxyServiceBaseInterface() throws ClassNotFoundException {
        assertThat(RemoteServiceInvocationHandler.determineProxyServiceBaseInterface(new TestProxy()))
                .as("Wrong service class").isEqualTo(TestService.class);
        assertThat(RemoteServiceInvocationHandler.determineProxyServiceBaseInterface(new TestProxyAsync()))
                .as("Wrong service class for async").isEqualTo(TestService.class);
        try {
            RemoteServiceInvocationHandler.determineProxyServiceBaseInterface(new TestMissingProxyAsync());
            fail("Should have thrown exception unfound class");
        } catch (ClassNotFoundException cnfe) {
            // ignored
        }
    }
}
