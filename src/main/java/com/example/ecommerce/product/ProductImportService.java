package com.example.ecommerce.product;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImportService {
    private static final int SAVE_BATCH_SIZE = 500;
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

        List<ImportResult.RejectedRow> errors = new ArrayList<>();
        List<ImportCandidate> candidates = new ArrayList<>();
        Set<String> csvSkus = new HashSet<>();
        for (ProductCsvParser.ParseResult result : parser.parse(file)) {
            if (result.error() != null) {
                errors.add(new ImportResult.RejectedRow(result.rowNumber(), result.sku(), result.error().getMessage()));
                continue;
            }
            ProductCsvParser.ParsedRow row = result.row();
            if (!csvSkus.add(normalizeSku(row.sku()))) {
                errors.add(new ImportResult.RejectedRow(result.rowNumber(), row.sku(), "SKU already exists in CSV"));
                continue;
            }
            candidates.add(new ImportCandidate(result.rowNumber(), row));
        }

        Set<String> candidateSkus = candidates.stream()
                .map(candidate -> normalizeSku(candidate.row().sku()))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> existingSkus = candidateSkus.isEmpty() ? Set.of() : products.findExistingSkus(candidateSkus);
        if (existingSkus == null) {
            existingSkus = Set.of();
        }

        List<Product> batch = new ArrayList<>(SAVE_BATCH_SIZE);
        int imported = 0;
        for (ImportCandidate candidate : candidates) {
            ProductCsvParser.ParsedRow row = candidate.row();
            if (existingSkus.contains(normalizeSku(row.sku()))) {
                errors.add(new ImportResult.RejectedRow(candidate.rowNumber(), row.sku(), "SKU already exists"));
                continue;
            }
            batch.add(new Product(row.name(), row.sku(), row.description(), row.category(),
                    row.price(), row.stock(), row.weightKg()));
            if (batch.size() == SAVE_BATCH_SIZE) {
                products.saveAll(batch);
                imported += batch.size();
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            products.saveAll(batch);
            imported += batch.size();
        }
        return new ImportResult(imported, errors.size(), errors);
    }

    private String normalizeSku(String sku) {
        return sku.toLowerCase(Locale.ROOT);
    }

    private record ImportCandidate(int rowNumber, ProductCsvParser.ParsedRow row) {
    }
}
