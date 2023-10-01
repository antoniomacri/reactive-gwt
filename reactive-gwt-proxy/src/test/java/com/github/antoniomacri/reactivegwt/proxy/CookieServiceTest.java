/**
 * Jan 10, 2015 Copyright Blue Esoteric Web Development, LLC
 * Contact: P.Prith@BlueEsoteric.com
 */
package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RpcAsyncTestBase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Preethum
 * @since 0.5
 * <p>
 * Modified by Antonio Macrì to perform tests against an embedded Jetty with the GWT servlet.
 */
public class CookieServiceTest extends RpcAsyncTestBase<CookieService, CookieServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(CookieService.class, CookieServiceImpl.class, SessionsTracker.class);
    }


    public CookieServiceTest() {
        super(CookieService.class);
    }

    @Override
    protected CookieServiceAsync getService() {
        CookieManager cm = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieServiceAsync cookieService = SyncProxy.createProxy(CookieServiceAsync.class,
                new ProxySettings().setCookieManager(cm));
        return cookieService;
    }


    @BeforeEach
    public final void setUpTimeout() {
        // setRpcTimeout(2);
    }

    @AfterEach
    public final void tearDown() {
        waitForServiceCall((AsyncCallback<Void> callback) -> service.invalidateAllSessions(callback));
    }


    @Test
    public void testEchoCookiesFromClient() {
        // Make sure Cookie Store is empty to begin with
        assertThat((Object) ((HasProxySettings) service).getCookieManager()
                .getCookieStore().getCookies().size()).as("Cookie Store should be empty").isEqualTo(0);
        for (String val : CookieService.COOKIE_VALS) {
            String domain = URI.create(getModuleBaseURL()).getHost();
            HttpCookie cookie = new HttpCookie(val, val);
            ((HasProxySettings) service).getCookieManager()
                    .getCookieStore()
                    .add(URI.create("http://" + domain), cookie);
        }
        service.echoCookiesFromClient(createCallback(result -> {
            assertThat((Object) result.size()).as("Missing Cookies").isEqualTo(CookieService.COOKIE_VALS.length);
            for (String val : CookieService.COOKIE_VALS) {
                assertThat(searchCookieStoreForValue(val,
                        ((HasProxySettings) service)
                                .getCookieManager()
                                .getCookieStore())).as("Missing Cookie Value in Store").isTrue();
            }
            for (String val : CookieService.COOKIE_VALS) {
                assertThat(result.contains(val)).as("Missing Cookie Value Server Response").isTrue();
            }
        }));
    }

    @Test
    public void testEchoCookiesFromClientForDifferentURL() {
        // Make sure Cookie Store is empty to begin with
        assertThat((Object) ((HasProxySettings) service).getCookieManager()
                .getCookieStore().getCookies().size()).as("Cookie Store should be empty").isEqualTo(0);
        for (String val : CookieService.COOKIE_VALS) {
            ((HasProxySettings) service)
                    .getCookieManager()
                    .getCookieStore()
                    .add(URI.create("http://another.url"), new HttpCookie(val, val));
        }
        service.echoCookiesFromClient(createCallback(result ->
                assertThat((Object) result.size()).as("Should be no cookies").isEqualTo(0)));
    }

    @Test
    public void testGenerateCookiesOnServer() {
        // Make sure Cookie Store is empty to begin with
        assertThat((Object) ((HasProxySettings) service).getCookieManager()
                .getCookieStore().getCookies().size()).as("Cookie Store should be empty").isEqualTo(0);
        service.generateCookiesOnServer(createCallback(result -> {
            assertThat((Object) ((HasProxySettings) service).getCookieManager()
                    .getCookieStore().getCookies().size()).as("Missing Cookies").isEqualTo(CookieService.COOKIE_VALS.length);
            for (String val : CookieService.COOKIE_VALS) {
                assertThat(searchCookieStoreForValue(val,
                        ((HasProxySettings) service)
                                .getCookieManager()
                                .getCookieStore())).as("Missing Cookie Value").isTrue();
            }
        }));
    }

    @Test
    public void testMultipleSessions() {
        final CookieServiceAsync service2 = getService();

        final String test = "TEST";
        final String test2 = "TEST2";

        waitForServiceCall((AsyncCallback<Void> callback) -> service.setSessionAttrib(test, callback));
        assertThat(getFromServiceCall(service::getSessionAttrib))
                .as("Wrong session 1 attribute")
                .isEqualTo(test);

        waitForServiceCall((AsyncCallback<Void> callback) -> service2.setSessionAttrib(test2, callback));
        assertThat(getFromServiceCall(service::getSessionAttrib))
                .as("Wrong session 1 attribute")
                .isEqualTo(test);
        assertThat(getFromServiceCall(service2::getSessionAttrib))
                .as("Wrong session 2 attribute")
                .isEqualTo(test2);
    }

    @Test
    public void testOnTheFlyCookieManagerChange() {
        // Make sure Cookie Store is empty to begin with
        assertThat((Object) ((HasProxySettings) service).getCookieManager()
                .getCookieStore().getCookies().size()).as("Cookie Store should be empty").isEqualTo(0);
        for (String val : CookieService.COOKIE_VALS) {
            String domain = URI.create(getModuleBaseURL()).getHost();
            HttpCookie cookie = new HttpCookie(val, val);
            ((HasProxySettings) service).getCookieManager()
                    .getCookieStore()
                    .add(URI.create("http://" + domain), cookie);
        }
        ArrayList<String> result = getFromServiceCall(callback -> service.echoCookiesFromClient(callback));
        assertThat((Object) result.size()).as("Missing Cookies").isEqualTo(CookieService.COOKIE_VALS.length);
        for (String val : CookieService.COOKIE_VALS) {
            assertThat(searchCookieStoreForValue(val,
                    ((HasProxySettings) service).getCookieManager()
                            .getCookieStore())).as("Missing Cookie Value in Store").isTrue();
        }
        for (String val : CookieService.COOKIE_VALS) {
            assertThat(result.contains(val)).as("Missing Cookie Value Server Response").isTrue();
        }
        // Change cookiemanager with new cookies and check again
        String postFix = "-T2";
        CookieManager cm2 = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        for (String val : CookieService.COOKIE_VALS) {
            String domain = URI.create(getModuleBaseURL()).getHost();
            HttpCookie cookie = new HttpCookie(val + postFix, val + postFix);
            cm2.getCookieStore().add(URI.create("http://" + domain), cookie);
        }
        ((HasProxySettings) service).setCookieManager(cm2);
        result = getFromServiceCall(callback -> service.echoCookiesFromClient(callback));
        assertThat((Object) result.size()).as("Missing Cookies").isEqualTo(CookieService.COOKIE_VALS.length);
        for (String val : CookieService.COOKIE_VALS) {
            assertThat(searchCookieStoreForValue(val + postFix,
                    ((HasProxySettings) service).getCookieManager()
                            .getCookieStore())).as("Missing Cookie Value in Store").isTrue();
        }
        for (String val : CookieService.COOKIE_VALS) {
            assertThat(result.contains(val + postFix)).as("Missing Cookie Value Server Response").isTrue();
        }
    }

    @Test
    public void testSessionGood() {
        final String test = "TEST";
        service.setSessionAttrib(test, createCallback(session ->
                service.getSessionAttrib(createCallback(result ->
                        assertThat((Object) result).as("Wrong session attribute value").isEqualTo(test)))));
    }

    @Test
    public void testVerifyServerGeneratedIsEchoed() {
        // Make sure Cookie Store is empty to begin with
        assertThat((Object) ((HasProxySettings) service).getCookieManager()
                .getCookieStore().getCookies().size()).as("Cookie Store should be empty").isEqualTo(0);
        service.generateCookiesOnServer(createCallback(ignored -> {
            // Make sure Cookie Store is empty to begin with
            assertThat((Object) ((HasProxySettings) service).getCookieManager()
                    .getCookieStore().getCookies().size()).as("Cookie Store should not be empty").isEqualTo(CookieService.COOKIE_VALS.length);
            service.echoCookiesFromClient(createCallback(result2 -> {
                assertThat((Object) result2.size()).as("Missing Cookies").isEqualTo(CookieService.COOKIE_VALS.length);
                for (String val : CookieService.COOKIE_VALS) {
                    assertThat(searchCookieStoreForValue(
                            val,
                            ((HasProxySettings) service)
                                    .getCookieManager()
                                    .getCookieStore())).as("Missing Cookie Value in Store").isTrue();
                }
                for (String val : CookieService.COOKIE_VALS) {
                    assertThat(result2.contains(val)).as("Missing Cookie Value Server Response").isTrue();
                }
            }));
        }));
    }

    @Test
    public void testVerifyServerGeneratedIsNotEchoed() {
        // Make sure Cookie Store is empty to begin with
        assertThat((Object) ((HasProxySettings) service).getCookieManager()
                .getCookieStore().getCookies().size()).as("Cookie Store should be empty").isEqualTo(0);
        service.generateCookiesOnServer(createCallback(ignored -> {
            CookieServiceAsync serviceAsync2 = getService();
            // Make sure Cookie Store is empty to begin with
            assertThat((Object) ((HasProxySettings) serviceAsync2).getCookieManager()
                    .getCookieStore().getCookies().size()).as("Cookie Store should be empty").isEqualTo(0);
            serviceAsync2.echoCookiesFromClient(createCallback(result ->
                    assertThat((Object) result.size()).as("Should have no Cookies").isEqualTo(0)
            ));
        }));
    }


    private boolean searchCookieStoreForValue(String value, CookieStore store) {
        for (HttpCookie c : store.getCookies()) {
            if (c.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
