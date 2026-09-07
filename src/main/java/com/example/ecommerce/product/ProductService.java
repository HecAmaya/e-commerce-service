package com.example.ecommerce.product;

import com.example.ecommerce.common.ConflictException;
import com.example.ecommerce.common.NotFoundException;
import com.example.ecommerce.order.Order;
import com.example.ecommerce.order.OrderItem;
import com.example.ecommerce.order.OrderRepository;
import com.example.ecommerce.order.OrderRequest;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductService {
    private final ProductRepository products;
    private final OrderRepository orders;

    public ProductService(ProductRepository products, OrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<Product> find(String query) {
        return query == null || query.isBlank() ? products.findAll() : products.search(query.trim());
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " was not found"));
    }

    @Transactional
    public Product create(ProductRequest request) {
        if (products.existsBySkuIgnoreCase(request.sku().trim())) {
            throw new ConflictException("SKU already exists: " + request.sku());
        }
        return products.save(new Product(request.name().trim(), request.sku().trim(), request.description().trim(),
                request.category().trim(), request.price(), request.stock(), request.weightKg()));
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = get(id);
        products.findBySkuIgnoreCase(request.sku().trim()).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new ConflictException("SKU already exists: " + request.sku()); });
        product.setName(request.name().trim());
        product.setSku(request.sku().trim());
        product.setDescription(request.description().trim());
        product.setCategory(request.category().trim());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setWeightKg(request.weightKg());
        return products.save(product);
    }

    @Transactional
    public void delete(Long id) {
        if (!products.existsById(id)) {
            throw new NotFoundException("Product " + id + " was not found");
        }
        products.deleteById(id);
    }

    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file must not be empty");
        }
        int imported = 0;
        List<ImportResult.RejectedRow> errors = new ArrayList<>();
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true).setTrim(true).get().parse(reader)) {
            for (CSVRecord row : parser) {
                int rowNumber = (int) row.getRecordNumber() + 1;
                String sku = value(row, "sku");
                try {
                    String name = required(row, "name");
                    sku = required(row, "sku");
                    String description = required(row, "description");
                    String category = required(row, "category");
                    BigDecimal price = decimal(row, "price", false);
                    int stock = integer(row, "stock");
                    BigDecimal weight = decimal(row, "weight_kg", true);
                    if (products.existsBySkuIgnoreCase(sku)) {
                        throw new IllegalArgumentException("SKU already exists");
                    }
                    products.save(new Product(name, sku, description, category, price, stock, weight));
                    imported++;
                } catch (RuntimeException ex) {
                    errors.add(new ImportResult.RejectedRow(rowNumber, sku, ex.getMessage()));
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to parse CSV: " + ex.getMessage(), ex);
        }
        return new ImportResult(imported, errors.size(), errors);
    }

    @Transactional
    public Order purchase(OrderRequest request) {
        Product product = products.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new NotFoundException("Product " + request.productId() + " was not found"));
        if (product.getStock() < request.quantity()) {
            throw new ConflictException("Insufficient stock for " + product.getSku());
        }
        product.setStock(product.getStock() - request.quantity());
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));
        Order order = new Order(total);
        order.addItem(new OrderItem(product.getId(), product.getSku(), product.getName(), request.quantity(), product.getPrice()));
        products.save(product);
        return orders.save(order);
    }

    private static String value(CSVRecord row, String column) {
        return row.isMapped(column) ? row.get(column).trim() : "";
    }

    private static String required(CSVRecord row, String column) {
        String value = value(row, column);
        if (value.isBlank()) {
            throw new IllegalArgumentException(column + " is required");
        }
        return value;
    }

    private static BigDecimal decimal(CSVRecord row, String column, boolean allowZero) {
        String value = required(row, column);
        try {
            BigDecimal parsed = new BigDecimal(value);
            if ((allowZero && parsed.signum() < 0) || (!allowZero && parsed.signum() < 0)) {
                throw new IllegalArgumentException(column + " must not be negative");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(column + " must be a decimal number; received '" + value + "'");
        }
    }

    private static int integer(CSVRecord row, String column) {
        String value = required(row, column);
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException(column + " must not be negative");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(column + " must be an integer; received '" + value + "'");
        }
    }
}
