package com.example.ecommerce.product;

import com.example.ecommerce.common.ConflictException;
import com.example.ecommerce.common.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<Product> find(String query) {
        return query == null || query.isBlank() ? products.findAll() : products.search(query.trim());
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " was not found"));
    }

    @Transactional
    public Product create(ProductRequest request) {
        if (products.existsBySkuIgnoreCase(request.sku().trim())) {
            throw new ConflictException("SKU already exists: " + request.sku());
        }
        return products.save(new Product(request.name().trim(), request.sku().trim(), request.description().trim(),
                request.category().trim(), request.price(), request.stock(), request.weightKg()));
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = get(id);
        products.findBySkuIgnoreCase(request.sku().trim()).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new ConflictException("SKU already exists: " + request.sku()); });
        product.setName(request.name().trim());
        product.setSku(request.sku().trim());
        product.setDescription(request.description().trim());
        product.setCategory(request.category().trim());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setWeightKg(request.weightKg());
        return products.save(product);
    }

    @Transactional
    public void delete(Long id) {
        if (!products.existsById(id)) {
            throw new NotFoundException("Product " + id + " was not found");
        }
        products.deleteById(id);
    }
}
