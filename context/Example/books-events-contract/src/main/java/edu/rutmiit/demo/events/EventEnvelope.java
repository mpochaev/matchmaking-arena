package edu.rutmiit.demo.events;

/**
 * Обёртка (конверт) для любого доменного события.
 *
 * Промышленный паттерн: отделяем метаданные доставки от бизнес-содержимого.
 * Это позволяет инфраструктурному коду (логирование, дедупликация, маршрутизация)
 * работать с метаданными, не зная ничего о payload.
 *
 * Аналогия с почтой: EventMetadata — это конверт с адресом и штампом,
 * а payload — письмо внутри. Почтальону не нужно читать письмо,
 * чтобы доставить его.
 *
 * @param <T> тип полезной нагрузки (BookEvent.Created, AuthorEvent.Deleted и т.д.)
 */
public record EventEnvelope<T>(
        EventMetadata metadata,
        T payload
) {
    /**
     * Фабричный метод — оборачивает payload в конверт с автоматически
     * заполненными метаданными (eventId, timestamp).
     */
    public static <T> EventEnvelope<T> wrap(T payload, String source, String eventType) {
        return new EventEnvelope<>(
                EventMetadata.create(source, eventType),
                payload
        );
    }
}
