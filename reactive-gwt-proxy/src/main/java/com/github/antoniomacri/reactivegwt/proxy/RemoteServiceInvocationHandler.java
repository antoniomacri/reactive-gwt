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
import com.google.gwt.user.client.rpc.*;
import com.google.gwt.user.client.rpc.RpcToken.RpcTokenImplementation;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter.ResponseReader;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.impl.SerializabilityUtil;
import org.apache.http.MethodNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static java.util.Map.entry;

/**
 * Handles method call delegation from the Proxy interfaces
 */
public class RemoteServiceInvocationHandler implements InvocationHandler {
    private static final Logger log = LoggerFactory.getLogger(RemoteServiceInvocationHandler.class);
    private static final Map<Class<?>, ResponseReader> JPRIMITIVETYPE_TO_RESPONSEREADER = Map.ofEntries(
            entry(boolean.class, ResponseReader.BOOLEAN),
            entry(byte.class, ResponseReader.BYTE),
            entry(char.class, ResponseReader.CHAR),
            entry(double.class, ResponseReader.DOUBLE),
            entry(float.class, ResponseReader.FLOAT),
            entry(int.class, ResponseReader.INT),
            entry(long.class, ResponseReader.LONG),
            entry(short.class, ResponseReader.SHORT),
            entry(void.class, ResponseReader.VOID)
    );


    private final HasProxySettings settings;
    RpcToken token;
    String serviceEntryPoint;
    RpcTokenExceptionHandler rpcTokenExceptionHandler;


    public RemoteServiceInvocationHandler(HasProxySettings settings) {
        this.settings = settings;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("Invoking method={} on proxy={}", method.getName(), proxy.getClass().getName());
        if (log.isDebugEnabled()) {
            StringBuilder intfs = new StringBuilder();
            for (Class<?> intf : proxy.getClass().getInterfaces()) {
                intfs.append(intf.getName()).append(",");
            }
            log.debug("Proxy has interfaces: " + intfs);
        }

        if (ServiceDefTarget.class.getName().equals(method.getDeclaringClass().getName())) {
            log.info("Handling invocation of ServiceDefTarget Interface");
            return handleServiceDefTarget(proxy, method, args);
        } else if (HasRpcToken.class.getName().equals(method.getDeclaringClass().getName())) {
            log.info("Handling invocation of HasRpcToken Interface");
            return handleHasRpcToken(proxy, method, args);
        } else if (HasProxySettings.class.getName().equals(method.getDeclaringClass().getName())) {
            log.info("Handling invocation of HasProxySettings Interface");
            return handleHasProxySettings(method, args);
        }

        String policyName = settings.getPolicyFinder().getOrFetchPolicyName(settings.getServiceName());
        SerializationPolicy policy = settings.getPolicyFinder().getSerializationPolicy(policyName);
        RemoteServiceProxy syncProxy = new RemoteServiceProxy(settings, policyName, policy, this.token, this.rpcTokenExceptionHandler);

        // Handle delegation of calls to the RemoteServiceProxy hierarchy
        if (SerializationStreamFactory.class.getName().equals(method.getDeclaringClass().getName())) {
            log.info("Handling invocation of SerializationStreamFactory Interface");
            return method.invoke(syncProxy, args);
        }

        log.info("Handling invocation of RemoteService Interface");
        return handleRemoteService(syncProxy, method, args);
    }

    /**
     * Handles method invocations to the {@link ServiceDefTarget} interface
     * implemented by the service.
     */
    protected Object handleServiceDefTarget(Object proxy, Method method, Object[] args) throws Throwable {
        if (ServiceDefTarget.class.getMethod("getSerializationPolicyName").equals(method)) {
            return this.settings.getPolicyFinder().getOrFetchPolicyName(settings.getServiceName());
        } else if (ServiceDefTarget.class.getMethod("setServiceEntryPoint", String.class).equals(method)) {
            this.serviceEntryPoint = (String) args[0];
            // Modify current base and relative Path to newly specific
            // serviceEntryPoint assuming that base path is part of
            // serviceEntryPoint
            // TODO May not be a valid assumption
            if (this.serviceEntryPoint.contains(this.settings.getModuleBaseUrl())) {
                String remoteServiceRelativePath = this.serviceEntryPoint.split(this.settings.getModuleBaseUrl())[1];
                this.settings.setRemoteServiceRelativePath(remoteServiceRelativePath);
            } else {
                log.warn("Unable to determine base (orig: {}) against: {}",
                        this.settings.getModuleBaseUrl(), this.serviceEntryPoint);
                throw new SyncProxyException(
                        determineProxyServiceBaseInterface(proxy),
                        InfoType.SERVICE_BASE_DELTA);
            }
            return null;
        } else if (ServiceDefTarget.class.getMethod("getServiceEntryPoint").equals(method)) {
            return this.serviceEntryPoint;
        }
        // TODO handle all methods
        throw new MethodNotSupportedException("Method: " + method.getName()
                                              + " in class: " + method.getDeclaringClass().getName()
                                              + " not defined for class: " + proxy.getClass().getName());
    }

