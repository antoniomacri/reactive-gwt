/**
 * Dec 30, 2014 Copyright Blue Esoteric Web Development, LLC
 * Contact: P.Prith@BlueEsoteric.com
 */
package com.github.antoniomacri.reactivegwt.proxy;

import com.github.antoniomacri.reactivegwt.proxy.exception.SyncProxyException;
import com.github.antoniomacri.reactivegwt.proxy.exception.SyncProxyException.InfoType;
import com.github.antoniomacri.reactivegwt.proxy.test.*;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests the static verification methods within the main SyncProxy class. All
 * tests included here should not require a remote service to actually read from
 *
 * @author Preethum
 * @since 0.5
 */
public class ReactiveGWTTest {
    public interface InnerMissingTestServiceAsync {
    }

    public interface InnerNoAnnotTestService extends RemoteService {

    }

    public interface InnerNoAnnotTestServiceAsync {

    }

    @RemoteServiceRelativePath("innertest")
    public interface InnerTestService extends RemoteService {

    }

    public interface InnerTestServiceAsync {

    }

    @RemoteServiceRelativePath("/innertest")
    public interface InnerTestServiceStartingWithSlash extends RemoteService {

    }

    @BeforeEach
    void setUp() {
        // Reset SyncProxy
        ReactiveGWT.suppressRelativePathWarning(false);
    }

    @Test
    public void testClassGetRemoteServiceRelativePathFromAnnotation() {
        assertThat(ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(TestService.class))
                .as("Wrong path from annotation")
                .isEqualTo("test");

        assertThat(ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(TestServiceAsync.class))
                .as("Wrong path from Async annotation")
                .isEqualTo("test");

        assertThatThrownBy(() -> ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(NoAnnotTestService.class))
                .isInstanceOf(SyncProxyException.class)
                .satisfies(e -> ((SyncProxyException) e).verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH));

