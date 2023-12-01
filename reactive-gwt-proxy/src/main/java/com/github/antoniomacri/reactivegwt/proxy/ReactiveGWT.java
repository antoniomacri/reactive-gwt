/*
 * Copyright www.gdevelop.com.
 * Copyright Antonio Macrì.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.antoniomacri.reactivegwt.proxy;

import com.github.antoniomacri.reactivegwt.proxy.exception.SyncProxyException;
import com.github.antoniomacri.reactivegwt.proxy.exception.SyncProxyException.InfoType;
import com.google.gwt.user.client.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.Executors;

/**
 * Offers {@link com.google.gwt.core.client.GWT#create(Class)} methods to instantiate
 * proxies for RPC service interfaces.
 */
public class ReactiveGWT {
    private static final Logger log = LoggerFactory.getLogger(ReactiveGWT.class);
    private static boolean suppressRelativePathWarning = false;

    private static final CookieManager DEFAULT_COOKIE_MANAGER = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

    protected static final String ASYNC_POSTFIX = "Async";


    /**
     * Creates the client proxy for a GWT service interface.
     * <p>
     * It is similar to {@link com.google.gwt.core.client.GWT#create(Class)}. This method
     * assumes your service is annotated with {@link RemoteServiceRelativePath} (otherwise,
     * see {@link #suppressRelativePathWarning(boolean)}).
     *
     * @param serviceIntf   the {@link RemoteService} (sync) interface to instantiate
     * @param moduleBaseURL the base url of the remote service, which is prepended to the
     *                      {@link RemoteServiceRelativePath} value
     * @return the Async interface of the provided {@code serviceIntf}
     */
    public static <ServiceIntfAsync, ServiceIntf extends RemoteService>
    ServiceIntfAsync create(Class<ServiceIntf> serviceIntf, String moduleBaseURL) {
        RpcPolicyFinder policyFinder = new RpcPolicyFinder(moduleBaseURL);
        return create(serviceIntf, new ProxySettings(moduleBaseURL, serviceIntf.getName(), policyFinder));
    }

    /**
     * Creates the client proxy for a GWT service interface.
     * <p>
     * It is similar to {@link com.google.gwt.core.client.GWT#create(Class)}. This method
     * assumes your service is annotated with {@link RemoteServiceRelativePath} (otherwise,
     * see {@link #suppressRelativePathWarning(boolean)}).
     *
     * @param serviceIntf   the {@link RemoteService} (sync) interface to instantiate
     * @param proxySettings proxy settings, comprising the base url of the remote service
     * @return the Async interface of the provided {@code serviceIntf}
     */
    @SuppressWarnings("unchecked")
    public static <ServiceIntfAsync, ServiceIntf extends RemoteService>
    ServiceIntfAsync create(Class<ServiceIntf> serviceIntf, ProxySettings proxySettings) {
        Class<ServiceIntfAsync> asyncServiceIntf;

        try {
            asyncServiceIntf = (Class<ServiceIntfAsync>) ClassLoading.loadClass(serviceIntf.getName() + ASYNC_POSTFIX);
        } catch (ClassNotFoundException e) {
            throw new SyncProxyException(serviceIntf, InfoType.SERVICE_BASE);
        }

        return createProxy(asyncServiceIntf, proxySettings);
    }

    /**
     * Creates the actual Async ProxyInterface for the service with the
     * specified options. This method assumes your service is annotated with
     * {@link RemoteServiceRelativePath}. See
     * {@link #suppressRelativePathWarning(boolean)} in the event your service
     * is not annotated with {@link RemoteServiceRelativePath}.
     *
     * @param asyncServiceIntf the service to create a proxy for
     * @return an object representing the Async interface
     */
    @SuppressWarnings("unchecked")
    protected static <ServiceIntfAsync> ServiceIntfAsync createProxy(Class<ServiceIntfAsync> asyncServiceIntf, ProxySettings settings) {
        prepareSettings(asyncServiceIntf, settings);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return (ServiceIntfAsync) Proxy.newProxyInstance(
                classLoader,
                new Class[]{asyncServiceIntf, ServiceDefTarget.class, HasRpcToken.class, SerializationStreamFactory.class, HasProxySettings.class},
                new RemoteServiceInvocationHandler(settings)
        );
    }