    /**
     * Handles method invocations to the {@link HasRpcToken} interface
     * implemented by the service. Also handles Annotation of service with
     * {@link RpcTokenImplementation}
     *
     * @throws ClassNotFoundException if base service interface class cannot be found if actual
     *                                implemented interface is Async
     * @since 0.5
     */
    protected Object handleHasRpcToken(Object proxy, Method method, Object[] args) throws MethodNotSupportedException,
            NoSuchMethodException, ClassNotFoundException {
        if (HasRpcToken.class.getMethod("setRpcToken", RpcToken.class).equals(method)) {
            // Check if service has annotation defining the Token class and
            // that this token matches the specified class
            Class<?> srvcIntf = determineProxyServiceBaseInterface(proxy);
            if (srvcIntf != null) {
                RpcTokenImplementation rti = srvcIntf.getAnnotation(RpcTokenImplementation.class);
                // Replace $ in class name in order to handle inner classes
                if (rti != null
                    && !args[0].getClass().getName().replace("$", ".").equals(rti.value())) {
                    throw new RpcTokenException("Incorrect Token Class. Got "
                                                + args[0].getClass().getName() + " but expected: "
                                                + rti.value());
                }
            }

            this.token = (RpcToken) args[0];
            return null;
        } else if (HasRpcToken.class.getMethod("getRpcToken").equals(method)) {
            return this.token;
        } else if (HasRpcToken.class.getMethod("setRpcTokenExceptionHandler", RpcTokenExceptionHandler.class).equals(method)) {
            this.rpcTokenExceptionHandler = (RpcTokenExceptionHandler) args[0];
            return null;
        } else if (HasRpcToken.class.getMethod("getRpcTokenExceptionHandler").equals(method)) {
            return this.rpcTokenExceptionHandler;
        }
        throw new MethodNotSupportedException("Method: " + method.getName()
                                              + " in class: " + method.getDeclaringClass().getName()
                                              + " not defined for class: " + proxy.getClass().getName());
    }

