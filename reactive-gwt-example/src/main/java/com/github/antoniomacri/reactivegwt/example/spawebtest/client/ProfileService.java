package com.github.antoniomacri.reactivegwt.example.spawebtest.client;

import com.github.antoniomacri.reactivegwt.example.spawebtest.shared.UnauthenticateException;
import com.github.antoniomacri.reactivegwt.example.spawebtest.shared.UserInfo;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("rpcsuite/profile")
public interface ProfileService extends RemoteService {

	public UserInfo getAuthProfile() throws UnauthenticateException;

	public UserInfo getOAuthProfile() throws UnauthenticateException;

}
