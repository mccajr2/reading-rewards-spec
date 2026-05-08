package com.example.readingrewards.domain.service;

import com.example.readingrewards.domain.dto.BookSummaryDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GoogleBooksService {

    private static final String API_URL = "https://www.googleapis.com/books/v1/volumes";

    private final RestTemplate restTemplate;

    public GoogleBooksService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public List<BookSummaryDto> search(String title, String author, String isbn) {
        if (isBlank(title) && isBlank(author) && isBlank(isbn)) {
            return Collections.emptyList();
        }

        StringBuilder q = new StringBuilder();
        if (!isBlank(title))  appendPart(q, "intitle:" + title.replace(" ", "+"));
        if (!isBlank(author)) appendPart(q, "inauthor:" + author.replace(" ", "+"));
        if (!isBlank(isbn))   appendPart(q, "isbn:" + isbn.replace("-", ""));

        String url = API_URL + "?q=" + q + "&maxResults=20";
        Map<String, Object> result = restTemplate.getForObject(url, Map.class);
        if (result == null || !(result.get("items") instanceof List<?> items)) {
            return Collections.emptyList();
        }

        List<BookSummaryDto> books = new ArrayList<>();
        for (Object itemObj : items) {
            if (!(itemObj instanceof Map<?, ?> item)) continue;
            String volumeId = item.get("id") != null ? item.get("id").toString() : null;
            if (!(item.get("volumeInfo") instanceof Map<?, ?> info)) continue;

            String titleVal = info.get("title") != null ? info.get("title").toString() : null;
            String description = info.get("description") != null ? info.get("description").toString() : null;

            List<String> authors = new ArrayList<>();
            if (info.get("authors") instanceof List<?> al) {
                for (Object a : al) { if (a != null) authors.add(a.toString()); }
            }

            String thumbnailUrl = null;
            if (info.get("imageLinks") instanceof Map<?, ?> links && links.get("thumbnail") != null) {
                thumbnailUrl = links.get("thumbnail").toString();
            }

            books.add(new BookSummaryDto(volumeId, titleVal, authors, description, thumbnailUrl));
        }
        return books;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static void appendPart(StringBuilder sb, String part) {
        if (!sb.isEmpty()) sb.append("+");
        sb.append(part);
    }
}
