package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.ArrayList;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("greet")
public interface GreetingService extends RemoteService {
    int COUNT = 2;
    String NAME = "GWT User";

    String greetServer(String name) throws IllegalArgumentException;

    T1 greetServer2(String name) throws IllegalArgumentException;

    ArrayList<String> greetServerArr(String name)
            throws IllegalArgumentException;

}
