package com.example.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecommerce.order.Order;
import com.example.ecommerce.order.OrderRepository;
import com.example.ecommerce.order.OrderRequest;
import com.example.ecommerce.order.OrderService;
import com.example.ecommerce.product.Product;
import com.example.ecommerce.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EcommerceIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ProductRepository products;
    @Autowired private OrderRepository orders;
    @Autowired private OrderService orderService;

    @AfterEach
    void cleanDatabase() {
        orders.deleteAll();
        products.deleteAll();
    }

    @Test
    void productApiPersistsAndFiltersSoftDeletedProducts() throws Exception {
        String sku = "INT-" + System.nanoTime();
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Integration product","sku":"%s","description":"Test product",
                                "category":"Test","price":12.50,"stock":4,"weightKg":0.4}
                                """.formatted(sku)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value(sku));

        Product product = products.findBySkuIgnoreCase(sku).orElseThrow();
        mvc.perform(get("/api/products").param("q", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value(sku));

        product.setActive(false);
        products.saveAndFlush(product);
        mvc.perform(get("/api/products").param("q", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void exposesHealthAndReadinessEndpoints() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void duplicateSkuConstraintIsRejectedByApi() throws Exception {
        String sku = "DUP-" + System.nanoTime();
        products.saveAndFlush(new Product("Existing", sku, "Existing product", "Test",
                BigDecimal.ONE, 1, BigDecimal.ONE));

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Duplicate","sku":"%s","description":"Test product",
                                "category":"Test","price":2.00,"stock":1,"weightKg":1.0}
                                """.formatted(sku)))
                .andExpect(status().isConflict());
    }

    @Test
    void databaseEnforcesUniqueSkuConstraint() {
        String sku = "DB-DUP-" + System.nanoTime();
        products.saveAndFlush(new Product("First", sku, "Product", "Test",
                BigDecimal.ONE, 1, BigDecimal.ONE));

        assertThrows(DataIntegrityViolationException.class, () -> products.saveAndFlush(
                new Product("Second", sku, "Product", "Test", BigDecimal.ONE, 1, BigDecimal.ONE)));
    }

    @Test
    void concurrentPurchasesCannotOversellInventory() throws Exception {
        Product product = products.saveAndFlush(new Product("Limited", "LIMITED-" + System.nanoTime(),
                "Limited product", "Test", BigDecimal.TEN, 1, BigDecimal.ONE));
        Callable<Order> purchase = () -> orderService.purchase(new OrderRequest(product.getId(), 1));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Order>> results = executor.invokeAll(List.of(purchase, purchase));
            long successes = results.stream().filter(result -> {
                try {
                    result.get();
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }).count();

            assertEquals(1, successes);
            assertThat(products.findById(product.getId()).orElseThrow().getStock()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }
}
