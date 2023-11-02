package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * Remote Service for testing the deserialization of exceptions.
 */
@RemoteServiceRelativePath("throws")
public interface ExceptionThrowingService extends RemoteService {
    void throwCheckedException() throws Exception;
}
