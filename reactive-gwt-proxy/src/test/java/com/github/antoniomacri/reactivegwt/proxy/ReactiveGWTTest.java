/**
 * Dec 30, 2014 Copyright Blue Esoteric Web Development, LLC
 * Contact: P.Prith@BlueEsoteric.com
 */
package com.github.antoniomacri.reactivegwt.proxy;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;

import com.github.antoniomacri.reactivegwt.proxy.exception.SyncProxyException;
import com.github.antoniomacri.reactivegwt.proxy.exception.SyncProxyException.InfoType;
import com.github.antoniomacri.reactivegwt.proxy.test.MissingTestServiceAsync;
import com.github.antoniomacri.reactivegwt.proxy.test.NoAnnotTestService;
import com.github.antoniomacri.reactivegwt.proxy.test.NoAnnotTestServiceAsync;
import com.github.antoniomacri.reactivegwt.proxy.test.TestService;
import com.github.antoniomacri.reactivegwt.proxy.test.TestServiceAsync;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.ArgumentMatchers.any;
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
        try {
            ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(NoAnnotTestService.class);
            fail("Should have thrown an exception");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH);
        }
        try {
            ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(NoAnnotTestServiceAsync.class);
            fail("Should have thrown an exception");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH);
        }
        try {
            ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(MissingTestServiceAsync.class);
            fail("Should have thrown an exception");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.SERVICE_BASE);
        }
    }

    @Test
    public void testPrepareSettings() throws IOException {
        RpcPolicyFinder policyFinder = mock(RpcPolicyFinder.class);

        ProxySettings settings = new ProxySettings(null);
        try {
            ReactiveGWT.prepareSettings(InnerTestService.class, settings, policyFinder);
            fail("Should have failed on lack of a server base url available");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.MODULE_BASE_URL);
        }

        // Setup in-place policy's for services test below
        String testUrl = "testUrl";
        String testUrl2 = "testUrl2";
        String itsPolicy = "ITSPolicy";
        String inatsPolicy = "INATSPolicy";
        doReturn(itsPolicy).when(policyFinder).fetchSerializationPolicyName(InnerTestService.class, testUrl);
        doReturn(inatsPolicy).when(policyFinder).fetchSerializationPolicyName(InnerNoAnnotTestService.class, testUrl2);

        // Test Override of moduleBaseUrl
        settings.setModuleBaseUrl(testUrl2);
        assertThat(settings.getModuleBaseUrl()).isEqualTo(testUrl2);

        // Test relative path assignment
        settings = new ProxySettings(testUrl2);
        try {
            ReactiveGWT.prepareSettings(InnerNoAnnotTestService.class, settings, policyFinder);
            fail("Should have failed on lack of available annotation");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH);
        }

        String testRelativePath = "relativePath";
        settings.setRemoteServiceRelativePath(testRelativePath);
        ReactiveGWT.prepareSettings(InnerNoAnnotTestService.class, settings, policyFinder);
        assertThat(settings.getRemoteServiceRelativePath())
                .as("Failed to utilized manual relative path").isEqualTo(testRelativePath);
        ReactiveGWT.prepareSettings(InnerTestService.class, settings, policyFinder);
        assertThat(settings.getRemoteServiceRelativePath())
                .as("Failed to utilized manual relative path with annotation")
                .isEqualTo(testRelativePath);
        // Cookie manager should always be available
        assertThat(settings.getCookieManager())
                .as("Cookie manager should be default if not provided").isNotNull();
        CookieManager cm = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        settings.setCookieManager(cm);
        ReactiveGWT.prepareSettings(InnerTestService.class, settings, policyFinder);
        assertThat(settings.getCookieManager()).as("Wrong Cookie Manager").isEqualTo(cm);

        // Test policy names for provided test classes , then remove to check
        // for exception when missing
        settings = new ProxySettings(testUrl);
        ReactiveGWT.prepareSettings(InnerTestService.class, settings, policyFinder);
        assertThat(settings.getPolicyName())
                .as("Wrong policy for InnerTestService").isEqualTo(itsPolicy);

        settings = new ProxySettings(testUrl2);
        settings.setRemoteServiceRelativePath(testRelativePath);
        ReactiveGWT.prepareSettings(InnerNoAnnotTestService.class, settings, policyFinder);
        assertThat(settings.getPolicyName())
                .as("Wrong policy for InnerNoAnnotTestService").isEqualTo(inatsPolicy);

        doReturn(null).when(policyFinder).fetchSerializationPolicyName(any(), any());
        try {
            ReactiveGWT.prepareSettings(InnerTestService.class, settings, policyFinder);
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.POLICY_NAME_MISSING);
        }
    }

    @Test
    public void testInnerClassGetRemoteServiceRelativePathFromAnnotation() {
        assertThat(ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(InnerTestService.class))
                .as("Wrong path from annotation").isEqualTo("innertest");
        assertThat(ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(InnerTestServiceAsync.class))
                .as("Wrong path from Async annotation").isEqualTo("innertest");
        try {
            ReactiveGWT
                    .getRemoteServiceRelativePathFromAnnotation(InnerNoAnnotTestService.class);
            fail("Should have thrown an exception");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH);
        }
        try {
            ReactiveGWT
                    .getRemoteServiceRelativePathFromAnnotation(InnerNoAnnotTestServiceAsync.class);
            fail("Should have thrown an exception");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH);
        }
        try {
            ReactiveGWT
                    .getRemoteServiceRelativePathFromAnnotation(InnerMissingTestServiceAsync.class);
            fail("Should have thrown an exception");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.SERVICE_BASE);
        }
    }

    @Test
    public void testRemoveLeadingSlashFromRemoteServiceRelativePathAnnotation() {
        assertThat(ReactiveGWT.getRemoteServiceRelativePathFromAnnotation(InnerTestServiceStartingWithSlash.class))
                .as("Wrong path from annotation").isEqualTo("innertest");
    }
}