    protected Object handleHasProxySettings(Method method, Object[] args) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        return method.invoke(this.settings, args);
    }


    private Object handleRemoteService(RemoteServiceProxy syncProxy, Method method, Object[] args) throws Throwable {
        // // Get Service Interface
        Class<?> remoteServiceIntf = method.getDeclaringClass();

        SerializationStreamWriter streamWriter = syncProxy.createStreamWriter();

        AsyncCallback<?> callback = null;
        Class<?>[] paramTypes = method.getParameterTypes();
        try {
            // Determine whether sync or async
            boolean isAsync = false;
            // String serviceIntfName =
            // method.getDeclaringClass().getCanonicalName();
            String serviceIntfName = remoteServiceIntf.getCanonicalName();
            int paramCount = paramTypes.length;
            Class<?> returnType = method.getReturnType();
            if (method.getDeclaringClass().getCanonicalName().endsWith("Async")) {
                log.info("Invoking as an Async Service");
                isAsync = true;
                serviceIntfName = serviceIntfName.substring(0, serviceIntfName.length() - 5);
                paramCount--;
                callback = (AsyncCallback<?>) args[paramCount];

                // Determine the return type
                Class<?>[] syncParamTypes = new Class[paramCount];
                System.arraycopy(paramTypes, 0, syncParamTypes, 0, paramCount);
                Class<?> clazz;
                try {
                    clazz = ClassLoading.loadClass(serviceIntfName);
                } catch (ClassNotFoundException e) {
                    throw new InvocationException(
                            "There is no sync version of " + serviceIntfName + "Async");
                }
                Method syncMethod;
                try {
                    syncMethod = clazz.getMethod(method.getName(), syncParamTypes);
                    log.debug("Sync Method determined: {}", syncMethod.getName());
                } catch (NoSuchMethodException nsme) {
                    StringBuilder temp = new StringBuilder();
                    for (Class<?> cl : syncParamTypes) {
                        temp.append(cl.getSimpleName()).append(",");
                    }
                    throw new NoSuchMethodException("SPNoMeth "
                                                    + method.getName() + " class "
                                                    + clazz.getSimpleName() + " params " + temp);
                }
                returnType = syncMethod.getReturnType();
            }

            // Interface name
            streamWriter.writeString(serviceIntfName);
            // Method name
            streamWriter.writeString(method.getName());

            // Params count
            streamWriter.writeInt(paramCount);

            // Params type
            for (int i = 0; i < paramCount; i++) {
                // streamWriter.writeString(computeBinaryClassName(paramTypes[i]));
                streamWriter.writeString(SerializabilityUtil.getSerializedTypeName(paramTypes[i]));
            }

            // Params
            for (int i = 0; i < paramCount; i++) {
                writeParam(streamWriter, paramTypes[i], args[i]);
            }

            String payload = streamWriter.toString();
            log.debug("Payload: {}", payload);
            if (isAsync) {
                log.info("Making Remote call as Async");
                final AsyncCallback callback_2 = callback;

                try {
                    CompletionStage<Object> stage = syncProxy.doInvokeAsync(getReaderFor(returnType), payload);
                    // Check to make sure response should be processed,
                    // or not in case of situation such as
                    // RpcTokenException handled by a separate handler
                    if (!syncProxy.shouldIgnoreResponse() && callback_2 != null) {
                        stage.handle((result, exception) -> {
                            if (exception != null) {
                                if (exception instanceof CompletionException) {
                                    exception = exception.getCause();
                                }
                                if (exception instanceof UndeclaredThrowableException) {
                                    exception = exception.getCause();
                                }
                                callback_2.onFailure(exception);
                            } else {
                                callback_2.onSuccess(result);
                            }
                            return null;
                        });
                    }
                } catch (Throwable e) {
                    if (callback_2 != null) {
                        callback_2.onFailure(e);
                    }
                }

                return null;
            } else {
                log.info("Making Remote call as Sync");
                return syncProxy.doInvoke(getReaderFor(returnType), payload);
            }
            /*
             * Object result = syncProxy.doInvoke(getReaderFor(returnType),
             * payload); if (callback != null){ callback.onSuccess(result); }
             * return result;
             */
        } catch (Throwable ex) {
            if (callback != null) {
                callback.onFailure(ex);
                return null;
            }
            Class<?>[] expClasses = method.getExceptionTypes();
            for (Class<?> clazz : expClasses) {
                if (clazz.isAssignableFrom(ex.getClass())) {
                    throw ex;
                }
            }

            throw ex;
        }
    }


    private ResponseReader getReaderFor(Class<?> type) {
        log.trace("Getting reader for: " + type.getName());
        ResponseReader primitiveResponseReader = JPRIMITIVETYPE_TO_RESPONSEREADER.get(type);
        if (primitiveResponseReader != null) {
            return primitiveResponseReader;
        }

        if (type == String.class) {
            return ResponseReader.STRING;
        }
        if (type == Void.class || type == void.class) {
            return ResponseReader.VOID;
        }

        return ResponseReader.OBJECT;
    }

    private void writeParam(SerializationStreamWriter streamWriter, Class<?> paramType, Object paramValue)
            throws SerializationException {
        if (paramType == boolean.class) {
            streamWriter.writeBoolean((Boolean) paramValue);
        } else if (paramType == byte.class) {
            streamWriter.writeByte((Byte) paramValue);
        } else if (paramType == char.class) {
            streamWriter.writeChar((Character) paramValue);
        } else if (paramType == double.class) {
            streamWriter.writeDouble((Double) paramValue);
        } else if (paramType == float.class) {
            streamWriter.writeFloat((Float) paramValue);
        } else if (paramType == int.class) {
            streamWriter.writeInt((Integer) paramValue);
        } else if (paramType == long.class) {
            streamWriter.writeLong((Long) paramValue);
        } else if (paramType == short.class) {
            streamWriter.writeShort((Short) paramValue);
        } else if (paramType == String.class) {
            streamWriter.writeString((String) paramValue);
        } else {
            streamWriter.writeObject(paramValue);
        }
    }


    /**
     * Attempts to determine the service interface class of the provided proxy
     * object.
     *
     * @return the base service interface class
     * @throws ClassNotFoundException if attempt to find base interface from class ending in
     *                                "Async" fails
     * @since 0.5
     */
    protected static Class<?> determineProxyServiceBaseInterface(Object proxy)
            throws ClassNotFoundException {
        for (Class<?> clazz : proxy.getClass().getInterfaces()) {
            if (RemoteService.class.isAssignableFrom(clazz)) {
                return clazz;
            }
            if (clazz.getName().endsWith(ReactiveGWT.ASYNC_POSTFIX)) {
                return ClassLoading.loadClass(clazz.getName().replace(ReactiveGWT.ASYNC_POSTFIX, ""));
            }
            // if (!ServiceDefTarget.class.equals(clazz)
            // && !HasRpcToken.class.equals(clazz)
            // && !SerializationStreamFactory.class.equals(clazz)) {
            // srvcIntf = Class.forName(clazz.getName().replace("Async",
            // ""));
            // }
        }
        return null;
    }
}
