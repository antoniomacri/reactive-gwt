package com.github.antoniomacri.reactivegwt.servlet;

import com.github.antoniomacri.reactivegwt.proxy.OrderItem;
import com.github.antoniomacri.reactivegwt.proxy.OrderService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.util.List;

public class OrderServlet extends RemoteServiceServlet implements OrderService {

    private static final long serialVersionUID = 1L;

    @Override
    public OrderItem echo(OrderItem orderItem) {
        return orderItem;
    }

    @Override
    public int putList(List<OrderItem> list) {
        return list.size();
    }
}
