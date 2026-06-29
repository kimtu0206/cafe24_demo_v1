package org.example.cafe24_demo_v1.customergroup.domain.service;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.customergroup.domain.model.CustomerGroup;

import java.util.List;

public interface Cafe24CustomerGroupPort {

    List<CustomerGroup> listCustomerGroups(String mallId, TokenCredential credential);
}
