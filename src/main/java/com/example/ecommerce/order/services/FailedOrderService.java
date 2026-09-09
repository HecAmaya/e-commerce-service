package com.example.ecommerce.order.services;

import com.example.ecommerce.order.model.Order;
import com.example.ecommerce.order.model.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FailedOrderService {
    private final OrderRepository orders;

    public FailedOrderService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order save(Order order) {
        return orders.save(order);
    }
}
