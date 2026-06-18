package org.example.cafe24_demo_v1.product.application.service;

import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.product.application.command.CreateProductCommand;
import org.example.cafe24_demo_v1.product.application.command.DeleteProductCommand;
import org.example.cafe24_demo_v1.product.application.command.UpdateProductCommand;
import org.example.cafe24_demo_v1.product.domain.model.Product;
import org.example.cafe24_demo_v1.product.domain.model.ProductStatus;
import org.example.cafe24_demo_v1.product.domain.repository.ProductRepository;
import org.example.cafe24_demo_v1.product.domain.service.Cafe24ProductPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository repository;
    @Mock private Cafe24ProductPort cafe24ProductPort;
    @Mock private AppAuthorizationService authorizationService;

    private ProductService productService;

    private final TokenCredential credential = new TokenCredential(
            "access-token", "refresh-token", "Bearer", LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1)
    );

    @BeforeEach
    void setUp() {
        productService = new ProductService(repository, cafe24ProductPort, authorizationService);
    }

    @Test
    void register는_Cafe24에_등록하고_로컬DB에_저장한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Product created = Product.register("mymall", 1L, "상품", new BigDecimal("1000"), new BigDecimal("500"), ProductStatus.ON_SALE);
        given(cafe24ProductPort.createProduct("mymall", "상품", new BigDecimal("1000"), new BigDecimal("500"), credential))
                .willReturn(created);

        Product result = productService.register(new CreateProductCommand("mymall", "상품", new BigDecimal("1000"), new BigDecimal("500")));

        assertThat(result.getProductNo()).isEqualTo(1L);
        verify(repository).save(created);
    }

    @Test
    void update은_Cafe24에서_수정하고_로컬DB에_반영한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Product updated = Product.register("mymall", 1L, "수정된 상품", new BigDecimal("2000"), new BigDecimal("900"), ProductStatus.ON_SALE);
        given(cafe24ProductPort.updateProduct("mymall", 1L, "수정된 상품", new BigDecimal("2000"), new BigDecimal("900"), credential))
                .willReturn(updated);

        Product existing = Product.reconstitute(
                10L, 1L, "mymall", "기존 상품", new BigDecimal("1000"), new BigDecimal("500"),
                ProductStatus.ON_SALE, LocalDateTime.now(), LocalDateTime.now()
        );
        given(repository.findByProductNo(1L)).willReturn(Optional.of(existing));

        Product result = productService.update(new UpdateProductCommand("mymall", 1L, "수정된 상품", new BigDecimal("2000"), new BigDecimal("900")));

        assertThat(result.getProductName()).isEqualTo("수정된 상품");
        assertThat(existing.getProductName()).isEqualTo("수정된 상품");
        assertThat(existing.getPrice()).isEqualTo(new BigDecimal("2000"));
        verify(repository).save(existing);
    }

    @Test
    void delete는_Cafe24에서_삭제하고_로컬DB에서도_삭제한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);

        productService.delete(new DeleteProductCommand("mymall", 1L));

        verify(cafe24ProductPort).deleteProduct("mymall", 1L, credential);
        verify(repository).deleteByProductNo(1L);
    }

    @Test
    void deleteFromWebhook은_Cafe24를_호출하지_않고_로컬DB에서만_삭제한다() {
        productService.deleteFromWebhook(1L);

        verify(repository).deleteByProductNo(1L);
        verify(cafe24ProductPort, never()).deleteProduct(any(), any(), any());
    }

    @Test
    void upsertFromWebhook은_기존_상품이_있으면_갱신한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Product snapshot = Product.register("mymall", 1L, "변경된 이름", new BigDecimal("2000"), new BigDecimal("900"), ProductStatus.SUSPENDED);
        given(cafe24ProductPort.getProduct("mymall", 1L, credential)).willReturn(snapshot);

        Product existing = Product.reconstitute(
                10L, 1L, "mymall", "기존 이름", new BigDecimal("1000"), new BigDecimal("500"),
                ProductStatus.ON_SALE, LocalDateTime.now(), LocalDateTime.now()
        );
        given(repository.findByProductNo(1L)).willReturn(Optional.of(existing));

        productService.upsertFromWebhook("mymall", 1L);

        assertThat(existing.getProductName()).isEqualTo("변경된 이름");
        assertThat(existing.getStatus()).isEqualTo(ProductStatus.SUSPENDED);
        verify(repository).save(existing);
    }

    @Test
    void upsertFromWebhook은_없는_상품이면_신규로_저장한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Product snapshot = Product.register("mymall", 2L, "신규 상품", new BigDecimal("3000"), new BigDecimal("1500"), ProductStatus.ON_SALE);
        given(cafe24ProductPort.getProduct("mymall", 2L, credential)).willReturn(snapshot);
        given(repository.findByProductNo(2L)).willReturn(Optional.empty());

        productService.upsertFromWebhook("mymall", 2L);

        verify(repository).save(snapshot);
    }

    @Test
    void syncFromCafe24는_페이지가_가득_찰_때까지_반복_조회한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);

        List<Product> fullPage = fixedSizeProducts(100, 1);
        List<Product> lastPage = fixedSizeProducts(20, 101);

        given(cafe24ProductPort.getProducts("mymall", 0, 100, credential)).willReturn(fullPage);
        given(cafe24ProductPort.getProducts("mymall", 100, 100, credential)).willReturn(lastPage);
        given(repository.findByProductNo(any())).willReturn(Optional.empty());

        productService.syncFromCafe24("mymall");

        verify(cafe24ProductPort).getProducts("mymall", 0, 100, credential);
        verify(cafe24ProductPort).getProducts("mymall", 100, 100, credential);
        verify(repository, times(120)).save(any());
    }

    private List<Product> fixedSizeProducts(int size, long startProductNo) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            products.add(Product.register("mymall", startProductNo + i, "상품" + i, BigDecimal.TEN, BigDecimal.ONE, ProductStatus.ON_SALE));
        }
        return products;
    }
}
