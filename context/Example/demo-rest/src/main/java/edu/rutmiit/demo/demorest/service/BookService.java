package edu.rutmiit.demo.demorest.service;

import edu.rutmiit.demo.booksapicontract.dto.*;
import edu.rutmiit.demo.booksapicontract.exception.IsbnAlreadyExistsException;
import edu.rutmiit.demo.booksapicontract.exception.ResourceNotFoundException;
import edu.rutmiit.demo.demorest.event.BookEventPublisher;
import edu.rutmiit.demo.demorest.storage.InMemoryStorage;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class BookService {

    private final InMemoryStorage storage;
    private final AuthorService authorService;
    private final BookEventPublisher eventPublisher;

    public BookService(InMemoryStorage storage,
                       @Lazy AuthorService authorService,
                       BookEventPublisher eventPublisher) {
        this.storage = storage;
        this.authorService = authorService;
        this.eventPublisher = eventPublisher;
    }

    public BookResponse findBookById(Long id) {
        return Optional.ofNullable(storage.books.get(id))
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
    }

    public PagedResponse<BookResponse> findAllBooks(Long authorId, String genre, Integer publishedYear,
                                                    String titleSearch, int page, int size) {
        Stream<BookResponse> stream = storage.books.values().stream()
                .sorted((b1, b2) -> b1.getId().compareTo(b2.getId()));

        if (authorId != null) {
            stream = stream.filter(b -> b.getAuthor() != null && b.getAuthor().getId().equals(authorId));
        }
        if (genre != null && !genre.isBlank()) {
            stream = stream.filter(b -> genre.equalsIgnoreCase(b.getGenre()));
        }
        if (publishedYear != null) {
            stream = stream.filter(b -> publishedYear.equals(b.getPublishedYear()));
        }
        if (titleSearch != null && !titleSearch.isBlank()) {
            String q = titleSearch.toLowerCase();
            stream = stream.filter(b -> b.getTitle() != null && b.getTitle().toLowerCase().contains(q));
        }

        List<BookResponse> allBooks = stream.toList();
        int totalElements = allBooks.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 1;
        int from = page * size;
        int to = Math.min(from + size, totalElements);
        List<BookResponse> content = (from >= totalElements) ? List.of() : allBooks.subList(from, to);
        return new PagedResponse<>(content, page, size, totalElements, totalPages, page >= totalPages - 1);
    }

    public BookResponse createBook(BookRequest request) {
        validateIsbn(request.isbn(), null);
        AuthorResponse author = authorService.findById(request.authorId());

        long id = storage.bookSequence.incrementAndGet();
        BookResponse book = BookResponse.builder()
                .id(id)
                .title(request.title())
                .isbn(request.isbn())
                .author(author)
                .description(request.description())
                .genre(request.genre())
                .publishedYear(request.publishedYear())
                .language(request.language())
                .createdAt(LocalDateTime.now())
                .build();
        storage.books.put(id, book);

        // Публикуем доменное событие ПОСЛЕ успешного сохранения.
        // Если RabbitMQ недоступен — книга всё равно создана, событие просто потеряется.
        eventPublisher.publishCreated(book);

        return book;
    }

    public BookResponse updateBook(Long id, UpdateBookRequest request) {
        BookResponse existing = findBookById(id);
        validateIsbn(request.isbn(), id);

        BookResponse updated = BookResponse.builder()
                .id(id)
                .title(request.title())
                .isbn(request.isbn())
                .author(existing.getAuthor())
                .description(request.description())
                .genre(request.genre())
                .publishedYear(request.publishedYear())
                .language(request.language())
                .createdAt(existing.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        storage.books.put(id, updated);
        eventPublisher.publishUpdated(updated);
        return updated;
    }

    public BookResponse patchBook(Long id, PatchBookRequest request) {
        BookResponse existing = findBookById(id);

        // ISBN меняется — проверяем уникальность, исключая текущую книгу
        if (request.isbn() != null && !request.isbn().equalsIgnoreCase(existing.getIsbn())) {
            validateIsbn(request.isbn(), id);
        }

        BookResponse updated = BookResponse.builder()
                .id(id)
                .title(request.title() != null ? request.title() : existing.getTitle())
                .isbn(request.isbn() != null ? request.isbn() : existing.getIsbn())
                .author(existing.getAuthor())
                .description(request.description() != null ? request.description() : existing.getDescription())
                .genre(request.genre() != null ? request.genre() : existing.getGenre())
                .publishedYear(request.publishedYear() != null ? request.publishedYear() : existing.getPublishedYear())
                .language(request.language() != null ? request.language() : existing.getLanguage())
                .createdAt(existing.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        storage.books.put(id, updated);
        eventPublisher.publishUpdated(updated);
        return updated;
    }

    public void deleteBook(Long id) {
        BookResponse book = findBookById(id);
        storage.books.remove(id);
        eventPublisher.publishDeleted(id, book.getTitle());
    }

    public void deleteBooksByAuthorId(Long authorId) {
        List<Long> toDelete = storage.books.values().stream()
                .filter(b -> b.getAuthor() != null && b.getAuthor().getId().equals(authorId))
                .map(BookResponse::getId)
                .toList();
        toDelete.forEach(storage.books::remove);
    }

    private void validateIsbn(String isbn, Long currentBookId) {
        storage.books.values().stream()
                .filter(b -> b.getIsbn().equalsIgnoreCase(isbn))
                .filter(b -> !b.getId().equals(currentBookId))
                .findAny()
                .ifPresent(b -> { throw new IsbnAlreadyExistsException(isbn); });
    }
}
