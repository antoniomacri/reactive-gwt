package com.github.antoniomacri.reactivegwt.proxy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class OrderServiceProducer {

    @Produces
    public OrderServiceAsync produce() {
        String moduleBaseURL = "http://localhost:8090/AppModule/";
        ProxySettings proxySettings = new ProxySettings(moduleBaseURL, OrderService.class.getName());
        proxySettings.setSerializationStreamVersion(5);
        OrderServiceAsync serviceAsync = ReactiveGWT.create(OrderService.class, proxySettings);
        return serviceAsync;
    }

    @Produces
    public OrderServiceMutiny produce(OrderServiceAsync serviceAsync) {
        OrderServiceMutiny serviceMutiny = new OrderServiceMutiny(serviceAsync);
        return serviceMutiny;
    }
}
