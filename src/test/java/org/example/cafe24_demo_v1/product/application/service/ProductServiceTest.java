package org.example.cafe24_demo_v1.product.application.service;

import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.product.application.command.CreateProductCommand;
import org.example.cafe24_demo_v1.product.application.command.DeleteProductCommand;
import org.example.cafe24_demo_v1.product.application.command.UpdateProductCommand;
import org.example.cafe24_demo_v1.product.domain.model.Product;
import org.example.cafe24_demo_v1.product.domain.model.ProductPage;
import org.example.cafe24_demo_v1.product.domain.model.ProductRegistration;
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
import static org.mockito.ArgumentMatchers.eq;
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
        Product created = product("mymall", 1L, "상품", "1000", "500", ProductStatus.ON_SALE);
        given(cafe24ProductPort.createProduct(eq("mymall"), any(ProductRegistration.class), eq(credential)))
                .willReturn(created);

        Product result = productService.register(new CreateProductCommand(
                "mymall", "상품", new BigDecimal("1000"), new BigDecimal("500"),
                null, null, null, null, null, null, null
        ));

        assertThat(result.getProductNo()).isEqualTo(1L);
        verify(repository).save(created);
    }

    @Test
    void update은_Cafe24에서_수정하고_로컬DB에_반영한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Product updated = product("mymall", 1L, "수정된 상품", "2000", "900", ProductStatus.ON_SALE);
        given(cafe24ProductPort.updateProduct(eq("mymall"), eq(1L), any(ProductRegistration.class), eq(credential)))
                .willReturn(updated);

        Product existing = existingProduct(10L, 1L, "mymall", "기존 상품", "1000", "500", ProductStatus.ON_SALE);
        given(repository.findByMallIdAndProductNo("mymall", 1L)).willReturn(Optional.of(existing));

        Product result = productService.update(new UpdateProductCommand(
                "mymall", 1L, "수정된 상품", new BigDecimal("2000"), new BigDecimal("900"),
                null, null, null, null, null, null, null
        ));

        assertThat(result.getProductName()).isEqualTo("수정된 상품");
        assertThat(existing.getProductName()).isEqualTo("수정된 상품");
        assertThat(existing.getPrice()).isEqualTo(new BigDecimal("2000"));
        verify(repository).save(existing);
    }

    @Test
    void list는_repository에서_mallId_기준으로_페이지를_조회한다() {
        ProductPage page = new ProductPage(List.of(product("mymall", 1L, "상품", "1000", "500", ProductStatus.ON_SALE)), 1L);
        given(repository.findByMallId("mymall", 0, 20)).willReturn(page);

        ProductPage result = productService.list("mymall", 0, 20);

        assertThat(result.totalCount()).isEqualTo(1L);
        assertThat(result.products()).hasSize(1);
    }

    @Test
    void list는_size가_최대값을_넘으면_100으로_제한한다() {
        given(repository.findByMallId("mymall", 0, 100)).willReturn(new ProductPage(List.of(), 0L));

        productService.list("mymall", 0, 999);

        verify(repository).findByMallId("mymall", 0, 100);
    }

    @Test
    void list는_page가_음수면_0으로_보정한다() {
        given(repository.findByMallId("mymall", 0, 20)).willReturn(new ProductPage(List.of(), 0L));

        productService.list("mymall", -1, 20);

        verify(repository).findByMallId("mymall", 0, 20);
    }

    @Test
    void delete는_Cafe24에서_삭제하고_로컬DB에서도_삭제한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);

        productService.delete(new DeleteProductCommand("mymall", 1L));

        verify(cafe24ProductPort).deleteProduct("mymall", 1L, credential);
        verify(repository).deleteByMallIdAndProductNo("mymall", 1L);
    }

    @Test
    void deleteFromWebhook은_Cafe24를_호출하지_않고_로컬DB에서만_삭제한다() {
        productService.deleteFromWebhook("mymall", 1L);

        verify(repository).deleteByMallIdAndProductNo("mymall", 1L);
        verify(cafe24ProductPort, never()).deleteProduct(any(), any(), any());
    }

    @Test
    void upsertFromWebhook은_기존_상품이_있으면_갱신한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Product snapshot = product("mymall", 1L, "변경된 이름", "2000", "900", ProductStatus.SUSPENDED);
        given(cafe24ProductPort.getProduct("mymall", 1L, credential)).willReturn(snapshot);

        Product existing = existingProduct(10L, 1L, "mymall", "기존 이름", "1000", "500", ProductStatus.ON_SALE);
        given(repository.findByMallIdAndProductNo("mymall", 1L)).willReturn(Optional.of(existing));

        productService.upsertFromWebhook("mymall", 1L);

        assertThat(existing.getProductName()).isEqualTo("변경된 이름");
        assertThat(existing.getStatus()).isEqualTo(ProductStatus.SUSPENDED);
        verify(repository).save(existing);
    }

    @Test
    void upsertFromWebhook은_없는_상품이면_신규로_저장한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Product snapshot = product("mymall", 2L, "신규 상품", "3000", "1500", ProductStatus.ON_SALE);
        given(cafe24ProductPort.getProduct("mymall", 2L, credential)).willReturn(snapshot);
        given(repository.findByMallIdAndProductNo("mymall", 2L)).willReturn(Optional.empty());

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
        given(repository.findByMallIdAndProductNo(any(), any())).willReturn(Optional.empty());

        productService.syncFromCafe24("mymall");

        verify(cafe24ProductPort).getProducts("mymall", 0, 100, credential);
        verify(cafe24ProductPort).getProducts("mymall", 100, 100, credential);
        verify(repository, times(120)).save(any());
    }

    private List<Product> fixedSizeProducts(int size, long startProductNo) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            products.add(Product.register(
                    "mymall", startProductNo + i, "상품" + i, BigDecimal.TEN, BigDecimal.ONE, ProductStatus.ON_SALE,
                    null, null, null, null, null, null, null
            ));
        }
        return products;
    }

    private Product product(String mallId, Long productNo, String productName, String price, String supplyPrice, ProductStatus status) {
        return Product.register(
                mallId, productNo, productName, new BigDecimal(price), new BigDecimal(supplyPrice), status,
                null, null, null, null, null, null, null
        );
    }

    private Product existingProduct(Long id, Long productNo, String mallId, String productName, String price, String supplyPrice, ProductStatus status) {
        return Product.reconstitute(
                id, productNo, mallId, productName, new BigDecimal(price), new BigDecimal(supplyPrice), status,
                null, null, null, null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
