package com.example.ecommerce.order.services;

import com.example.ecommerce.order.model.Order;
import com.example.ecommerce.order.model.OrderItem;
import com.example.ecommerce.order.model.OrderRepository;
import com.example.ecommerce.order.model.OrderRequest;
import com.example.ecommerce.order.model.OrderStatus;
import com.example.ecommerce.order.model.PaymentResult;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.model.ProductRepository;
import com.example.ecommerce.common.ConflictException;
import com.example.ecommerce.common.NotFoundException;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final ProductRepository products;
    private final OrderRepository orders;
    private final PaymentService payments;
    private final FailedOrderService failedOrders;

    public OrderService(ProductRepository products, OrderRepository orders, PaymentService payments,
            FailedOrderService failedOrders) {
        this.products = products;
        this.orders = orders;
        this.payments = payments;
        this.failedOrders = failedOrders;
    }

    @Transactional
    public Order purchase(OrderRequest request) {
        Product product = products.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new NotFoundException("Product " + request.productId() + " was not found"));
        if (product.getStock() < request.quantity()) {
            throw new ConflictException("Insufficient stock for " + product.getSku());
        }
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));
        Order order = new Order(total);
        order.addItem(new OrderItem(product.getId(), product.getSku(), product.getName(),
                request.quantity(), product.getPrice()));
        if (payments.authorize(total) != PaymentResult.APPROVED) {
            order.setStatus(OrderStatus.FAILED);
            failedOrders.save(order);
            throw new ConflictException("Payment was declined");
        }
        order.setStatus(OrderStatus.COMPLETED);
        product.setStock(product.getStock() - request.quantity());
        products.save(product);
        return orders.save(order);
    }
}
