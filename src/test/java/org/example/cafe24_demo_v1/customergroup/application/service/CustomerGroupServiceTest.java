package org.example.cafe24_demo_v1.customergroup.application.service;

import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.customergroup.domain.model.CustomerGroup;
import org.example.cafe24_demo_v1.customergroup.domain.service.Cafe24CustomerGroupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomerGroupServiceTest {

    @Mock private Cafe24CustomerGroupPort cafe24CustomerGroupPort;
    @Mock private AppAuthorizationService authorizationService;

    private CustomerGroupService customerGroupService;

    private final TokenCredential credential = new TokenCredential(
            "access-token", "refresh-token", "Bearer",
            LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1)
    );

    @BeforeEach
    void setUp() {
        customerGroupService = new CustomerGroupService(cafe24CustomerGroupPort, authorizationService);
    }

    @Test
    void list는_getValidCredential_후_port에_위임한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        CustomerGroup group = new CustomerGroup(1, "일반회원", "기본 등급", "T", "T");
        given(cafe24CustomerGroupPort.listCustomerGroups("mymall", credential)).willReturn(List.of(group));

        List<CustomerGroup> result = customerGroupService.list("mymall");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGroupName()).isEqualTo("일반회원");

        InOrder inOrder = Mockito.inOrder(authorizationService, cafe24CustomerGroupPort);
        inOrder.verify(authorizationService).getValidCredential("mymall");
        inOrder.verify(cafe24CustomerGroupPort).listCustomerGroups("mymall", credential);
    }

    @Test
    void list는_빈_결과를_그대로_반환한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24CustomerGroupPort.listCustomerGroups("mymall", credential)).willReturn(List.of());

        List<CustomerGroup> result = customerGroupService.list("mymall");

        assertThat(result).isEmpty();
    }
}
