package com.example.ecommerce.order;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class SimulatedPaymentService implements PaymentService {
    @Override
    public PaymentResult authorize(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            return PaymentResult.DECLINED;
        }
        return PaymentResult.APPROVED;
    }
}
