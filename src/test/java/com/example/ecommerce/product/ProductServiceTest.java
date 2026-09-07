package com.example.ecommerce.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecommerce.common.ConflictException;
import com.example.ecommerce.order.OrderRepository;
import com.example.ecommerce.order.OrderRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository products;
    @Mock OrderRepository orders;
    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(products, orders);
    }

    @Test
    void importReportsMalformedCurrencyAndFreeValues() {
        String csv = """
                name,sku,description,category,price,stock,weight_kg
                Valid,V-1,Description,Test,12.50,4,0.4
                Currency,C-1,Description,Test,$29.99,4,0.4
                Free,F-1,Description,Test,free,4,0.4
                """;
        when(products.existsBySkuIgnoreCase(any())).thenReturn(false);
        ImportResult result = service.importCsv(new MockMultipartFile("file", "items.csv", "text/csv", csv.getBytes()));
        assertEquals(1, result.imported());
        assertEquals(2, result.rejected());
        assertEquals("price must be a decimal number; received '$29.99'", result.errors().get(0).reason());
        assertEquals("price must be a decimal number; received 'free'", result.errors().get(1).reason());
    }

    @Test
    void importRejectsRowsWithoutSku() {
        String csv = """
                name,sku,description,category,price,stock,weight_kg
                Missing SKU,,Description,Test,12.50,4,0.4
                """;
        ImportResult result = service.importCsv(new MockMultipartFile(
                "file", "items.csv", "text/csv", csv.getBytes()));

        assertEquals(0, result.imported());
        assertEquals(1, result.rejected());
        assertEquals("sku is required", result.errors().getFirst().reason());
    }

    @Test
    void purchaseDecrementsStockAndPersistsOrder() {
        Product product = new Product("Item", "I-1", "Description", "Test", new BigDecimal("9.99"), 5, new BigDecimal("1.0"));
        when(products.findByIdForUpdate(1L)).thenReturn(java.util.Optional.of(product));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.purchase(new OrderRequest(1L, 2));
        assertEquals(3, product.getStock());
        verify(products).save(product);
        verify(orders).save(any());
    }

    @Test
    void purchaseRejectsInsufficientStock() {
        Product product = new Product("Item", "I-1", "Description", "Test", new BigDecimal("9.99"), 1, new BigDecimal("1.0"));
        when(products.findByIdForUpdate(1L)).thenReturn(java.util.Optional.of(product));
        assertThrows(ConflictException.class, () -> service.purchase(new OrderRequest(1L, 2)));
    }
}
