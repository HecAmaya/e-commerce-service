package com.example.ecommerce.product;

import java.util.List;

public record ImportResult(int imported, int rejected, List<RejectedRow> errors) {
    public record RejectedRow(int row, String sku, String reason) {
    }
}
