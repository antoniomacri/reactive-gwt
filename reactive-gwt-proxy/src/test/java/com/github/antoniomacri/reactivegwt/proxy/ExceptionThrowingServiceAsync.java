package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ExceptionThrowingServiceAsync {
    void throwCheckedException(AsyncCallback<Void> callback);
}
