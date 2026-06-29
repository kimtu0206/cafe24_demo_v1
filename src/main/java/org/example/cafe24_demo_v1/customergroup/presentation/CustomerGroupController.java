package org.example.cafe24_demo_v1.customergroup.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.customergroup.application.service.CustomerGroupService;
import org.example.cafe24_demo_v1.customergroup.domain.model.CustomerGroup;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "CustomerGroups", description = "회원 등급 목록 조회 — Cafe24 Admin API 연동 (실패 시 502 반환)")
@Slf4j
@RestController
@RequestMapping("/customergroups")
@RequiredArgsConstructor
public class CustomerGroupController {

    private final CustomerGroupService customerGroupService;
    private final Cafe24Properties cafe24Properties;

    @Operation(
            summary = "회원 등급 목록 조회",
            description = "Cafe24 쇼핑몰의 회원 등급 목록을 조회합니다. 혜택 생성 시 customerGroupList에 사용할 group_no를 확인할 수 있습니다."
    )
    @GetMapping
    public ResponseEntity<?> list() {
        List<CustomerGroup> groups = customerGroupService.list(cafe24Properties.getMallId());
        List<CustomerGroupResponse> response = groups.stream().map(CustomerGroupResponse::from).toList();
        return ResponseEntity.ok(new CustomerGroupListResponse(response, response.size()));
    }

    @ExceptionHandler(Cafe24ApiException.class)
    public ResponseEntity<String> handleCafe24ApiException(Cafe24ApiException e) {
        log.error("Cafe24 customergroups API error: {}, body={}", e.getMessage(), e.getResponseBody());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("Cafe24 API 호출에 실패했습니다: " + e.getMessage() + "\nCafe24 응답: " + e.getResponseBody());
    }

    private record CustomerGroupResponse(
            Integer groupNo,
            String groupName,
            String groupDescription,
            String isBuyer,
            String isPrimary
    ) {
        static CustomerGroupResponse from(CustomerGroup group) {
            return new CustomerGroupResponse(
                    group.getGroupNo(),
                    group.getGroupName(),
                    group.getGroupDescription(),
                    group.getIsBuyer(),
                    group.getIsPrimary()
            );
        }
    }

    private record CustomerGroupListResponse(List<CustomerGroupResponse> customerGroups, int count) {}
}
