package com.github.antoniomacri.reactivegwt.proxy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Path("orders")
public class OrderServiceRest {
    @Inject
    OrderServiceMutiny orderServiceMutiny;

    @POST
    public Uni<OrderItem> echo(OrderItem orderItem) {
        return orderServiceMutiny.echo(orderItem);
    }
}