    /**
     * Sets default values to the settings parameters that are not yet set
     */
    protected static <ServiceIntf> void prepareSettings(Class<ServiceIntf> serviceIntf, ProxySettings settings) {
        if (settings.getModuleBaseUrl() == null) {
            throw new SyncProxyException(serviceIntf, InfoType.MODULE_BASE_URL);
        }
        log.debug("service={} moduleBaseUrl={}", serviceIntf.getName(), settings.getModuleBaseUrl());

        if (settings.getRemoteServiceRelativePath() == null) {
            settings.setRemoteServiceRelativePath(getRemoteServiceRelativePathFromAnnotation(serviceIntf));
        }
        log.debug("service={} remoteServiceRelativePath={}", serviceIntf.getName(), settings.getRemoteServiceRelativePath());

        if (settings.getPolicyFinder() == null) {
            throw new SyncProxyException(serviceIntf, InfoType.POLICY_FINDER_MISSING);
        }

        if (settings.getCookieManager() == null) {
            log.debug("service={} cookieManager=default", serviceIntf.getName());
            settings.setCookieManager(DEFAULT_COOKIE_MANAGER);
        } else {
            log.debug("service={} cookieManager=custom", serviceIntf.getName());
        }

        if (settings.getExecutor() == null) {
            log.debug("service={} executor=default", serviceIntf.getName());
            settings.setExecutor(Executors.newSingleThreadExecutor());
        } else {
            log.debug("service={} executor={}", serviceIntf.getName(), settings.getExecutor());
        }
    }


    /**
     * Attempts to ascertain the remote service's relative path by retrieving
     * the {@link RemoteServiceRelativePath} annotation value from the base
     * interface. This method can take either the base interface or the Async
     * interface class.
     *
     * @param serviceIntf either the base or Async interface class
     * @since 0.5
     */
    protected static <ServiceIntf> String getRemoteServiceRelativePathFromAnnotation(Class<ServiceIntf> serviceIntf) {
        Class<?> baseServiceIntf = serviceIntf;
        if (serviceIntf.getName().endsWith(ASYNC_POSTFIX)) {
            // Try determine remoteServiceRelativePath from the 'sync' version
            // of the Async one
            String className = serviceIntf.getName();
            try {
                baseServiceIntf = ClassLoading.loadClass(className.substring(0, className.length() - ASYNC_POSTFIX.length()));
            } catch (ClassNotFoundException e) {
                throw new SyncProxyException(baseServiceIntf, InfoType.SERVICE_BASE, e);
            }
        }
        if (baseServiceIntf.getAnnotation(RemoteServiceRelativePath.class) == null) {
            if (isSuppressRelativePathWarning()) {
                log.info("Suppressed warning for lack of RemoteServiceRelativePath annotation on service={}",
                        baseServiceIntf);
                return "";
            }
            throw new SyncProxyException(baseServiceIntf, InfoType.REMOTE_SERVICE_RELATIVE_PATH);
        }
        return baseServiceIntf.getAnnotation(RemoteServiceRelativePath.class).value().replaceFirst("^/+", "");
    }

    public static boolean isSuppressRelativePathWarning() {
        return suppressRelativePathWarning;
    }

    /**
     * Static flag to suppress the exception issued if a RemoteService does not
     * implement the {@link RemoteServiceRelativePath} annotation
     *
     * @param suppressRelativePathWarning the suppressRelativePathWarning to set
     */
    public static void suppressRelativePathWarning(boolean suppressRelativePathWarning) {
        log.info("suppressRelativePathWarning={}", suppressRelativePathWarning);
        ReactiveGWT.suppressRelativePathWarning = suppressRelativePathWarning;
    }
}
