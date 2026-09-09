package com.example.ecommerce.product;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImportService {
    private static final int SAVE_BATCH_SIZE = 500;
    private final ProductRepository products;
    private final ProductCsvParser parser;
    @PersistenceContext
    private EntityManager entityManager;

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
        List<ImportCandidate> candidates = new ArrayList<>(SAVE_BATCH_SIZE);
        Set<String> csvSkus = new HashSet<>();
        AtomicInteger imported = new AtomicInteger();
        parser.forEach(file, result -> {
            if (result.error() != null) {
                errors.add(new ImportResult.RejectedRow(result.rowNumber(), result.sku(), result.error().getMessage()));
                return;
            }
            ProductCsvParser.ParsedRow row = result.row();
            if (!csvSkus.add(normalizeSku(row.sku()))) {
                errors.add(new ImportResult.RejectedRow(result.rowNumber(), row.sku(), "SKU already exists in CSV"));
                return;
            }
            candidates.add(new ImportCandidate(result.rowNumber(), row));
            if (candidates.size() == SAVE_BATCH_SIZE) {
                imported.addAndGet(importBatch(candidates, errors));
                candidates.clear();
            }
        });
        if (!candidates.isEmpty()) {
            imported.addAndGet(importBatch(candidates, errors));
        }
        return new ImportResult(imported.get(), errors.size(), errors);
    }

    private int importBatch(List<ImportCandidate> candidates, List<ImportResult.RejectedRow> errors) {
        Set<String> candidateSkus = candidates.stream()
                .map(candidate -> normalizeSku(candidate.row().sku()))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> existingSkus = products.findExistingSkus(candidateSkus);
        List<Product> batch = new ArrayList<>(candidates.size());
        for (ImportCandidate candidate : candidates) {
            ProductCsvParser.ParsedRow row = candidate.row();
            if (existingSkus.contains(normalizeSku(row.sku()))) {
                errors.add(new ImportResult.RejectedRow(candidate.rowNumber(), row.sku(), "SKU already exists"));
                continue;
            }
            batch.add(new Product(row.name(), row.sku(), row.description(), row.category(),
                    row.price(), row.stock(), row.weightKg()));
        }
        if (!batch.isEmpty()) {
            products.saveAll(batch);
            if (entityManager != null) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        return batch.size();
    }

    private String normalizeSku(String sku) {
        return sku.toLowerCase(Locale.ROOT);
    }

    private record ImportCandidate(int rowNumber, ProductCsvParser.ParsedRow row) {
    }
}
