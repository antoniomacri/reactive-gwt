package com.github.antoniomacri.reactivegwt.servlet;

import com.github.antoniomacri.reactivegwt.proxy.OrderItem;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;


@QuarkusIntegrationTest
public class OrderServiceIT {
    @Test
    public void testEcho() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1);
        orderItem.setDescription("Example");

        OrderItem responseOrderItem = given()
                .contentType(ContentType.JSON)
                .body(orderItem)
                .when().log().all()
                .post("/orders/echo")
                .then().log().all()
                .statusCode(200)
                .extract()
                .as(OrderItem.class);
        assertThat(responseOrderItem)
                .isInstanceOf(OrderItem.class)
                .usingRecursiveComparison().isEqualTo(orderItem);
    }

    @Test
    public void testPutList() {
        OrderItem orderItem1 = new OrderItem();
        orderItem1.setId(1);
        orderItem1.setDescription("Example 1");
        OrderItem orderItem2 = new OrderItem();
        orderItem2.setId(2);
        orderItem2.setDescription("Example 2");
        var list = List.of(orderItem1, orderItem2);

        given()
                .contentType(ContentType.JSON)
                .body(list)
                .when().log().all()
                .post("/orders/putList")
                .then().log().all()
                .statusCode(200)
                .body(is("2"));
    }
}
