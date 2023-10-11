/*
 * Copyright www.gdevelop.com.
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
import com.google.gwt.user.client.rpc.HasRpcToken;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.user.client.rpc.SerializationStreamFactory;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import java.lang.reflect.Proxy;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * See the official Wiki for details and examples on usage. As a starting point,
 * this class offers GWT-like GWT#create method access to create RPC service
 * interfaces.
 */
public class SyncProxy {

    /**
     * Similar action to Gwt.create(). This method assumes your service is
     * annotated with {@link RemoteServiceRelativePath} and that you have
     * appropriately set the base url: {@link #setBaseURL(String)}. See
     * {@link #suppressRelativePathWarning(boolean)} in the event your service
     * is not annotated with {@link RemoteServiceRelativePath}.
     *
     * @return the Async interface of the provided serviceIntf
     * @since 0.5
     */
    @SuppressWarnings("unchecked")
    public static <ServiceIntfAsync, ServiceIntf extends RemoteService> ServiceIntfAsync create(
            Class<ServiceIntf> serviceIntf) {
        logger.config("Create service: " + serviceIntf.getName());

        Class<ServiceIntfAsync> asyncServiceIntf;

        try {
            asyncServiceIntf = (Class<ServiceIntfAsync>) ClassLoading.loadClass(serviceIntf.getName() + ASYNC_POSTFIX);
        } catch (ClassNotFoundException e) {
            throw new SyncProxyException(serviceIntf, InfoType.SERVICE_BASE);
        }

        logger.config("Creating Async Service: " + asyncServiceIntf.getName());

        return createProxy(asyncServiceIntf, new ProxySettings());
    }

    /**
     * Creates the actual Sync and Async ProxyInterface for the service with the
     * specified options.This method assumes your service is annotated with
     * {@link RemoteServiceRelativePath} and that you have appropriately set the
     * base url: {@link #setBaseURL(String)}. See
     * {@link #suppressRelativePathWarning(boolean)} in the event your service
     * is not annotated with {@link RemoteServiceRelativePath}.
     *
     * @param serviceIntf the service to create a proxy for
     * @return an object representing either the Async or Sync interface,
     * whichever was provided as serviceIntf
     * @since 0.5
     */
    @SuppressWarnings("unchecked")
    public static <ServiceIntf> ServiceIntf createProxy(Class<ServiceIntf> serviceIntf, ProxySettings settings) {
        logger.config("Setting up Proxy: " + serviceIntf.getName());
        defaultUnsetSettings(serviceIntf, settings);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return (ServiceIntf) Proxy.newProxyInstance(classLoader, new Class[]{serviceIntf,
                        ServiceDefTarget.class, HasRpcToken.class, SerializationStreamFactory.class, HasProxySettings.class},
                new RemoteServiceInvocationHandler(settings));
    }

    /**
     * Similar action to Gwt.create() except that this creates a sync'ed proxy.
     * This method assumes your service is annotated with
     * {@link RemoteServiceRelativePath} and that you have appropriately set the
     * base url: {@link #setBaseURL(String)}. See
     * {@link #suppressRelativePathWarning(boolean)} in the event your service
     * is not annotated with {@link RemoteServiceRelativePath}.
     *
     * @return a Sync Service interface object representing the serviceIntf
     * provided
     * @since 0.5
     */
    public static <ServiceIntf extends RemoteService> ServiceIntf createSync(Class<ServiceIntf> serviceIntf) {
        logger.config("Create Sync Service: " + serviceIntf.getName());
        return createProxy(serviceIntf, new ProxySettings());
    }

    /**
     * Sets default values to the settings parameters that are not yet set
     */
    protected static <ServiceIntf> ProxySettings defaultUnsetSettings(Class<ServiceIntf> serviceIntf,
                                                                      ProxySettings settings) {
        logger.info("Updating Default Settings for Unset Values");
        if (settings.getModuleBaseUrl() == null) {
            if (moduleBaseURL == null) {
                throw new SyncProxyException(serviceIntf, InfoType.MODULE_BASE_URL);
            }
            logger.config("Setting server base to module: " + moduleBaseURL);
            settings.setModuleBaseUrl(moduleBaseURL);
        }
        logger.finer("Server Base Url: " + settings.getModuleBaseUrl());
        if (settings.getRemoteServiceRelativePath() == null) {
            logger.config("Setting Service Relative Path by Annotation");
            settings.setRemoteServiceRelativePath(getRemoteServiceRelativePathFromAnnotation(serviceIntf));
        }
        logger.finer("Remote Service Relative path: " + settings.getRemoteServiceRelativePath());
        if (settings.getPolicyName() == null) {
            logger.config("Setting Policy Name by Map");
            settings.setPolicyName(POLICY_MAP.get(serviceIntf.getName()));
        }
        if (settings.getPolicyName() == null) {
            throw new SyncProxyException(serviceIntf, InfoType.POLICY_NAME_MISSING);
        }
        logger.finer("Service Policy name: " + settings.getPolicyName());
        if (settings.getCookieManager() == null) {
            logger.config("Setting Cookie Manager to Default");
            settings.setCookieManager(DEFAULT_COOKIE_MANAGER);
        }
        return settings;
    }

