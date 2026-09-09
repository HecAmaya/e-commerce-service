package com.example.ecommerce.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.example.ecommerce.common.ConflictException;
import com.example.ecommerce.common.NotFoundException;
import com.example.ecommerce.order.OrderRepository;
import com.example.ecommerce.order.OrderRequest;
import com.example.ecommerce.order.OrderService;
import com.example.ecommerce.order.PaymentService;
import com.example.ecommerce.order.PaymentResult;
import com.example.ecommerce.order.SimulatedPaymentService;
import com.example.ecommerce.order.OrderStatus;
import com.example.ecommerce.order.FailedOrderService;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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
    @Mock PaymentService payments;
    @Mock FailedOrderService failedOrders;
    @BeforeEach
    void setUp() {
        productService = new ProductService(products);
        csvParser = new ProductCsvParser();
        importService = new ProductImportService(products, csvParser);
        orderService = new OrderService(products, orders, payments, failedOrders);
    }

    @Test
    void importReportsMalformedCurrencyAndFreeValues() {
        String csv = """
                name,sku,description,category,price,stock,weight_kg
                Valid,V-1,Description,Test,12.50,4,0.4
                Currency,C-1,Description,Test,$29.99,4,0.4
                Free,F-1,Description,Test,free,4,0.4
                """;
        when(products.findExistingSkus(any())).thenReturn(Set.of());
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
        when(products.findExistingSkus(any())).thenReturn(Set.of("e-1"));
        ImportResult result = importService.importCsv(new MockMultipartFile(
                "file", "items.csv", "text/csv", csv.getBytes()));
        assertEquals(0, result.imported());
        assertEquals("SKU already exists", result.errors().getFirst().reason());
    }

    @Test
    void importRejectsDuplicateSkusWithinCsvAndPersistsAcceptedRowsAsBatch() {
        String csv = """
                name,sku,description,category,price,stock,weight_kg
                First,SKU-1,Description,Test,12.50,4,0.4
                Duplicate,sku-1,Description,Test,14.50,4,0.4
                Second,SKU-2,Description,Test,16.50,4,0.4
                """;
        when(products.findExistingSkus(any())).thenReturn(Set.of());

        ImportResult result = importService.importCsv(new MockMultipartFile(
                "file", "items.csv", "text/csv", csv.getBytes()));

        assertEquals(2, result.imported());
        assertEquals(1, result.rejected());
        assertEquals("SKU already exists in CSV", result.errors().getFirst().reason());
        verify(products).saveAll(any());
        verify(products).findExistingSkus(Set.of("sku-1", "sku-2"));
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
    void parserRejectsHtmlAndScriptingInTextFields() {
        String csv = """
                name,sku,description,category,price,stock,weight_kg
                <script>alert('xss')</script>,X-1,Description,Test,1,1,0.1
                Safe,X-2,<img src=x onerror=alert(1)>,Test,1,1,0.1
                Safe,X-3,Description,<b>Test</b>,1,1,0.1
                """;

        List<ProductCsvParser.ParseResult> results = csvParser.parse(
                new MockMultipartFile("file", "items.csv", "text/csv", csv.getBytes()));

        assertEquals("name must not contain HTML or scripting tags", results.get(0).error().getMessage());
        assertEquals("description must not contain HTML or scripting tags", results.get(1).error().getMessage());
        assertEquals("category must not contain HTML or scripting tags", results.get(2).error().getMessage());
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
        when(products.findAll(org.mockito.ArgumentMatchers.<Specification<Product>>any(),
                any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(existing)));
        assertEquals(1, productService.find(null, null, null, null, PageRequest.of(0, 20)).getTotalElements());
        assertEquals(1, productService.find(" shoe ", " Category ", null, null,
                PageRequest.of(0, 20)).getTotalElements());

        ProductRequest request = new ProductRequest(" Name ", " SKU ", " Description ",
                " Category ", new BigDecimal("4.00"), 2, new BigDecimal("0.2"));
        when(products.existsBySkuIncludingInactive("SKU")).thenReturn(false);
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Product created = productService.create(request);
        assertEquals("Name", created.getName());
        assertEquals("SKU", created.getSku());

        when(products.findById(1L)).thenReturn(Optional.of(existing));
        Product updated = productService.update(1L, request);
        assertEquals("Name", updated.getName());
        when(products.findById(1L)).thenReturn(Optional.of(existing));
        productService.delete(1L);
        assertEquals(false, existing.isActive());
        verify(products, times(2)).save(existing);
    }

    @Test
    void productServiceCapsPageSizeAndRejectsInvalidSearchRangeAndSort() {
        when(products.findAll(org.mockito.ArgumentMatchers.<Specification<Product>>any(),
                any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        productService.find(null, null, null, null, PageRequest.of(0, 500));
        verify(products).findAll(org.mockito.ArgumentMatchers.<Specification<Product>>any(), eq(PageRequest.of(0, 100,
                org.springframework.data.domain.Sort.by("name", "id"))));

        assertThrows(IllegalArgumentException.class,
                () -> productService.find(null, null, new BigDecimal("10"), new BigDecimal("1"),
                        PageRequest.of(0, 20)));
        assertThrows(IllegalArgumentException.class,
                () -> productService.find(null, null, null, null,
                        PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("description"))));

        productService.find(null, null, null, null,
                PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("price").descending()));
        productService.find(null, null, null, null,
                PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("name", "id")));
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
        when(products.existsBySkuIncludingInactive("SKU")).thenReturn(true);
        assertThrows(ConflictException.class, () -> productService.create(request));

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
        when(payments.authorize(new BigDecimal("19.98"))).thenReturn(PaymentResult.APPROVED);
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

    @Test
    void purchaseMarksDeclinedPaymentAsFailed() {
        Product product = new Product("Item", "I-1", "Description", "Test", new BigDecimal("9.99"), 5, new BigDecimal("1.0"));
        when(products.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(payments.authorize(any())).thenReturn(PaymentResult.DECLINED);
        when(failedOrders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(ConflictException.class, () -> orderService.purchase(new OrderRequest(1L, 2)));

        org.mockito.ArgumentCaptor<com.example.ecommerce.order.Order> captor =
                org.mockito.ArgumentCaptor.forClass(com.example.ecommerce.order.Order.class);
        verify(failedOrders).save(captor.capture());
        assertEquals(OrderStatus.FAILED, captor.getValue().getStatus());
        assertEquals(5, product.getStock());
    }

    @Test
    void simulatedPaymentApprovesValidAmountsAndDeclinesInvalidAmounts() {
        SimulatedPaymentService simulatedPayments = new SimulatedPaymentService();

        assertEquals(PaymentResult.APPROVED, simulatedPayments.authorize(BigDecimal.ONE));
        assertEquals(PaymentResult.DECLINED, simulatedPayments.authorize(null));
        assertEquals(PaymentResult.DECLINED, simulatedPayments.authorize(BigDecimal.ONE.negate()));
    }
}
