package org.example.cafe24_demo_v1.customergroup.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CustomerGroup {
    private final Integer groupNo;
    private final String groupName;
    private final String groupDescription;
    private final String isBuyer;
    private final String isPrimary;
}
