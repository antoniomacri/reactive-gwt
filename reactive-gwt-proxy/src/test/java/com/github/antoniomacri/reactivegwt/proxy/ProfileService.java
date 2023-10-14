package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("profile")
public interface ProfileService extends RemoteService {

    UserInfo getAuthProfile() throws UnauthenticateException;

    UserInfo getOAuthProfile() throws UnauthenticateException;

}
