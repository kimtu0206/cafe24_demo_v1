package org.example.cafe24_demo_v1.shared.exception;

import org.springframework.http.HttpStatusCode;

/**
 * Cafe24 Admin/OAuth API 호출이 실패했을 때 던지는 공통 예외.
 *
 * HTTP 상태 코드와 Cafe24가 반환한 응답 본문(에러 메시지)을 함께 보존해
 * 호출부나 로그에서 실패 원인을 추적할 수 있게 한다.
 */
public class Cafe24ApiException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String responseBody;

    public Cafe24ApiException(String message, HttpStatusCode statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
