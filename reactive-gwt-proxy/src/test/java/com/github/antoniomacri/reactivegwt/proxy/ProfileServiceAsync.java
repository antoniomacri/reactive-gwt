package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ProfileServiceAsync {
    void getAuthProfile(AsyncCallback<UserInfo> callback);

    void getOAuthProfile(AsyncCallback<UserInfo> callback);

}
