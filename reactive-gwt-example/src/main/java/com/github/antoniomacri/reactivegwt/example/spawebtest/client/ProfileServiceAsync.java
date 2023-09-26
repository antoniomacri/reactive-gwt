package com.github.antoniomacri.reactivegwt.example.spawebtest.client;

import com.github.antoniomacri.reactivegwt.example.spawebtest.shared.UserInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ProfileServiceAsync {
	public void getAuthProfile(AsyncCallback<UserInfo> callback);

	public void getOAuthProfile(AsyncCallback<UserInfo> callback);

}
