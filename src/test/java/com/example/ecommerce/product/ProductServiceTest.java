package com.example.ecommerce.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecommerce.common.ConflictException;
import com.example.ecommerce.common.NotFoundException;
import com.example.ecommerce.order.OrderRepository;
import com.example.ecommerce.order.OrderRequest;
import com.example.ecommerce.order.OrderService;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository products;
    @Mock OrderRepository orders;
    @Mock MultipartFile file;
    private ProductService productService;
    private ProductCsvParser csvParser;
    private ProductImportService importService;
    private OrderService orderService;
    @BeforeEach
    void setUp() {
        productService = new ProductService(products);
        csvParser = new ProductCsvParser();
        importService = new ProductImportService(products, csvParser);
        orderService = new OrderService(products, orders);
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
        ImportResult result = importService.importCsv(new MockMultipartFile("file", "items.csv", "text/csv", csv.getBytes()));
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
        ImportResult result = importService.importCsv(new MockMultipartFile(
                "file", "items.csv", "text/csv", csv.getBytes()));

        assertEquals(0, result.imported());
        assertEquals(1, result.rejected());
        assertEquals("sku is required", result.errors().getFirst().reason());
    }

    @Test
    void importRejectsEmptyFileAndDuplicateSku() {
        assertThrows(IllegalArgumentException.class, () -> importService.importCsv(
                new MockMultipartFile("file", "items.csv", "text/csv", new byte[0])));
        String csv = """
                name,sku,description,category,price,stock,weight_kg
                Existing,E-1,Description,Test,12.50,4,0.4
                """;
        when(products.existsBySkuIgnoreCase("E-1")).thenReturn(true);
        ImportResult result = importService.importCsv(new MockMultipartFile(
                "file", "items.csv", "text/csv", csv.getBytes()));
        assertEquals(0, result.imported());
        assertEquals("SKU already exists", result.errors().getFirst().reason());
    }

    @Test
    void parserReportsNegativeAndMalformedNumbers() {
        String csv = """
                name,sku,description,category,price,stock,weight_kg
                Negative,N-1,Description,Test,-1,4,0.4
                Stock,S-1,Description,Test,1,invalid,0.4
                Weight,W-1,Description,Test,1,4,-0.4
                """;
        List<ProductCsvParser.ParseResult> results = csvParser.parse(
                new MockMultipartFile("file", "items.csv", "text/csv", csv.getBytes()));
        assertEquals("price must not be negative", results.get(0).error().getMessage());
        assertEquals("stock must be an integer; received 'invalid'", results.get(1).error().getMessage());
        assertEquals("weight_kg must not be negative", results.get(2).error().getMessage());
    }

    @Test
    void parserWrapsInputErrors() throws IOException {
        doThrow(new IOException("read failed")).when(file).getInputStream();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> csvParser.parse(file));
        assertEquals("Unable to parse CSV: read failed", exception.getMessage());
    }

    @Test
    void productServiceSupportsSearchAndCrud() {
        Product existing = new Product(" Old ", "OLD", "Description", "Category",
                new BigDecimal("2.00"), 3, new BigDecimal("0.5"));
        when(products.findAll()).thenReturn(List.of(existing));
        when(products.search("shoe")).thenReturn(List.of(existing));
        assertEquals(1, productService.find(null).size());
        assertEquals(1, productService.find(" shoe ").size());

        ProductRequest request = new ProductRequest(" Name ", " SKU ", " Description ",
                " Category ", new BigDecimal("4.00"), 2, new BigDecimal("0.2"));
        when(products.existsBySkuIgnoreCase("SKU")).thenReturn(false);
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Product created = productService.create(request);
        assertEquals("Name", created.getName());
        assertEquals("SKU", created.getSku());

        when(products.findById(1L)).thenReturn(Optional.of(existing));
        Product updated = productService.update(1L, request);
        assertEquals("Name", updated.getName());
        verify(products).save(existing);

        when(products.existsById(1L)).thenReturn(true);
        productService.delete(1L);
        verify(products).deleteById(1L);
    }

    @Test
    void productServiceRejectsMissingAndConflictingProducts() {
        when(products.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> productService.get(99L));
        assertThrows(NotFoundException.class, () -> productService.update(99L,
                new ProductRequest("Name", "SKU", "Description", "Category",
                        BigDecimal.ONE, 1, BigDecimal.ONE)));

        ProductRequest request = new ProductRequest("Name", "SKU", "Description",
                "Category", BigDecimal.ONE, 1, BigDecimal.ONE);
        when(products.existsBySkuIgnoreCase("SKU")).thenReturn(true);
        assertThrows(ConflictException.class, () -> productService.create(request));

        when(products.existsById(99L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> productService.delete(99L));
    }

    @Test
    void productServiceRejectsDuplicateSkuOnUpdate() {
        Product existing = org.mockito.Mockito.mock(Product.class);
        Product other = org.mockito.Mockito.mock(Product.class);
        when(other.getId()).thenReturn(2L);
        when(products.findById(1L)).thenReturn(Optional.of(existing));
        when(products.findBySkuIgnoreCase("SKU")).thenReturn(Optional.of(other));
        ProductRequest request = new ProductRequest("Name", "SKU", "Description",
                "Category", BigDecimal.ONE, 1, BigDecimal.ONE);
        assertThrows(ConflictException.class, () -> productService.update(1L, request));
    }

    @Test
    void purchaseDecrementsStockAndPersistsOrder() {
        Product product = new Product("Item", "I-1", "Description", "Test", new BigDecimal("9.99"), 5, new BigDecimal("1.0"));
        when(products.findByIdForUpdate(1L)).thenReturn(java.util.Optional.of(product));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        orderService.purchase(new OrderRequest(1L, 2));
        assertEquals(3, product.getStock());
        verify(products).save(product);
        verify(orders).save(any());
    }

    @Test
    void purchaseRejectsInsufficientStock() {
        Product product = new Product("Item", "I-1", "Description", "Test", new BigDecimal("9.99"), 1, new BigDecimal("1.0"));
        when(products.findByIdForUpdate(1L)).thenReturn(java.util.Optional.of(product));
        assertThrows(ConflictException.class, () -> orderService.purchase(new OrderRequest(1L, 2)));
    }

    @Test
    void purchaseRejectsUnknownProduct() {
        when(products.findByIdForUpdate(404L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> orderService.purchase(new OrderRequest(404L, 1)));
    }
}
