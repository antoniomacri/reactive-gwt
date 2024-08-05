package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;

public interface OrderServiceAsync {
    void echo(OrderItem item, AsyncCallback<OrderItem> callback);

    void putList(List<OrderItem> list, AsyncCallback<Integer> callback);
}
