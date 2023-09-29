/**
 * Jan 9, 2015 Copyright Blue Esoteric Web Development, LLC
 * Contact: P.Prith@BlueEsoteric.com
 */
package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.ArrayList;

/**
 * @author Preethum
 * @since 0.5
 */
@RemoteServiceRelativePath("cookieservice")
public interface CookieService extends RemoteService {
    String[] COOKIE_VALS = {"COOKIE_VAL1", "COOKIE_VAL2"};

    String SESSION_ATTRIB = "TestSession";

    ArrayList<String> echoCookiesFromClient();

    void generateCookiesOnServer();

    String getSessionAttrib();

    void invalidateAllSessions();

    void setSessionAttrib(String extra);
}
