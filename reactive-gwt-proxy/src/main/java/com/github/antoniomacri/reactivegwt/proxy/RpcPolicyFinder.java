/*
 * Copyright www.gdevelop.com.
 * Copyright Antonio Macrì
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

import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class RpcPolicyFinder {
    private static final Logger log = LoggerFactory.getLogger(RpcPolicyFinder.class);
    private static final String GWT_PRC_POLICY_FILE_EXT = ".gwt.rpc";
    private static final Pattern PERMUTATION_NAME_PATTERN = Pattern.compile("'([A-Z0-9]){32}'");
    private static final Pattern CACHE_JS_FILE_PATTERN = Pattern.compile("([A-Z0-9]){32}\\.cache\\.js");
    private static final Pattern POLICY_NAME_DOUBLE_QUOTES_PATTERN = Pattern.compile("\"([A-Z0-9]){32}\"");
    private static final Pattern POLICY_NAME_SINGLE_QUOTES_PATTERN = Pattern.compile("'([A-Z0-9]){32}'");
    private static final ExecutorService DEFAULT_EXECUTOR = Executors.newSingleThreadExecutor();

    private final String moduleBaseURL;
    private final Map<String, String> policyNameByService = new ConcurrentHashMap<>();
    private final Map<String, SerializationPolicy> policyByName = new ConcurrentHashMap<>();


    public RpcPolicyFinder(String moduleBaseURL) {
        this.moduleBaseURL = moduleBaseURL.trim();
    }

    public String getOrFetchPolicyName(String serviceName) {
        try {
            return getOrFetchPolicyNameAsync(serviceName, DEFAULT_EXECUTOR).toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new InvocationException("Error while fetching serialization policy", e);
        }
    }

    public CompletionStage<String> getOrFetchPolicyNameAsync(String serviceName, ExecutorService executor) {
        String policyName = policyNameByService.get(serviceName);
        if (policyName != null) {
            return CompletableFuture.completedFuture(policyName);
        } else {
            return fetchPolicyNameAsync(serviceName, executor);
        }
    }

    public CompletionStage<String> fetchPolicyNameAsync(String serviceName, ExecutorService executor) {
        return fetchSerializationPoliciesAsync(executor).thenApply(ignored -> {
            String newPolicyName = policyNameByService.get(serviceName);
            return newPolicyName;
        });
    }

    public SerializationPolicy getSerializationPolicy(String policyName) {
        return policyByName.get(policyName);
    }


    /**
     * Map from ServiceInterface class name to Serialization Policy name.
     */
    private CompletionStage<Void> fetchSerializationPoliciesAsync(Executor executor) {
        Map<String, String> newPolicyNameByService = new HashMap<>();
        Map<String, SerializationPolicy> newPolicyByName = new HashMap<>();

        log.info("Fetching serialization policies...");

        HttpClient httpClient = HttpClient.newBuilder()
                .executor(executor)
                .version(HttpClient.Version.HTTP_2)
                .build();

        String[] urlparts = moduleBaseURL.split("/");
        String moduleNoCacheJs = urlparts[urlparts.length - 1] + ".nocache.js";
        return getResposeTextAsync(moduleBaseURL + moduleNoCacheJs, httpClient).thenCompose(noCacheJsFileContent -> {
            Matcher matcher = PERMUTATION_NAME_PATTERN.matcher(noCacheJsFileContent);
            if (matcher.find()) {
                boolean xsiFrameLinker = noCacheJsFileContent.contains(".cache.js");
                if (xsiFrameLinker) {
                    log.debug("Searching for policies generated by XSIFrame linker");
                    String permutationFile = matcher.group().replace("'", "") + ".cache.js";
                    return getResposeTextAsync(moduleBaseURL + permutationFile, httpClient).thenApply(responseText ->
                            POLICY_NAME_DOUBLE_QUOTES_PATTERN.matcher(responseText).results()
                                    .map(mr -> mr.group().replace("\"", ""))
                                    .collect(Collectors.toSet())
                    );
                } else {
                    log.debug("Searching for policies generated by standard linker");
                    String permutationFile = matcher.group().replace("'", "") + ".cache.html";
                    return getResposeTextAsync(moduleBaseURL + permutationFile, httpClient).thenApply(responseText ->
                            POLICY_NAME_SINGLE_QUOTES_PATTERN.matcher(responseText).results()
                                    .skip(1 /* the permutation name */)
                                    .map(mr -> mr.group().replace("'", ""))
                                    .collect(Collectors.toSet())
                    );
                }
            } else {
                return CompletableFuture.completedFuture(Set.<String>of());
            }
        }).thenCompose(policyNames -> {
            if (policyNames == null || policyNames.isEmpty()) {
                // Examine the compilation-mappings.txt file that is generated by GWT, in the event
                // (such as in 2.7.0) that serialization policies are no longer in the nocache.js file
                log.info("No RemoteService fetched from server using JS/HTML fetcher, using JS fetcher...");
                return getResposeTextAsync(moduleBaseURL + "compilation-mappings.txt", httpClient).thenCompose(compilationMappings -> {
                    Matcher matcher = CACHE_JS_FILE_PATTERN.matcher(compilationMappings);
                    if (matcher.find()) {
                        String browserSpec = matcher.group();
                        return getResposeTextAsync(moduleBaseURL + browserSpec, httpClient).thenApply(cacheJs ->
                                POLICY_NAME_SINGLE_QUOTES_PATTERN.matcher(cacheJs).results()
                                        .skip(1 /* the permutation name */)
                                        .map(mr -> mr.group().replace("'", ""))
                                        .collect(Collectors.toSet())
                        );
                    } else {
                        return CompletableFuture.completedFuture(Set.<String>of());
                    }
                });
            } else {
                return CompletableFuture.completedFuture(policyNames);
            }
        }).thenCompose(policyNames -> {
            CompletableFuture<?>[] completableFutures = new CompletableFuture[policyNames.size()];
            int i = 0;
            for (String policyName : policyNames) {
                String policyUrl = moduleBaseURL + policyName + GWT_PRC_POLICY_FILE_EXT;
                completableFutures[i] = getResposeTextAsync(policyUrl, httpClient).thenAccept(policyContent -> {
                    SerializationPolicy serializationPolicy;
                    try {
                        serializationPolicy = SerializationPolicyLoader.load(new StringReader(policyContent), null);
                    } catch (IOException | ParseException e) {
                        log.error("Error while loading serialization policy " + policyName, e);
                        return;
                    }

                    AtomicInteger addedServices = new AtomicInteger();
                    policyContent.lines().forEach(line -> {
                        int pos = line.indexOf(", false, false, false, false, _, ");
                        if (pos > 0) {
                            newPolicyNameByService.put(line.substring(0, pos), policyName);
                            newPolicyNameByService.put(line.substring(0, pos) + "Async", policyName);
                            addedServices.incrementAndGet();
                        }
                    });
                    if (addedServices.get() > 0) {
                        newPolicyByName.put(policyName, serializationPolicy);
                        log.debug("Added policy from url={} with {} service(s)", policyUrl, addedServices.get());
                    }
                });
                i++;
            }
            return CompletableFuture.allOf(completableFutures);
        }).thenAccept(ignored -> {
            if (newPolicyNameByService.isEmpty()) {
                log.info("No RemoteService fetched from server");
            } else {
                log.info("Found {} RemoteService(s) from {} policies: {}",
                        newPolicyNameByService.size(), newPolicyByName.size(), String.join(", ", newPolicyNameByService.keySet()));
            }
            policyNameByService.putAll(newPolicyNameByService);
            policyByName.putAll(newPolicyByName);
        });
    }

    private CompletableFuture<String> getResposeTextAsync(String url, HttpClient httpClient) {
        URI uri = URI.create(url);
        HttpRequest request = HttpRequest.newBuilder().GET().uri(uri).build();

        log.debug("Getting resource at url=%s".formatted(url));
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            String responseText = response.body();
            return responseText;
        });
    }
}
