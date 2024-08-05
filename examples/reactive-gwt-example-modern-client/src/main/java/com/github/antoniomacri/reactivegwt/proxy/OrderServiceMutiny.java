package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.AsyncCallback;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import jakarta.annotation.Generated;

import java.util.List;

@Generated("com.github.antoniomacri.reactivegwt.processor.ReactiveGwtProcessor")
public class OrderServiceMutiny {
    private final OrderServiceAsync async$;

    public OrderServiceMutiny(OrderServiceAsync async) {
        this.async$ = async;
    }

    private <T> AsyncCallback<T> createCallback(UniEmitter<? super T> em) {
        return new AsyncCallback<T>() {
            @Override
            public void onFailure(Throwable e) {
                em.fail(e);
            }

            @Override
            public void onSuccess(T result) {
                em.complete(result);
            }
        };
    }

    public Uni<OrderItem> echo(final OrderItem item) {
        return Uni.createFrom().emitter(em -> async$.echo(item, createCallback(em)));
    }

    public Uni<Integer> putList(final List<OrderItem> list) {
        return Uni.createFrom().emitter(em -> async$.putList(list, createCallback(em)));
    }
}
