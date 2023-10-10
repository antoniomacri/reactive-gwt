/**
 * Dec 30, 2014 Copyright Blue Esoteric Web Development, LLC
 * Contact: P.Prith@BlueEsoteric.com
 */
package com.github.antoniomacri.reactivegwt.proxy;

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

/**
 * Tests the static verification methods within the main SyncProxy class. All
 * tests included here should not require a remote service to actually read from
 *
 * @author Preethum
 * @since 0.5
 */
public class SyncProxyTest {
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
        SyncProxy.setBaseURL(null);
        SyncProxy.suppressRelativePathWarning(false);
    }

    @Test
    public void testClassGetRemoteServiceRelativePathFromAnnotation() {
        assertThat(SyncProxy.getRemoteServiceRelativePathFromAnnotation(TestService.class))
                .as("Wrong path from annotation")
                .isEqualTo("test");
        assertThat(SyncProxy.getRemoteServiceRelativePathFromAnnotation(TestServiceAsync.class))
                .as("Wrong path from Async annotation")
                .isEqualTo("test");
        try {
            SyncProxy.getRemoteServiceRelativePathFromAnnotation(NoAnnotTestService.class);
            fail("Should have thrown an exception");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH);
        }
        try {
            SyncProxy.getRemoteServiceRelativePathFromAnnotation(NoAnnotTestServiceAsync.class);
            fail("Should have thrown an exception");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH);
        }
        try {
            SyncProxy.getRemoteServiceRelativePathFromAnnotation(MissingTestServiceAsync.class);
            fail("Should have thrown an exception");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.SERVICE_BASE);
        }
    }

    @Test
    public void testDefaultUnsetSettings() {
        ProxySettings settings = new ProxySettings();
        try {
            SyncProxy.defaultUnsetSettings(InnerTestService.class, settings);
            fail("Should have failed on lack of a server base url available");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.MODULE_BASE_URL);
        }
        // Setup in-place policy's for services test below
        String itsPolicy = "ITSPolicy";
        String inatsPolicy = "INATSPolicy";
        SyncProxy.POLICY_MAP.put(InnerTestService.class.getName(), itsPolicy);
        SyncProxy.POLICY_MAP.put(InnerNoAnnotTestService.class.getName(),
                inatsPolicy);
        String testUrl = "testUrl";
        String testUrl2 = "testUrl2";
        // Test Default assignment to moduleBaseUrl
        SyncProxy.moduleBaseURL = testUrl;
        SyncProxy.defaultUnsetSettings(InnerTestService.class, settings);
        assertThat(settings.getModuleBaseUrl())
                .as("Failed default module base url assignment").isEqualTo(testUrl);
        // Test Override of moduleBaseUrl
        settings.setModuleBaseUrl(testUrl2);
        SyncProxy.defaultUnsetSettings(InnerTestService.class, settings);
        assertThat(settings.getModuleBaseUrl())
                .as("Failed override module base url assignment").isEqualTo(testUrl2);
        // Test relative path assignment
        settings = new ProxySettings();
        try {
            SyncProxy.defaultUnsetSettings(InnerNoAnnotTestService.class,
                    settings);
            fail("Should have failed on lack of available annotation");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH);
        }
        String testRelativePath = "relativePath";
        settings.setRemoteServiceRelativePath(testRelativePath);
        SyncProxy.defaultUnsetSettings(InnerNoAnnotTestService.class, settings);
        assertThat(settings.getRemoteServiceRelativePath())
                .as("Failed to utilized manual relative path").isEqualTo(testRelativePath);
        SyncProxy.defaultUnsetSettings(InnerTestService.class, settings);
        assertThat(settings.getRemoteServiceRelativePath())
                .as("Failed to utilized manual relative path with annotation")
                .isEqualTo(testRelativePath);
        // Cookie manager should always be available
        assertThat(settings.getCookieManager())
                .as("Cookie manager should be default if not provided").isNotNull();
        CookieManager cm = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        settings.setCookieManager(cm);
        SyncProxy.defaultUnsetSettings(InnerTestService.class, settings);
        assertThat(settings.getCookieManager()).as("Wrong Cookie Manager").isEqualTo(cm);
        // Test policy names for provided test classes , then remove to check
        // for exception when missing
        settings = new ProxySettings();
        SyncProxy.defaultUnsetSettings(InnerTestService.class, settings);
        assertThat(settings.getPolicyName())
                .as("Wrong policy for InnerTestService").isEqualTo(itsPolicy);
        settings = new ProxySettings();
        settings.setRemoteServiceRelativePath(testRelativePath);
        SyncProxy.defaultUnsetSettings(InnerNoAnnotTestService.class, settings);
        assertThat(settings.getPolicyName())
                .as("Wrong policy for InnerNoAnnotTestService").isEqualTo(inatsPolicy);
        SyncProxy.POLICY_MAP.remove(InnerTestService.class.getName());
        SyncProxy.POLICY_MAP.remove(InnerNoAnnotTestService.class.getName());
        try {
            SyncProxy.defaultUnsetSettings(InnerTestService.class, settings);
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.POLICY_NAME_MISSING);
        }
    }

    @Test
    public void testInnerClassGetRemoteServiceRelativePathFromAnnotation() {
        assertThat(SyncProxy.getRemoteServiceRelativePathFromAnnotation(InnerTestService.class))
                .as("Wrong path from annotation").isEqualTo("innertest");
        assertThat(SyncProxy.getRemoteServiceRelativePathFromAnnotation(InnerTestServiceAsync.class))
                .as("Wrong path from Async annotation").isEqualTo("innertest");
        try {
            SyncProxy
                    .getRemoteServiceRelativePathFromAnnotation(InnerNoAnnotTestService.class);
            fail("Should have thrown an exception");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH);
        }
        try {
            SyncProxy
                    .getRemoteServiceRelativePathFromAnnotation(InnerNoAnnotTestServiceAsync.class);
            fail("Should have thrown an exception");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.REMOTE_SERVICE_RELATIVE_PATH);
        }
        try {
            SyncProxy
                    .getRemoteServiceRelativePathFromAnnotation(InnerMissingTestServiceAsync.class);
            fail("Should have thrown an exception");
        } catch (SyncProxyException spe) {
            spe.verify(InfoType.SERVICE_BASE);
        }
    }

    @Test
    public void testRemoveLeadingSlashFromRemoteServiceRelativePathAnnotation() {
        assertThat(SyncProxy.getRemoteServiceRelativePathFromAnnotation(InnerTestServiceStartingWithSlash.class))
                .as("Wrong path from annotation").isEqualTo("innertest");
    }
}
