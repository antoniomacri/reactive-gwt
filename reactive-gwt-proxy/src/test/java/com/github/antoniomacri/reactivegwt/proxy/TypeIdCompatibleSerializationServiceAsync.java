package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.http.client.Request;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;

public interface TypeIdCompatibleSerializationServiceAsync {
    Request put(List<Integer> items, AsyncCallback<Void> callback);
}
