package com.example.ecommerce.product;

import com.example.ecommerce.common.ConflictException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImportService {
    private final ProductRepository products;
    private final ProductCsvParser parser;

    public ProductImportService(ProductRepository products, ProductCsvParser parser) {
        this.products = products;
        this.parser = parser;
    }

    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file must not be empty");
        }

        int imported = 0;
        List<ImportResult.RejectedRow> errors = new ArrayList<>();
        for (ProductCsvParser.ParseResult result : parser.parse(file)) {
            if (result.error() != null) {
                errors.add(new ImportResult.RejectedRow(result.rowNumber(), result.sku(), result.error().getMessage()));
                continue;
            }
            ProductCsvParser.ParsedRow row = result.row();
            try {
                if (products.existsBySkuIgnoreCase(row.sku())) {
                    throw new ConflictException("SKU already exists");
                }
                products.save(new Product(row.name(), row.sku(), row.description(), row.category(),
                        row.price(), row.stock(), row.weightKg()));
                imported++;
            } catch (RuntimeException ex) {
                errors.add(new ImportResult.RejectedRow(result.rowNumber(), row.sku(), ex.getMessage()));
            }
        }
        return new ImportResult(imported, errors.size(), errors);
    }
}