        assertThatThrownBy(() -> ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(NoAnnotTestServiceAsync.class))
                .isInstanceOf(SyncProxyException.class)
                .satisfies(e -> ((SyncProxyException) e).verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH));

        assertThatThrownBy(() -> ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(MissingTestServiceAsync.class))
                .isInstanceOf(SyncProxyException.class)
                .satisfies(e -> ((SyncProxyException) e).verify(InfoType.SERVICE_BASE));
    }

    @Test
    public void shouldThrowExceptionIfBaseUrlIsMissing() {
        RpcPolicyFinder policyFinder = mock(RpcPolicyFinder.class);
        ProxySettings settings = new ProxySettings(null);

        assertThatThrownBy(() -> ReactiveGWT.prepareSettings(InnerTestService.class, settings, policyFinder))
                .isInstanceOf(SyncProxyException.class)
                .satisfies(e -> ((SyncProxyException) e).verify(InfoType.MODULE_BASE_URL));
    }

    @Test
    public void shouldOverrideModuleBaseUrlViaSetter() {
        ProxySettings settings = new ProxySettings(null);

        String testUrl2 = "testUrl2";
        settings.setModuleBaseUrl(testUrl2);
        assertThat(settings.getModuleBaseUrl()).isEqualTo(testUrl2);
    }

    @Test
    public void shouldThrowExceptionIfRemoteServiceAnnotationIsMissing() {
        String testUrl2 = "testUrl2";
        RpcPolicyFinder policyFinder = mock(RpcPolicyFinder.class);
        ProxySettings settings = new ProxySettings(testUrl2);

        assertThatThrownBy(() -> ReactiveGWT.prepareSettings(InnerNoAnnotTestService.class, settings, policyFinder))
                .isInstanceOf(SyncProxyException.class)
                .satisfies(e -> ((SyncProxyException) e).verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH));
    }

    @Test
    public void shouldAllowSettingRemoteServicePathViaSetter() throws IOException {
        String testUrl2 = "testUrl2";
        RpcPolicyFinder policyFinder = mock(RpcPolicyFinder.class);
        ProxySettings settings = new ProxySettings(testUrl2);

        doReturn("INATSPolicy").when(policyFinder).fetchSerializationPolicyName(InnerNoAnnotTestService.class, testUrl2);

        String testRelativePath = "relativePath";
        settings.setRemoteServiceRelativePath(testRelativePath);
        ReactiveGWT.prepareSettings(InnerNoAnnotTestService.class, settings, policyFinder);
        assertThat(settings.getRemoteServiceRelativePath()).isEqualTo(testRelativePath);
    }

    @Test
    public void settingRemoteServicePathViaSetterShouldOverrideAnnotation() throws IOException {
        String testUrl2 = "testUrl2";
        RpcPolicyFinder policyFinder = mock(RpcPolicyFinder.class);
        ProxySettings settings = new ProxySettings(testUrl2);

        doReturn("INATSPolicy").when(policyFinder).fetchSerializationPolicyName(InnerTestService.class, testUrl2);

        String testRelativePath = "relativePath";
        settings.setRemoteServiceRelativePath(testRelativePath);
        ReactiveGWT.prepareSettings(InnerTestService.class, settings, policyFinder);
        assertThat(settings.getRemoteServiceRelativePath()).isEqualTo(testRelativePath);
    }

    @Test
    public void cookieManagerShouldBeDefaultIfNotProvided() throws IOException {
        String testUrl2 = "testUrl2";
        RpcPolicyFinder policyFinder = mock(RpcPolicyFinder.class);
        ProxySettings settings = new ProxySettings(testUrl2);
        doReturn("INATSPolicy").when(policyFinder).fetchSerializationPolicyName(InnerTestService.class, testUrl2);

        ReactiveGWT.prepareSettings(InnerTestService.class, settings, policyFinder);

        // Cookie manager should always be available
        assertThat(settings.getCookieManager()).isNotNull();
    }

    @Test
    public void shouldOverrideCookieManagerViaSetter() throws IOException {
        String testUrl2 = "testUrl2";
        RpcPolicyFinder policyFinder = mock(RpcPolicyFinder.class);
        ProxySettings settings = new ProxySettings(testUrl2);
        doReturn("INATSPolicy").when(policyFinder).fetchSerializationPolicyName(InnerTestService.class, testUrl2);

        CookieManager cm = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        settings.setCookieManager(cm);

        ReactiveGWT.prepareSettings(InnerTestService.class, settings, policyFinder);
        assertThat(settings.getCookieManager()).isSameAs(cm);
    }

    @Test
    public void shouldThrowExceptionIfPolicyNameIsMissing() {
        String testUrl2 = "testUrl2";
        RpcPolicyFinder policyFinder = mock(RpcPolicyFinder.class);
        ProxySettings settings = new ProxySettings(testUrl2);

        assertThatThrownBy(() -> ReactiveGWT.prepareSettings(InnerTestService.class, settings, policyFinder))
                .isInstanceOf(SyncProxyException.class)
                .satisfies(e -> ((SyncProxyException) e).verify(InfoType.POLICY_NAME_MISSING));
    }

    @Test
    public void testInnerClassGetRemoteServiceRelativePathFromAnnotation() {
        assertThat(ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(InnerTestService.class))
                .as("Wrong path from annotation").isEqualTo("innertest");

        assertThat(ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(InnerTestServiceAsync.class))
                .as("Wrong path from Async annotation").isEqualTo("innertest");

        assertThatThrownBy(() -> ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(InnerNoAnnotTestService.class))
                .isInstanceOf(SyncProxyException.class)
                .satisfies(e -> ((SyncProxyException) e).verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH));

        assertThatThrownBy(() -> ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(InnerNoAnnotTestServiceAsync.class))
                .isInstanceOf(SyncProxyException.class)
                .satisfies(e -> ((SyncProxyException) e).verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH));

        assertThatThrownBy(() -> ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(InnerMissingTestServiceAsync.class))
                .isInstanceOf(SyncProxyException.class)
                .satisfies(e -> ((SyncProxyException) e).verify(InfoType.SERVICE_BASE));
    }

    @Test
    public void testRemoveLeadingSlashFromRemoteServiceRelativePathAnnotation() {
        assertThat(ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(InnerTestServiceStartingWithSlash.class))
                .as("Wrong path from annotation").isEqualTo("innertest");
    }
}
