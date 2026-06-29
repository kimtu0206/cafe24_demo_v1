package org.example.cafe24_demo_v1.customergroup.application.service;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.customergroup.domain.model.CustomerGroup;
import org.example.cafe24_demo_v1.customergroup.domain.service.Cafe24CustomerGroupPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerGroupService {

    private final Cafe24CustomerGroupPort cafe24CustomerGroupPort;
    private final AppAuthorizationService authorizationService;

    public List<CustomerGroup> list(String mallId) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        return cafe24CustomerGroupPort.listCustomerGroups(mallId, credential);
    }
}
