package com.example.readingrewards.integration.reward;

import com.example.readingrewards.external.OpenLibraryClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenLibraryProgressIntegrationTest {

    @Test
    void usesOpenLibraryPageCountWhenAvailable() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        String url = "https://openlibrary.org/works/OL82563W.json";
        when(restTemplate.getForObject(url, Map.class))
            .thenReturn(Map.of("number_of_pages_median", 288));

        OpenLibraryClient client = new OpenLibraryClient(restTemplate);
        OptionalInt pages = client.fetchPageCountByBookId("works/OL82563W");

        assertThat(pages).isPresent();
        assertThat(pages.getAsInt()).isEqualTo(288);
    }

    @Test
    void fallsBackToManualOverrideWhenOpenLibraryUnavailable() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        String url = "https://openlibrary.org/works/OL82563W.json";
        when(restTemplate.getForObject(url, Map.class))
            .thenThrow(new RestClientException("offline"));

        OpenLibraryClient client = new OpenLibraryClient(restTemplate);
        OptionalInt pages = client.suggestedPageCount("works/OL82563W", 250);

        assertThat(pages).isPresent();
        assertThat(pages.getAsInt()).isEqualTo(250);
    }
}
