package com.example.ecommerce.order.services;

import com.example.ecommerce.order.model.PaymentResult;
import java.math.BigDecimal;

public interface PaymentService {
    PaymentResult authorize(BigDecimal amount);
}
