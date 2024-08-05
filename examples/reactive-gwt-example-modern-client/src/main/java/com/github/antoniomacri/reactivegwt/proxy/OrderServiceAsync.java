package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.http.client.Request;
import com.google.gwt.user.client.rpc.AsyncCallback;
import jakarta.annotation.Generated;

@Generated("com.github.antoniomacri.reactivegwt.processor.ReactiveGwtProcessor")
public interface OrderServiceAsync {
  Request echo(OrderItem articolo, AsyncCallback<OrderItem> callback);
}
