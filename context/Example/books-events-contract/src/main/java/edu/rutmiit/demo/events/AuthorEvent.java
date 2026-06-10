package edu.rutmiit.demo.events;

/**
 * Семейство событий, связанных с авторами.
 *
 * Аналогично BookEvent — sealed interface гарантирует полный перечень вариантов.
 * Десериализация по eventType, а не через Jackson-аннотации.
 */
public sealed interface AuthorEvent {

    /**
     * Автор создан. Содержит основные атрибуты нового автора.
     */
    record Created(
            Long authorId,
            String firstName,
            String lastName,
            String fullName,
            String nationality
    ) implements AuthorEvent {}

    /**
     * Автор удалён. В нашей системе удаление каскадное — вместе с книгами.
     */
    record Deleted(
            Long authorId,
            String fullName,
            int deletedBooksCount
    ) implements AuthorEvent {}
}
