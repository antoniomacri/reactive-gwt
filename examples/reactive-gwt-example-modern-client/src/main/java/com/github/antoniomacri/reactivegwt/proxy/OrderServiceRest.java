package com.github.antoniomacri.reactivegwt.proxy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import java.util.List;

@ApplicationScoped
@Path("orders")
public class OrderServiceRest {
    @Inject
    OrderServiceMutiny orderServiceMutiny;

    @POST
    @Path("echo")
    public Uni<OrderItem> echo(OrderItem orderItem) {
        return orderServiceMutiny.echo(orderItem);
    }

    @POST
    @Path("putList")
    public Uni<Integer> putList(List<OrderItem> list) {
        return orderServiceMutiny.putList(list);
    }
}
