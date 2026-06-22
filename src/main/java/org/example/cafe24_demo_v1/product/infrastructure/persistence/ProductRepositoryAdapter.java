package org.example.cafe24_demo_v1.product.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.product.domain.model.Product;
import org.example.cafe24_demo_v1.product.domain.model.ProductPage;
import org.example.cafe24_demo_v1.product.domain.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 도메인 인터페이스(ProductRepository)의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpaRepository;
    private final ProductMapper mapper;

    @Override
    public Optional<Product> findByMallIdAndProductNo(String mallId, Long productNo) {
        return jpaRepository.findByMallIdAndProductNo(mallId, productNo)
                .map(mapper::toDomain);
    }

    @Override
    public ProductPage findByMallId(String mallId, int page, int size) {
        Page<ProductEntity> result = jpaRepository.findByMallId(mallId, PageRequest.of(page, size));
        List<Product> products = result.getContent().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new ProductPage(products, result.getTotalElements());
    }

    @Override
    public void save(Product product) {
        ProductEntity entity = mapper.toEntity(product);
        ProductEntity saved = jpaRepository.save(entity);
        product.setId(saved.getId());
    }

    @Override
    public void deleteByMallIdAndProductNo(String mallId, Long productNo) {
        jpaRepository.deleteByMallIdAndProductNo(mallId, productNo);
    }
}
