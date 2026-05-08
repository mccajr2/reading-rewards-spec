package com.example.readingrewards.domain.repo;

import com.example.readingrewards.domain.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, String> {
}
