package com.example.ecommerce.product;

import com.example.ecommerce.common.ConflictException;
import com.example.ecommerce.common.NotFoundException;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "sku", "category", "price", "stock");
    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public Page<Product> find(String query, String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must not be greater than maxPrice");
        }
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
        return products.findAll(buildSearchSpecification(normalizedQuery, normalizedCategory, minPrice, maxPrice),
                normalizePageable(pageable));
    }

    private Specification<Product> buildSearchSpecification(String query, String category, BigDecimal minPrice,
            BigDecimal maxPrice) {
        return (root, criteriaQuery, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (query != null) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("sku")), pattern)));
            }
            if (category != null) {
                predicates.add(builder.equal(builder.lower(root.get("category")), category.toLowerCase()));
            }
            if (minPrice != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            pageable = PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
        } else {
            for (Sort.Order order : sort) {
                if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                    throw new IllegalArgumentException("Unsupported sort field: " + order.getProperty());
                }
            }
            if (sort.getOrderFor("id") == null) {
                sort = sort.and(Sort.by(Sort.Order.asc("id")));
            }
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " was not found"));
    }

    @Transactional
    public Product create(ProductRequest request) {
        if (products.existsBySkuIncludingInactive(request.sku().trim())) {
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
        Product product = get(id);
        product.setActive(false);
        products.save(product);
    }
}
