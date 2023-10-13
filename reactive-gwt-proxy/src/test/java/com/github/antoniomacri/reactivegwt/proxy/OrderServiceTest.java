package com.github.antoniomacri.reactivegwt.proxy;

import com.google.gwt.user.client.rpc.RpcAsyncTestBase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test serialization of enhanced classes.
 * <p>
 * Enhanced classes, such as JPA entities, have a special field serialized first,
 * which carries a blob of server-only fields as a base64 string. See JavaDoc for
 * {@link com.google.gwt.user.rebind.rpc.SerializableTypeOracleBuilder}.
 */
public class OrderServiceTest extends RpcAsyncTestBase<OrderService, OrderServiceAsync> {

    @Deployment(testable = false)
    @SuppressWarnings("unused")
    public static WebArchive getTestArchive() {
        return buildTestArchive(OrderService.class, OrderServiceAsync.OrderServiceImpl.class, SessionsTracker.class);
    }


    public OrderServiceTest() {
        super(OrderService.class);
    }


    @Test
    public void testEchoOrderItem() {
        OrderItem orderItem = new OrderItem();
        orderItem.setDescription("TONNO");
        orderItem.setId(2806670);
        service.echo(orderItem, createCallback(result ->
                assertThat(result).usingRecursiveComparison().isEqualTo(orderItem)
        ));
    }
}
