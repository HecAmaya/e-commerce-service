package com.example.ecommerce.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {
}
