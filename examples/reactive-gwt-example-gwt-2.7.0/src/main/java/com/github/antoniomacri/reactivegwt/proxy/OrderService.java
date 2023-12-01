package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("orders")
public interface OrderService extends RemoteService {
    OrderItem echo(OrderItem articolo);
}