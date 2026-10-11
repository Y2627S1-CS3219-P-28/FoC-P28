package com.p28.userservice.logic;

import java.util.UUID;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;


@Component
public class CreditServiceClient {
    private final RestClient restClient;
    private record RegistrationFactRequest(
            UUID eventId,
            String userId,
            Instant occurredAt
    ) {}

    public CreditServiceClient(
            RestClient.Builder builder,
            @Value("${credit-service.url}") String creditServiceUrl) {
        
        
        this.restClient = builder
                .baseUrl(creditServiceUrl)
                .build();
    }

    public void registerUser(String token, String userId, UUID eventId) {
        RegistrationFactRequest request = new RegistrationFactRequest(
            eventId,
            userId,
            Instant.now()
        );

        restClient.post()
                .uri("/api/credits/registration-facts")
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + token
                )
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
