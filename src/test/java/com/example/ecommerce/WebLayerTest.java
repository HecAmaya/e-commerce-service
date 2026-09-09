package com.example.ecommerce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecommerce.order.controller.OrderController;
import com.example.ecommerce.order.model.Order;
import com.example.ecommerce.order.model.OrderRequest;
import com.example.ecommerce.order.services.OrderService;
import com.example.ecommerce.product.controller.ProductController;
import com.example.ecommerce.product.model.ImportResult;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.model.ProductRequest;
import com.example.ecommerce.product.services.ProductImportService;
import com.example.ecommerce.product.services.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class WebLayerTest {
    private final ProductService products = mock(ProductService.class);
    private final ProductImportService imports = mock(ProductImportService.class);
    private final ProductController productController = new ProductController(products, imports);
    private final OrderService orders = mock(OrderService.class);
    private final OrderController orderController = new OrderController(orders);

    @Test
    void productControllerDelegatesCatalogOperations() {
        Product product = new Product("Name", "SKU", "Description", "Category",
                BigDecimal.ONE, 2, BigDecimal.ONE);
        ProductRequest request = new ProductRequest("Name", "SKU", "Description",
                "Category", BigDecimal.ONE, 2, BigDecimal.ONE);
        when(products.find(org.mockito.ArgumentMatchers.eq("shoe"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(products.get(1L)).thenReturn(product);
        when(products.create(request)).thenReturn(product);
        when(products.update(1L, request)).thenReturn(product);

        assertEquals(1, productController.find("shoe", null, null, null, PageRequest.of(0, 20)).getTotalElements());
        assertEquals(product, productController.get(1L));
        assertEquals(product, productController.create(request));
        assertEquals(product, productController.update(1L, request));
        productController.delete(1L);

        verify(products).delete(1L);
    }

    @Test
    void productControllerDelegatesCsvImport() {
        MockMultipartFile file = new MockMultipartFile("file", "items.csv",
                "text/csv", "name,sku".getBytes());
        ImportResult result = new ImportResult(1, 0, List.of());
        when(imports.importCsv(file)).thenReturn(result);

        assertEquals(result, productController.importCsv(file));
    }

    @Test
    void orderControllerDelegatesPurchase() {
        OrderRequest request = new OrderRequest(1L, 2);
        Order order = new Order(BigDecimal.TEN);
        when(orders.purchase(any(OrderRequest.class))).thenReturn(order);

        assertEquals(order, orderController.purchase(request));
        verify(orders).purchase(request);
    }
}