    public static Class<?>[] getLoggerClasses() {
        return spClazzes;
    }

    protected static Level getLoggingLevel() {
        return level;
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
                logger.info("Suppressed warning for lack of RemoteServiceRelativePath annotation on service: "
                        + baseServiceIntf);
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
     * @throws SyncProxyException if occurs during
     *                            {@link RpcPolicyFinder#fetchSerializationPolicyName(String)}
     * @since 0.5
     */
    protected static void populatePolicyMap() throws SyncProxyException {
        logger.info("Populating Policy Map");
        try {
            POLICY_MAP.putAll(RpcPolicyFinder.fetchSerializationPolicyName(moduleBaseURL));
        } catch (Exception e) {
            throw new SyncProxyException(InfoType.POLICY_NAME_POPULATION, e);
        }
    }

    /**
     * @param override indicates if the request to set the base URL should forcably
     *                 make the network call even if the data has already been set
     *                 before
     * @since 0.5.5
     */
    public static void setBaseURL(String baseUrl, boolean override) throws SyncProxyException {
        if (!override && moduleBaseURL != null && moduleBaseURL.equals(baseUrl)) {
            // Don't need to make another network call unless specifically told
            // to override, moduledBase has not yet been set, and specified base
            // is not different from already set base
            return;
        }
        moduleBaseURL = baseUrl;
        if (moduleBaseURL != null) {
            populatePolicyMap();
        } else {
            POLICY_MAP.clear();
            POLICY_MAP.putAll(RpcPolicyFinder.searchPolicyFileInClassPath());
        }
    }

    /**
     * Calls {@link #setBaseURL(String, boolean)} with an override of false
     *
     * @param baseUrl if null, existing {@link #POLICY_MAP} is cleared and
     *                repopulated from ClassPath (
     *                {@link RpcPolicyFinder#searchPolicyFileInClassPath()}). If
     *                Non-Null, {@link #POLICY_MAP} is appended with new population
     * @throws SyncProxyException if occurs during {@link #populatePolicyMap()}
     * @since 0.5
     */
    public static void setBaseURL(String baseUrl) throws SyncProxyException {
        setBaseURL(baseUrl, false);
    }

    /**
     * Sets logging level for all SyncProxy classes
     */
    public static void setLoggingLevel(Level level) {
        SyncProxy.level = level;
        Logger topLogger = java.util.logging.Logger.getLogger("");
        // Handler for console (reuse it if it already exists)
        Handler consoleHandler = null;
        // see if there is already a console handler
        for (Handler handler : topLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                // found the console handler
                consoleHandler = handler;
                break;
            }
        }

        if (consoleHandler == null) {
            // there was no console handler found, create a new one
            consoleHandler = new ConsoleHandler();
            topLogger.addHandler(consoleHandler);
        }
        // set the console handler to level
        consoleHandler.setLevel(level);

        for (Class<?> clazz : spClazzes) {
            Logger iLogger = Logger.getLogger(clazz.getName());
            iLogger.setLevel(level);
        }
    }

    /**
     * Static flag to suppress the exception issued if a RemoteService does not
     * implement the {@link RemoteServiceRelativePath} annotation
     *
     * @param suppressRelativePathWarning the suppressRelativePathWarning to set
     */
    public static void suppressRelativePathWarning(boolean suppressRelativePathWarning) {
        logger.info((suppressRelativePathWarning ? "" : "Not ") + "Supressing Relative Path Warning");
        SyncProxy.suppressRelativePathWarning = suppressRelativePathWarning;
    }

    /**
     * A list of the GSP core classes that GSP has, mainly used to activate
     * code-based logging requests
     *
     * @since 0.5
     */
    protected static Class<?>[] spClazzes = {SyncProxy.class, RpcPolicyFinder.class,
            RemoteServiceInvocationHandler.class, RemoteServiceSyncProxy.class,
            SyncClientSerializationStreamReader.class, SyncClientSerializationStreamWriter.class};
    /**
     * @since 0.5
     */
    static boolean suppressRelativePathWarning = false;

    static Level level;

    static Logger logger = Logger.getLogger(SyncProxy.class.getName());

    protected static final String ASYNC_POSTFIX = "Async";

    /**
     * @since 0.5
     */
    static protected String moduleBaseURL;

    /**
     * Map from ServiceInterface class name to Serialization Policy name. By
     * Default this is loaded with policy-file data from the classpath:
     * {@link RpcPolicyFinder#searchPolicyFileInClassPath()}
     */
    protected static final Map<String, String> POLICY_MAP = RpcPolicyFinder.searchPolicyFileInClassPath();

    private static final CookieManager DEFAULT_COOKIE_MANAGER = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
}
