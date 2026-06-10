package edu.rutmiit.demo.demorest.storage;

import edu.rutmiit.demo.booksapicontract.dto.AuthorResponse;
import edu.rutmiit.demo.booksapicontract.dto.BookResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryStorage {
    public final Map<Long, AuthorResponse> authors = new ConcurrentHashMap<>();
    public final Map<Long, BookResponse> books = new ConcurrentHashMap<>();

    public final AtomicLong authorSequence = new AtomicLong(0);
    public final AtomicLong bookSequence = new AtomicLong(0);

    @PostConstruct
    public void init() {
        // Создаем авторов с полным набором полей (демонстрация Lombok @Builder)
        AuthorResponse author1 = AuthorResponse.builder()
                .id(authorSequence.incrementAndGet())
                .firstName("Лев")
                .lastName("Толстой")
                .fullName("Лев Толстой")
                .nationality("Русский")
                .birthDate(LocalDate.of(1828, 9, 9))
                .bio("Русский писатель, один из наиболее известных авторов в мировой литературе.")
                .booksCount(2)
                .build();

        AuthorResponse author2 = AuthorResponse.builder()
                .id(authorSequence.incrementAndGet())
                .firstName("Фёдор")
                .lastName("Достоевский")
                .fullName("Фёдор Достоевский")
                .nationality("Русский")
                .birthDate(LocalDate.of(1821, 11, 11))
                .bio("Русский писатель, мыслитель, философ и публицист.")
                .booksCount(1)
                .build();

        authors.put(author1.getId(), author1);
        authors.put(author2.getId(), author2);

        // Создаем книги с полным набором полей
        long bookId1 = bookSequence.incrementAndGet();
        books.put(bookId1, BookResponse.builder()
                .id(bookId1)
                .title("Война и мир")
                .isbn("9785389062598")
                .author(author1)
                .genre("Исторический роман")
                .publishedYear(1869)
                .language("ru")
                .description("Эпический роман-хроника, описывающий русское общество эпохи войн против Наполеона.")
                .createdAt(LocalDateTime.now())
                .build());

        long bookId2 = bookSequence.incrementAndGet();
        books.put(bookId2, BookResponse.builder()
                .id(bookId2)
                .title("Преступление и наказание")
                .isbn("9785389062599")
                .author(author2)
                .genre("Психологический роман")
                .publishedYear(1866)
                .language("ru")
                .description("Роман о моральной дилемме студента Раскольникова.")
                .createdAt(LocalDateTime.now())
                .build());

        long bookId3 = bookSequence.incrementAndGet();
        books.put(bookId3, BookResponse.builder()
                .id(bookId3)
                .title("Анна Каренина")
                .isbn("9785389062597")
                .author(author1)
                .genre("Роман")
                .publishedYear(1877)
                .language("ru")
                .description("Роман о трагической судьбе замужней дамы высшего общества.")
                .createdAt(LocalDateTime.now())
                .build());
    }
}