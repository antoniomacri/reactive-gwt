package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;

public interface LargePayloadServiceAsync {
    void testLargeResponsePayload(AsyncCallback<List<UserInfo>> callback);

    void testLargeResponseArray(AsyncCallback<int[]> callback);
}
