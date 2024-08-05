package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.List;

@RemoteServiceRelativePath("typeIds")
public interface TypeIdCompatibleSerializationService extends RemoteService {
    void put(List<Integer> items);
}
