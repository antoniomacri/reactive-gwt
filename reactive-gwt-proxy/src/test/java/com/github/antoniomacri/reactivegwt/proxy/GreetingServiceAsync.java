package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.ArrayList;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface GreetingServiceAsync {
	void greetServer(String input, AsyncCallback<String> callback)
			throws IllegalArgumentException;

	void greetServer2(String input, AsyncCallback<T1> callback)
			throws IllegalArgumentException;

	void greetServerArr(String input, AsyncCallback<ArrayList<String>> callback)
			throws IllegalArgumentException;
}
