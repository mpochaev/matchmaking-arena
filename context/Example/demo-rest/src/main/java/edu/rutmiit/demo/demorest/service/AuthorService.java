package edu.rutmiit.demo.demorest.service;

import edu.rutmiit.demo.booksapicontract.dto.*;
import edu.rutmiit.demo.booksapicontract.exception.ResourceNotFoundException;
import edu.rutmiit.demo.demorest.event.AuthorEventPublisher;
import edu.rutmiit.demo.demorest.storage.InMemoryStorage;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AuthorService {
    private final InMemoryStorage storage;
    private final BookService bookService;
    private final AuthorEventPublisher eventPublisher;

    public AuthorService(InMemoryStorage storage,
                         @Lazy BookService bookService,
                         AuthorEventPublisher eventPublisher) {
        this.storage = storage;
        this.bookService = bookService;
        this.eventPublisher = eventPublisher;
    }

    public PagedResponse<AuthorResponse> findAll(int page, int size) {
        List<AuthorResponse> all = storage.authors.values().stream()
                .sorted(Comparator.comparingLong(AuthorResponse::getId))
                .toList();
        int totalElements = all.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 1;
        int from = page * size;
        int to = Math.min(from + size, totalElements);
        List<AuthorResponse> content = (from >= totalElements) ? List.of() : all.subList(from, to);
        return new PagedResponse<>(content, page, size, totalElements, totalPages, page >= totalPages - 1);
    }

    public AuthorResponse findById(Long id) {
        return Optional.ofNullable(storage.authors.get(id))
                .orElseThrow(() -> new ResourceNotFoundException("Author", id));
    }

    public AuthorResponse create(AuthorRequest request) {
        long id = storage.authorSequence.incrementAndGet();
        String fullName = request.firstName() + " " + request.lastName();
        AuthorResponse author = AuthorResponse.builder()
                .id(id)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .fullName(fullName)
                .email(request.email())
                .bio(request.bio())
                .birthDate(request.birthDate())
                .nationality(request.nationality())
                .booksCount(0)
                .build();
        storage.authors.put(id, author);
        eventPublisher.publishCreated(author);
        return author;
    }

    public AuthorResponse update(Long id, AuthorRequest request) {
        AuthorResponse existing = findById(id); // Проверяем, что автор существует
        String fullName = request.firstName() + " " + request.lastName();
        AuthorResponse updatedAuthor = AuthorResponse.builder()
                .id(id)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .fullName(fullName)
                .email(request.email())
                .bio(request.bio())
                .birthDate(request.birthDate())
                .nationality(request.nationality())
                .booksCount(existing.getBooksCount())
                .build();
        storage.authors.put(id, updatedAuthor);
        return updatedAuthor;
    }

    public AuthorResponse patchAuthor(Long id, PatchAuthorRequest request) {
        AuthorResponse existing = findById(id);
        String newFirstName = request.firstName() != null ? request.firstName() : existing.getFirstName();
        String newLastName = request.lastName() != null ? request.lastName() : existing.getLastName();
        AuthorResponse updated = AuthorResponse.builder()
                .id(id)
                .firstName(newFirstName)
                .lastName(newLastName)
                .fullName(newFirstName + " " + newLastName)
                .email(request.email() != null ? request.email() : existing.getEmail())
                .bio(request.bio() != null ? request.bio() : existing.getBio())
                .birthDate(request.birthDate() != null ? request.birthDate() : existing.getBirthDate())
                .nationality(request.nationality() != null ? request.nationality() : existing.getNationality())
                .booksCount(existing.getBooksCount())
                .build();
        storage.authors.put(id, updated);
        return updated;
    }

    public void delete(Long id) {
        AuthorResponse author = findById(id);

        // Считаем книги до каскадного удаления — для события аудита
        int booksCount = (int) storage.books.values().stream()
                .filter(b -> b.getAuthor() != null && b.getAuthor().getId().equals(id))
                .count();

        bookService.deleteBooksByAuthorId(id);
        storage.authors.remove(id);
        eventPublisher.publishDeleted(author, booksCount);
    }
}
