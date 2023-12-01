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

import com.github.antoniomacri.reactivegwt.proxy.auth.ServiceAuthenticator;
import com.github.antoniomacri.reactivegwt.proxy.auth.TestModeHostVerifier;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.*;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter;
import com.google.gwt.user.server.rpc.SerializationPolicy;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

/**
 * Base on {@link com.google.gwt.user.client.rpc.impl.RemoteServiceProxy}
 * <p>
 * Modified by Antonio Macrì to make HTTP calls using the Java HttpClient
 * on a specific executor.
 */
public class RemoteServiceProxy implements SerializationStreamFactory {
    public static final String OAUTH_BEARER_HEADER = "Authorization";
    public static final String OAUTH_HEADER = "X-GSP-OAUTH-ID";

    static Logger logger = Logger.getLogger(RemoteServiceProxy.class.getName());

    public static boolean isReturnValue(String encodedResponse) {
        return encodedResponse.startsWith("//OK");
    }

    public static boolean isThrownException(String encodedResponse) {
        return encodedResponse.startsWith("//EX");
    }

    private final CookieManager cookieManager;
    private final String moduleBaseURL;
    private final String remoteServiceURL;
    private final RpcToken rpcToken;
    private final SerializationPolicy serializationPolicy;
    private final String serializationPolicyName;
    boolean ignoreResponse = false;

    RpcTokenExceptionHandler rpcTokenExceptionHandler;

    HasProxySettings settings;

    /**
     * Constructor to utilized provided Proxy Settings
     *
     * @since 0.6
     */
    public RemoteServiceProxy(HasProxySettings settings,
                              String serializationPolicyName,
                              SerializationPolicy serializationPolicy,
                              RpcToken rpcToken,
                              RpcTokenExceptionHandler rpcTokenExceptionhandler) {
        this.settings = settings;
        this.moduleBaseURL = settings.getModuleBaseUrl();
        this.remoteServiceURL = moduleBaseURL + settings.getRemoteServiceRelativePath();
        this.serializationPolicyName = serializationPolicyName;
        this.serializationPolicy = serializationPolicy;
        this.cookieManager = settings.getCookieManager();
        this.rpcToken = rpcToken;
        this.rpcTokenExceptionHandler = rpcTokenExceptionhandler;
    }

    @Override
    public SyncClientSerializationStreamReader createStreamReader(String encoded) throws SerializationException {
        SyncClientSerializationStreamReader reader = new SyncClientSerializationStreamReader(this.serializationPolicy);
        logger.finer("Preparing Stream Reader");
        reader.prepareToRead(encoded);
        logger.finer("Stream Reader Prepared");
        return reader;
    }

    @Override
    public SyncClientSerializationStreamWriter createStreamWriter() {
        SyncClientSerializationStreamWriter streamWriter = new SyncClientSerializationStreamWriter(null,
                this.moduleBaseURL, this.serializationPolicyName, this.serializationPolicy, this.rpcToken);
        streamWriter.prepareToWrite();

        return streamWriter;
    }

    private boolean requiresSecuredProtocol(URL serviceUrl) {
        logger.config("Checking if connection requires a secured protocol");
        ServiceAuthenticator authenticator = settings.getServiceAuthenticator();
        if (authenticator instanceof TestModeHostVerifier) {
            boolean whiteHost = ((TestModeHostVerifier) authenticator).isTestModeHost(serviceUrl);
            logger.config("WhiteHost status(" + serviceUrl + "): " + whiteHost);
            return !whiteHost;
        }
        logger.fine("TestModeHostVerifier not available, checking for HTTPS Protocol");
        return !serviceUrl.getProtocol().equalsIgnoreCase("https");
    }

