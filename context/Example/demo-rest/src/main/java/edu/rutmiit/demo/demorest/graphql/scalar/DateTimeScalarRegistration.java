package edu.rutmiit.demo.demorest.graphql.scalar;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsRuntimeWiring;
import graphql.scalars.ExtendedScalars;
import graphql.schema.idl.RuntimeWiring;

/**
 * Регистрация пользовательских скалярных типов для GraphQL.
 *
 * В GraphQL по умолчанию есть только пять скалярных типов:
 * String, Int, Float, Boolean, ID. Для дат и времени нужно регистрировать
 * дополнительные скаляры из библиотеки graphql-java-extended-scalars.
 *
 * DateTime маппится на java.time.OffsetDateTime / LocalDateTime.
 * Date маппится на java.time.LocalDate.
 *
 * Аннотация @DgsRuntimeWiring позволяет программно настроить runtime wiring —
 * механизм GraphQL-Java для связывания схемы с реализацией.
 */
@DgsComponent
public class DateTimeScalarRegistration {

    /**
     * Регистрируем скаляры DateTime и Date в runtime wiring.
     *
     * ExtendedScalars — это коллекция готовых скаляров от graphql-java.
     * Они уже содержат логику сериализации/десериализации, нам достаточно
     * лишь зарегистрировать их.
     */
    @DgsRuntimeWiring
    public RuntimeWiring.Builder addScalars(RuntimeWiring.Builder builder) {
        return builder
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.Date);
    }
}
