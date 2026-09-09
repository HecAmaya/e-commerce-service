package com.example.ecommerce.product;

import com.example.ecommerce.common.NoHtml;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @NoHtml String name,
        @NotBlank @NoHtml String sku,
        @NotBlank @NoHtml String description,
        @NotBlank @NoHtml String category,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotNull @Min(0) Integer stock,
        @NotNull @DecimalMin("0.000") @Digits(integer = 5, fraction = 3) BigDecimal weightKg) {
}
