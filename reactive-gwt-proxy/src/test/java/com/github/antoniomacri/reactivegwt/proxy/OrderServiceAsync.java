package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.http.client.Request;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public interface OrderServiceAsync {
    Request echo(OrderItem orderItem, AsyncCallback<OrderItem> callback);

    class OrderServiceImpl extends RemoteServiceServlet implements OrderService {
        @Override
        public OrderItem echo(OrderItem orderItem) {
            return orderItem;
        }
    }
}
