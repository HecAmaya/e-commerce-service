package com.example.ecommerce.order;

import com.example.ecommerce.common.ConflictException;
import com.example.ecommerce.common.NotFoundException;
import com.example.ecommerce.product.Product;
import com.example.ecommerce.product.ProductRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final ProductRepository products;
    private final OrderRepository orders;

    public OrderService(ProductRepository products, OrderRepository orders) {
        this.products = products;
        this.orders = orders;
    }

    @Transactional
    public Order purchase(OrderRequest request) {
        Product product = products.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new NotFoundException("Product " + request.productId() + " was not found"));
        if (product.getStock() < request.quantity()) {
            throw new ConflictException("Insufficient stock for " + product.getSku());
        }
        product.setStock(product.getStock() - request.quantity());
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(request.quantity()));
        Order order = new Order(total);
        order.addItem(new OrderItem(product.getId(), product.getSku(), product.getName(),
                request.quantity(), product.getPrice()));
        products.save(product);
        return orders.save(order);
    }
}
