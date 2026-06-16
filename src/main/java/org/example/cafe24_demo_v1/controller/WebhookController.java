package org.example.cafe24_demo_v1.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.config.Cafe24Properties;
import org.example.cafe24_demo_v1.dto.WebhookRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final Cafe24Properties cafe24Properties;

    @PostMapping("/cafe24")
    public ResponseEntity<Void> receive(
            @RequestBody String body,
            @RequestHeader Map<String, String> headers) {

        System.out.println("===== WEBHOOK =====");
        System.out.println(headers);
        System.out.println(body);

        return ResponseEntity.ok().build();
    }
}
