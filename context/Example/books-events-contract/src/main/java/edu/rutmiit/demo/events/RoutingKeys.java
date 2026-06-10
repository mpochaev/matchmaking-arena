package edu.rutmiit.demo.events;

/**
 * Константы для маршрутизации событий в RabbitMQ.
 *
 * Routing key в topic exchange работает как почтовый индекс:
 * - "book.created" — конкретное событие
 * - "book.*"       — все события книг
 * - "#"            — все события вообще
 *
 * Вынесены в контракт, чтобы publisher и consumer использовали одни и те же строки.
 * Рассогласование routing key — частая ошибка, которую трудно отследить.
 */
public final class RoutingKeys {

    private RoutingKeys() {
        // утилитарный класс — экземпляры не создаём
    }

    // Имя общего topic exchange для доменных событий
    public static final String EXCHANGE = "books.events";

    // Routing keys для событий книг
    public static final String BOOK_CREATED = "book.created";
    public static final String BOOK_UPDATED = "book.updated";
    public static final String BOOK_DELETED = "book.deleted";
    public static final String BOOK_ENRICHED = "book.enriched";

    // Routing keys для событий авторов
    public static final String AUTHOR_CREATED = "author.created";
    public static final String AUTHOR_DELETED = "author.deleted";

    // Паттерны для подписки (wildcard)
    public static final String ALL_BOOK_EVENTS = "book.*";
    public static final String ALL_AUTHOR_EVENTS = "author.*";
    public static final String ALL_EVENTS = "#";
}
