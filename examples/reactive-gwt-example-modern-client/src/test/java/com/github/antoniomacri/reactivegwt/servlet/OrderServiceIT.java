package com.github.antoniomacri.reactivegwt.servlet;

import com.github.antoniomacri.reactivegwt.proxy.OrderItem;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;


@QuarkusIntegrationTest
public class OrderServiceIT {
    @Test
    public void testCallSucceeds() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1);
        orderItem.setDescription("Example");

        OrderItem responseOrderItem = given()
                .contentType(ContentType.JSON)
                .body(orderItem)
                .when().log().all()
                .post("/orders")
                .then().log().all()
                .statusCode(200)
                .extract()
                .as(OrderItem.class);
        assertThat(responseOrderItem)
                .isInstanceOf(OrderItem.class)
                .usingRecursiveComparison().isEqualTo(orderItem);
    }
}
