package com.example.ecommerce.order;

import java.math.BigDecimal;

public interface PaymentService {
    PaymentResult authorize(BigDecimal amount);
}
