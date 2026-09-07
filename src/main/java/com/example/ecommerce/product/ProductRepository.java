package com.example.ecommerce.product;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySkuIgnoreCase(String sku);
    Optional<Product> findBySkuIgnoreCase(String sku);

    @Query("""
        select p from Product p
        where lower(p.name) like lower(concat('%', :query, '%'))
           or lower(p.sku) like lower(concat('%', :query, '%'))
           or lower(p.description) like lower(concat('%', :query, '%'))
           or lower(p.category) like lower(concat('%', :query, '%'))
        order by p.name
        """)
    List<Product> search(String query);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(Long id);
}
