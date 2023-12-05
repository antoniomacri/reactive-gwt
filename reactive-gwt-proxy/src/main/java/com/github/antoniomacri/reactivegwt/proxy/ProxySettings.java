/**
 * Copyright 2015 Blue Esoteric Web Development, LLC
 * <http://www.blueesoteric.com>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at <http://www.apache.org/licenses/LICENSE-2.0>
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.antoniomacri.reactivegwt.proxy;

import com.github.antoniomacri.reactivegwt.proxy.auth.ServiceAuthenticator;

import java.net.CookieManager;
import java.time.InstantSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Handles settings utilized by the {@link ReactiveGWT} proxy.
 */
public class ProxySettings implements HasProxySettings {
    String moduleBaseUrl;
    final String serviceName;
    final RpcPolicyFinder policyFinder;
    String bearerToken;
    CookieManager cookieManager;
    Map<String, String> headers;
    String oAuth2IdToken;
    String remoteServiceRelativePath;
    ServiceAuthenticator serviceAuthenticator;
    boolean waitForInvocation = false;
    ExecutorService executor;
    int serializationStreamVersion = 7;
    InstantSource instantSource = InstantSource.system();
    int serializationPolicyFetchMinIntervalMillis = 300_000;


    public ProxySettings(String moduleBaseUrl, String serviceName) {
        this.moduleBaseUrl = moduleBaseUrl;
        this.serviceName = serviceName;
        this.policyFinder = new RpcPolicyFinder(moduleBaseUrl);
    }

    public ProxySettings(String moduleBaseUrl, String serviceName, RpcPolicyFinder policyFinder) {
        this.moduleBaseUrl = moduleBaseUrl;
        this.serviceName = serviceName;
        this.policyFinder = policyFinder;
    }

    @Override
    public CookieManager getCookieManager() {
        return this.cookieManager;
    }

    @Override
    public Map<String, String> getCustomHeaders() {
        if (headers == null) {
            headers = new HashMap<>();
        }
        return headers;
    }

    @Override
    public String getModuleBaseUrl() {
        return this.moduleBaseUrl;
    }

    @Override
    public String getOAuth2IdToken() {
        return oAuth2IdToken;
    }

    @Override
    public String getOAuthBearerToken() {
        return bearerToken;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public RpcPolicyFinder getPolicyFinder() {
        return policyFinder;
    }

    @Override
    public String getRemoteServiceRelativePath() {
        return this.remoteServiceRelativePath;
    }

    @Override
    public ServiceAuthenticator getServiceAuthenticator() {
        return serviceAuthenticator;
    }

    @Override
    public boolean isWaitForInvocation() {
        return this.waitForInvocation;
    }

    @Override
    public ProxySettings setCookieManager(CookieManager cookieManager) {
        this.cookieManager = cookieManager;
        return this;
    }

    @Override
    public HasProxySettings setCustomHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public ProxySettings setModuleBaseUrl(String moduleBaseUrl) {
        this.moduleBaseUrl = moduleBaseUrl;
        return this;
    }

    @Override
    public HasProxySettings setOAuth2IdToken(String token) {
        this.oAuth2IdToken = token;
        return this;
    }

    @Override
    public HasProxySettings setOAuthBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
        return this;
    }

    @Override
    public ProxySettings setRemoteServiceRelativePath(String remoteServiceRelativePath) {
        this.remoteServiceRelativePath = remoteServiceRelativePath;
        return this;
    }

    @Override
    public HasProxySettings setServiceAuthenticator(ServiceAuthenticator authenticator) {
        serviceAuthenticator = authenticator;
        return this;
    }

    @Override
    public ProxySettings setWaitForInvocation(boolean waitForInvocation) {
        this.waitForInvocation = waitForInvocation;
        return this;
    }

    @Override
    public HasProxySettings setExecutor(ExecutorService executor) {
        this.executor = executor;
        return this;
    }

    @Override
    public ExecutorService getExecutor() {
        return executor;
    }

    public int getSerializationStreamVersion() {
        return serializationStreamVersion;
    }

    public HasProxySettings setSerializationStreamVersion(int serializationStreamVersion) {
        this.serializationStreamVersion = serializationStreamVersion;
        return this;
    }

    public InstantSource getInstantSource() {
        return instantSource;
    }

    public HasProxySettings setInstantSource(InstantSource instantSource) {
        this.instantSource = instantSource;
        return this;
    }

    public int getSerializationPolicyFetchMinIntervalMillis() {
        return serializationPolicyFetchMinIntervalMillis;
    }

    public HasProxySettings setSerializationPolicyFetchMinIntervalMillis(int serializationPolicyFetchMinIntervalMillis) {
        this.serializationPolicyFetchMinIntervalMillis = serializationPolicyFetchMinIntervalMillis;
        return this;
    }
}
