package com.github.antoniomacri.reactivegwt.client;

import com.github.antoniomacri.reactivegwt.proxy.OrderService;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

public class ExampleEntryPoint implements EntryPoint {
    public void onModuleLoad() {
        GWT.create(OrderService.class);
    }
}