    public <T> CompletionStage<T> doInvokeAsync(RequestCallbackAdapter.ResponseReader responseReader, String requestData) {
        HttpClient httpClient = createHttpClient();
        URI cookieUri = URI.create("http://" + URI.create(moduleBaseURL).getHost());
        HttpRequest request = createHttpRequest(requestData, cookieUri);

        return httpClient.sendAsync(request, BodyHandlers.ofString())
                .exceptionally(e -> {
                    throw new InvocationException("IOException while receiving RPC response", e);
                })
                .thenApply(response -> {
                    // get all headers
                    logger.fine("Checking Response");
                    response.headers().map().forEach((k, v) -> logger.finer(k + " : " + v));

                    int statusCode = response.statusCode();
                    String encodedResponse = response.body();
                    logger.config("Response code: " + statusCode);
                    logger.fine("Response payload: " + encodedResponse);
                    logger.config("Post-Response cookies:" + cookieManager.getCookieStore().get(cookieUri));

                    if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        // Do not provide full response data
                        throw new StatusCodeException(Response.SC_NOT_FOUND, "Not Found", null);
                    } else if (statusCode != HttpURLConnection.HTTP_OK) {
                        throw new StatusCodeException(statusCode, encodedResponse);
                    } else if (encodedResponse == null) {
                        // This can happen if the XHR is interrupted by the server dying
                        throw new InvocationException("No response payload");
                    } else if (isReturnValue(encodedResponse)) {
                        logger.info("Reading return value");
                        encodedResponse = encodedResponse.substring(4);
                        try {
                            // noinspection unchecked
                            return (T) responseReader.read(createStreamReader(encodedResponse));
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (isThrownException(encodedResponse)) {
                        logger.info("Handling Thrown exception");
                        encodedResponse = encodedResponse.substring(4);
                        Throwable throwable;
                        try {
                            throwable = (Throwable) createStreamReader(encodedResponse).readObject();
                            // Handle specific instance of RpcTokenException which may have
                            // a specified handler
                            if (throwable instanceof RpcTokenException && this.rpcTokenExceptionHandler != null) {
                                this.rpcTokenExceptionHandler.onRpcTokenException((RpcTokenException) throwable);
                                this.ignoreResponse = true;
                                return null;
                            }
                        } catch (SerializationException e) {
                            throw new UndeclaredThrowableException(e);
                        }
                        if (throwable instanceof RuntimeException) {
                            throw (RuntimeException) throwable;
                        } else {
                            throw new UndeclaredThrowableException(throwable);
                        }
                    } else {
                        throw new InvocationException("Unknown response " + encodedResponse);
                    }
                });
    }

    private HttpClient createHttpClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .executor(settings.getExecutor())
                .version(HttpClient.Version.HTTP_2)
                .cookieHandler(this.settings.getCookieManager())
                .build();
        return httpClient;
    }

    private HttpRequest createHttpRequest(String requestData, URI cookieUri) {
        // Auto apply authenticator if available. Makes it
        // possible that if the authenticator's values change (such as access
        // tokens that are refreshed), the client will not need to re-apply the
        // authenticator to the service. The RemoteServiceInvocationHandler will
        // pass on the stored settings (including authenticator) each time to
        // this Proxy, which will re-apply the appropriate settings data
        if (settings.getServiceAuthenticator() != null) {
            settings.getServiceAuthenticator().applyAuthenticationToService(settings);
        }
        logger.info("Send request to " + this.remoteServiceURL);
        logger.fine("Request payload: " + requestData);

        // Send request
        logger.config("Starting Request sending to " + this.remoteServiceURL);
        URI uri = URI.create(this.remoteServiceURL);
        URL url;
        try {
            url = uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(requestData))
                .uri(uri)
                .header(RpcRequestBuilder.STRONG_NAME_HEADER, this.serializationPolicyName)
                .header(RpcRequestBuilder.MODULE_BASE_HEADER, this.moduleBaseURL)
                .header("Content-Type", "text/x-gwt-rpc; charset=utf-8");

        if (settings.getOAuth2IdToken() != null) {
            if (requiresSecuredProtocol(url)) {
                throw new SecurityException(
                        "Cannot send OAUTH Id Token over a non-secured protocol. Please use HTTPS");
            }
            requestBuilder.header(OAUTH_HEADER, settings.getOAuth2IdToken());
        }
        if (settings.getOAuthBearerToken() != null) {
            if (requiresSecuredProtocol(url)) {
                throw new SecurityException(
                        "Cannot send OAUTH Bearer Token over a non-secured protocol. Please use HTTPS");
            }
            requestBuilder.header(OAUTH_BEARER_HEADER, "Bearer " + settings.getOAuthBearerToken());
        }

        // Review Settings for additional HTTP Headers to install as Request
        // Properties
        Map<String, String> customHeaders = settings.getCustomHeaders();
        if (customHeaders != null && !customHeaders.isEmpty()) {
            for (String key : customHeaders.keySet()) {
                requestBuilder.header(key, customHeaders.get(key));
            }
        }

        // Patch for Issue 21 - Modified to only send cookies for
        // moduleBaseURL host and sets the domain/path for the cookie in the
        // event
        // it is a user-added cookie without those values specified
        CookieStore store = this.cookieManager.getCookieStore();
        // Create the URI with port if specified
        String domain = URI.create(this.moduleBaseURL).getHost();
        String path = URI.create(this.remoteServiceURL).getPath();
        logger.fine("Cookie target uri: " + cookieUri);
        logger.config("Setting cookies: " + this.cookieManager.getCookieStore().get(cookieUri));
        for (HttpCookie cookie : store.get(cookieUri)) {
            // Domain must be specified on Cookie to be passed along in
            // Android
            if (cookie.getDomain() == null) {
                logger.finer("Setting domain for Cookie: " + cookie.getName() + " to " + domain);
                cookie.setDomain(domain);
            }
            // Path must be specified on Cookie to be passed along in POJ
            // and Android
            if (cookie.getPath() == null) {
                logger.finer("Setting path for Cookie: " + cookie.getName() + " to " + path);
                cookie.setPath(path);
            }
        }
        return requestBuilder.build();
    }

    /**
     * Specifically utilized if an RpcTokenException is returned and handled by
     * a separate handler
     */
    public boolean shouldIgnoreResponse() {
        return this.ignoreResponse;
    }
}
