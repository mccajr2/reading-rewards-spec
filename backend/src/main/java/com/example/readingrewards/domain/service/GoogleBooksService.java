package com.example.readingrewards.domain.service;

import com.example.readingrewards.domain.dto.BookSummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Book search service using Open Library API (free, no key required).
 * Open Library provides generous free tier suitable for personal/small-scale apps.
 */
@Service
public class GoogleBooksService {

    private static final Logger log = LoggerFactory.getLogger(GoogleBooksService.class);
    private static final String API_URL = "https://openlibrary.org/search.json";
    private static final String COVERS_URL = "https://covers.openlibrary.org/b/id";

    private final RestTemplate restTemplate;

    public GoogleBooksService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public List<BookSummaryDto> search(String title, String author, String isbn) {
        if (isBlank(title) && isBlank(author) && isBlank(isbn)) {
            return Collections.emptyList();
        }

        StringBuilder urlBuilder = new StringBuilder(API_URL + "?");
        if (!isBlank(title))  urlBuilder.append("title=").append(encodeParam(title)).append("&");
        if (!isBlank(author)) urlBuilder.append("author=").append(encodeParam(author)).append("&");
        if (!isBlank(isbn))   urlBuilder.append("isbn=").append(encodeParam(isbn.replace("-", ""))).append("&");
        urlBuilder.append("limit=20");

        Map<String, Object> result;
        try {
            result = restTemplate.getForObject(urlBuilder.toString(), Map.class);
        } catch (RestClientException e) {
            log.warn("Open Library API unavailable: {}", e.getMessage());
            return Collections.emptyList();
        }

        if (result == null || !(result.get("docs") instanceof List<?> docs)) {
            return Collections.emptyList();
        }

        List<BookSummaryDto> books = new ArrayList<>();
        for (Object docObj : docs) {
            if (!(docObj instanceof Map<?, ?> doc)) continue;

            // Open Library keys start with "/" (e.g., "/works/OL15936512W"), strip it for clean URLs
            String rawKey = doc.get("key") != null ? doc.get("key").toString() : null;
            String volumeId = normalizeProviderBookId(rawKey);
            String titleVal = doc.get("title") != null ? doc.get("title").toString() : null;

            List<String> authors = new ArrayList<>();
            if (doc.get("author_name") instanceof List<?> al) {
                for (Object a : al) { if (a != null) authors.add(a.toString()); }
            }

            // Open Library doesn't provide descriptions directly, omit for now
            String description = null;

            // Thumbnail from cover ID
            String thumbnailUrl = null;
            if (doc.get("cover_i") instanceof Number coverId) {
                thumbnailUrl = COVERS_URL + "/" + coverId + "-M.jpg";
            }

            if (titleVal != null) {
                books.add(new BookSummaryDto(volumeId, titleVal, authors, description, thumbnailUrl));
            }
        }
        return books;
    }

    private static String encodeParam(String s) {
        return s.replace(" ", "+").replace("&", "%26");
    }

    private static String normalizeProviderBookId(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return null;
        }
        String cleaned = rawKey.replaceFirst("^/", "");
        int slash = cleaned.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < cleaned.length()) {
            return cleaned.substring(slash + 1);
        }
        return cleaned;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
