package com.aryafatthurahman.backend.order.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end REST tests against the real Quarkus runtime backed by in-memory H2.
 * Each test uses distinct business keys because the schema is created once per
 * test JVM and data is shared across test methods.
 */
@QuarkusTest
class OrderResourceIT {

    private static final String BASE = "/api/v1/orders";

    @Test
    void createReturns201WithComputedTotalsAndEchoesRequestId() {
        Response response = createOrder("CUST-IT-CREATE", null);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(response.getHeader("Location")).contains(BASE + "/");

        var json = response.jsonPath();
        assertThat(json.getString("id")).isNotBlank();
        assertThat(json.getString("status")).isEqualTo("PENDING");
        assertThat(json.getString("currency")).isEqualTo("USD");
        assertThat(json.getString("customerEmail")).isEqualTo("grace@example.com");
        assertThat(money(json.getString("subtotal"))).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(money(json.getString("taxAmount"))).isEqualByComparingTo(new BigDecimal("2.06"));
        assertThat(money(json.getString("totalAmount"))).isEqualByComparingTo(new BigDecimal("27.06"));
        assertThat(json.getList("items")).hasSize(2);
        assertThat(json.getString("items[0].sku")).isEqualTo("IT-SKU-1");
        assertThat(json.getLong("version")).isEqualTo(0L);
    }

    @Test
    void repeatedIdempotencyKeyReturns200WithTheSameOrder() {
        String key = "idem-key-it-0001";

        Response first = createOrder("CUST-IT-IDEM", key);
        assertThat(first.statusCode()).isEqualTo(201);
        String firstId = first.jsonPath().getString("id");

        Response second = createOrder("CUST-IT-IDEM", key);
        assertThat(second.statusCode()).isEqualTo(200);
        assertThat(second.jsonPath().getString("id")).isEqualTo(firstId);
    }

    @Test
    void getReturnsOrderAndUnknownIdReturnsStructured404() {
        String id = createOrder("CUST-IT-GET", null).jsonPath().getString("id");

        Response found = RestAssured.given().get(BASE + "/" + id);
        assertThat(found.statusCode()).isEqualTo(200);
        assertThat(found.jsonPath().getString("id")).isEqualTo(id);
        assertThat(found.getHeader("X-Request-Id")).isNotBlank();

        Response missing = RestAssured.given().get(BASE + "/" + UUID.randomUUID());
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(missing.jsonPath().getString("error")).isEqualTo("Not Found");
        assertThat(missing.jsonPath().getString("code")).isEqualTo("NOT_FOUND");
        assertThat(missing.jsonPath().getString("path")).contains(BASE);
        assertThat(missing.jsonPath().getString("requestId")).isNotBlank();
    }

    @Test
    void listSupportsPaginationAndRejectsInvalidFilters() {
        createOrder("CUST-IT-LIST", null);

        Response page = RestAssured.given()
                .queryParam("status", "PENDING")
                .queryParam("page", 0)
                .queryParam("size", 5)
                .get(BASE);
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.jsonPath().getInt("page")).isZero();
        assertThat(page.jsonPath().getInt("size")).isEqualTo(5);
        assertThat(page.jsonPath().getLong("totalElements")).isGreaterThanOrEqualTo(1);
        assertThat(page.jsonPath().getList("content")).isNotNull();
        assertThat(page.jsonPath().getBoolean("first")).isTrue();

        Response badStatus = RestAssured.given().queryParam("status", "BOGUS").get(BASE);
        assertThat(badStatus.statusCode()).isEqualTo(400);
        assertThat(badStatus.jsonPath().getString("code")).isEqualTo("INVALID_STATUS_FILTER");

        Response badPage = RestAssured.given().queryParam("page", -1).get(BASE);
        assertThat(badPage.statusCode()).isEqualTo(400);
        assertThat(badPage.jsonPath().getString("code")).isEqualTo("INVALID_PAGE");

        Response badSize = RestAssured.given().queryParam("size", 101).get(BASE);
        assertThat(badSize.statusCode()).isEqualTo(400);
        assertThat(badSize.jsonPath().getString("code")).isEqualTo("INVALID_PAGE_SIZE");
    }

    @Test
    void statusTransitionAndCancelAreEnforcedOverHttp() {
        String id = createOrder("CUST-IT-STATUS", null).jsonPath().getString("id");

        Response confirmed = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"status\":\"CONFIRMED\"}")
                .patch(BASE + "/" + id + "/status");
        assertThat(confirmed.statusCode()).isEqualTo(200);
        assertThat(confirmed.jsonPath().getString("status")).isEqualTo("CONFIRMED");

        Response conflict = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"status\":\"DELIVERED\"}")
                .patch(BASE + "/" + id + "/status");
        assertThat(conflict.statusCode()).isEqualTo(409);
        assertThat(conflict.jsonPath().getString("code")).isEqualTo("INVALID_STATUS_TRANSITION");

        Response cancelled = RestAssured.given().delete(BASE + "/" + id);
        assertThat(cancelled.statusCode()).isEqualTo(200);
        assertThat(cancelled.jsonPath().getString("status")).isEqualTo("CANCELLED");

        Response repeatedCancel = RestAssured.given().delete(BASE + "/" + id);
        assertThat(repeatedCancel.statusCode()).isEqualTo(200);
        assertThat(repeatedCancel.jsonPath().getString("status")).isEqualTo("CANCELLED");
    }

    @Test
    void invalidPayloadReturnsValidationErrorEnvelope() {
        String body = """
                {
                  "customerId": "",
                  "customerName": "Missing Email",
                  "customerEmail": "not-an-email",
                  "items": []
                }
                """;

        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .post(BASE);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("code")).isEqualTo("VALIDATION_ERROR");
        assertThat(response.jsonPath().getList("violations")).isNotEmpty();
    }

    @Test
    void revenueEndpointReturnsAggregateEnvelope() {
        Response response = RestAssured.given().get(BASE + "/stats/revenue");

        assertThat(response.statusCode()).isEqualTo(200);
        var json = response.jsonPath();
        assertThat(json.getString("currency")).isEqualTo("USD");
        assertThat(json.getLong("totalOrders")).isGreaterThanOrEqualTo(0);
        assertThat(json.getList("byStatus")).isNotNull();
        assertThat(json.getString("generatedAt")).isNotBlank();
    }

    private Response createOrder(String customerId, String idempotencyKey) {
        String body = """
                {
                  "customerId": "%s",
                  "customerName": "Grace Hopper",
                  "customerEmail": "grace@example.com",
                  "currency": "USD",
                  "items": [
                    {"sku": "it-sku-1", "name": "Widget", "quantity": 2, "unitPrice": 10.00},
                    {"sku": "it-sku-2", "name": "Gadget", "quantity": 1, "unitPrice": 5.00}
                  ]
                }
                """.formatted(customerId);

        RequestSpecification request = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body);
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return request.post(BASE);
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
