package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.List;

@RemoteServiceRelativePath("orders")
public interface OrderService extends RemoteService {
    OrderItem echo(OrderItem item);

    int putList(List<OrderItem> list);
}
