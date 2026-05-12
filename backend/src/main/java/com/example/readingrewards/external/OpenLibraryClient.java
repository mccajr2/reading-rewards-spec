package com.example.readingrewards.external;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

@Component
public class OpenLibraryClient {

    private static final String WORKS_API_URL = "https://openlibrary.org/works/%s.json";

    private final RestTemplate restTemplate;

    public OpenLibraryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public OptionalInt fetchPageCountByBookId(String providerBookId) {
        if (providerBookId == null || providerBookId.isBlank()) {
            return OptionalInt.empty();
        }

        String normalizedId = normalizeWorkId(providerBookId);
        if (normalizedId.isBlank()) {
            return OptionalInt.empty();
        }

        try {
            String url = Objects.requireNonNull(String.format(WORKS_API_URL, normalizedId));
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = restTemplate.getForObject(
                url,
                Map.class
            );

            if (payload == null) {
                return OptionalInt.empty();
            }

            Integer medianPages = asPositiveInt(payload.get("number_of_pages_median"));
            if (medianPages != null) {
                return OptionalInt.of(medianPages);
            }

            Integer pages = asPositiveInt(payload.get("number_of_pages"));
            if (pages != null) {
                return OptionalInt.of(pages);
            }

            return OptionalInt.empty();
        } catch (RestClientException ex) {
            return OptionalInt.empty();
        }
    }

    public OptionalInt suggestedPageCount(String providerBookId, Integer manualOverride) {
        Integer manual = asPositiveInt(manualOverride);
        if (manual != null) {
            return OptionalInt.of(manual);
        }
        return fetchPageCountByBookId(providerBookId);
    }

    private static String normalizeWorkId(String providerBookId) {
        String normalized = providerBookId.trim().replace("/works/", "");
        if (normalized.startsWith("works/")) {
            normalized = normalized.substring("works/".length());
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static Integer asPositiveInt(Object value) {
        if (value instanceof Number number) {
            int parsed = number.intValue();
            return parsed > 0 ? parsed : null;
        }
        if (value instanceof Integer integer) {
            return integer > 0 ? integer : null;
        }
        return null;
    }
}
