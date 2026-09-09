package com.example.ecommerce.product.model;

import java.util.Collection;
import java.util.Set;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsBySkuIgnoreCase(String sku);
    Optional<Product> findBySkuIgnoreCase(String sku);

    @Query(value = "select lower(sku) from products where lower(sku) in (:skus)", nativeQuery = true)
    Set<String> findExistingSkus(Collection<String> skus);

    @Query(value = "select count(*) > 0 from products where lower(sku) = lower(:sku)", nativeQuery = true)
    boolean existsBySkuIncludingInactive(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(Long id);
}
