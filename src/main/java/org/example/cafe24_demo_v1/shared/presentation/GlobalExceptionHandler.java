package org.example.cafe24_demo_v1.shared.presentation;

import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Cafe24 API 호출 실패를 전역으로 처리하는 핸들러.
 * 각 컨트롤러에 중복 선언하던 @ExceptionHandler를 이곳에 통합한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Cafe24ApiException.class)
    public ResponseEntity<String> handleCafe24ApiException(Cafe24ApiException e) {
        log.error("Cafe24 API error: {}, body={}", e.getMessage(), e.getResponseBody());
        String body = "Cafe24 API 호출에 실패했습니다: " + e.getMessage();
        if (StringUtils.hasText(e.getResponseBody())) {
            body += "\nCafe24 응답: " + e.getResponseBody();
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }
}
