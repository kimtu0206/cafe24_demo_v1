package org.example.cafe24_demo_v1.product.presentation;

import org.example.cafe24_demo_v1.product.application.service.ProductService;
import org.example.cafe24_demo_v1.product.domain.model.Product;
import org.example.cafe24_demo_v1.product.domain.model.ProductPage;
import org.example.cafe24_demo_v1.product.domain.model.ProductStatus;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProductService productService;
    @MockitoBean private Cafe24Properties cafe24Properties;

    @Test
    void 상품_목록을_조회하면_전체개수와_함께_반환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        Product product = Product.register(
                "mymall", 1L, "상품", new BigDecimal("1000"), new BigDecimal("500"), ProductStatus.ON_SALE,
                null, null, null, null, null, null, null
        );
        given(productService.list("mymall", 0, 20)).willReturn(new ProductPage(List.of(product), 1L));

        mockMvc.perform(get("/products").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.products[0].productNo").value(1));
    }

    @Test
    void 상품_목록_조회는_page_size_기본값을_사용한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        given(productService.list("mymall", 0, 20)).willReturn(new ProductPage(List.of(), 0L));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void 상품명이_비어있으면_등록을_거부한다() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\": \"\", \"price\": 1000, \"supplyPrice\": 500}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 가격이_음수면_등록을_거부한다() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\": \"상품\", \"price\": -1, \"supplyPrice\": 500}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 가격이_없으면_등록을_거부한다() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\": \"상품\", \"supplyPrice\": 500}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 유효한_요청이면_상품을_등록한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        Product product = Product.register(
                "mymall", 1L, "상품", new BigDecimal("1000"), new BigDecimal("500"), ProductStatus.ON_SALE,
                null, null, null, null, null, null, null
        );
        given(productService.register(any())).willReturn(product);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\": \"상품\", \"price\": 1000, \"supplyPrice\": 500}"))
                .andExpect(status().isCreated());
    }

    @Test
    void 수정_요청도_상품명이_비어있으면_거부한다() throws Exception {
        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\": \"\", \"price\": 1000, \"supplyPrice\": 500}"))
                .andExpect(status().isBadRequest());
    }
}
