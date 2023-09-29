/**
 * Jan 9, 2015 Copyright Blue Esoteric Web Development, LLC
 * Contact: P.Prith@BlueEsoteric.com
 */
package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.ArrayList;

/**
 * @author Preethum
 * @since 0.5
 *
 */
public interface CookieServiceAsync {
	void echoCookiesFromClient(AsyncCallback<ArrayList<String>> callback);

	void generateCookiesOnServer(AsyncCallback<Void> callback);

	void getSessionAttrib(AsyncCallback<String> callback);

	void invalidateAllSessions(AsyncCallback<Void> callback);

	void setSessionAttrib(String extra, AsyncCallback<Void> callback);
}
