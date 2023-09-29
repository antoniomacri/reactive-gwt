package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.List;

@RemoteServiceRelativePath("large")
public interface LargePayloadService extends RemoteService {
    int ARRAY_SIZE = 70000;
    int PAYLOAD_SIZE = 1000;

    int[] testLargeResponseArray();

    List<UserInfo> testLargeResponsePayload();
}
