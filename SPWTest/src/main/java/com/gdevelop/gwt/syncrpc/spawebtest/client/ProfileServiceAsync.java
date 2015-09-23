package com.gdevelop.gwt.syncrpc.spawebtest.client;

import com.gdevelop.gwt.syncrpc.spawebtest.shared.UserInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ProfileServiceAsync {
	public void getAuthProfile(AsyncCallback<UserInfo> callback);

	public void getOAuthProfile(AsyncCallback<UserInfo> callback);

}
