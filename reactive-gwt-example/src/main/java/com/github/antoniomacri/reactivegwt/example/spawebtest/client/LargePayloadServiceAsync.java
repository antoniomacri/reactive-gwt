package com.github.antoniomacri.reactivegwt.example.spawebtest.client;

import java.util.List;

import com.github.antoniomacri.reactivegwt.example.spawebtest.shared.UserInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface LargePayloadServiceAsync {
	public void testLargeResponsePayload(AsyncCallback<List<UserInfo>> callback);

	public void testLargeResponseArray(AsyncCallback<int[]> callback);
}
