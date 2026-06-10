package edu.rutmiit.demo.booksapicontract.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Аннотация для валидации ISBN-10 и ISBN-13.
 *
 * Дефисы и пробелы в ISBN допустимы — они игнорируются при проверке.
 * null и пустая строка считаются корректными: за обязательность отвечает @NotBlank.
 *
 * Примеры валидных значений:
 *   9785389062598      — ISBN-13 без разделителей
 *   978-5-389-06259-8  — ISBN-13 с дефисами
 *   0306406152         — ISBN-10
 */
@Documented
@Constraint(validatedBy = IsbnValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIsbn {

    String message() default "Некорректный ISBN. Допустимые форматы: ISBN-10 (10 цифр или 9 цифр + X) "
            + "или ISBN-13 (начинается с 978/979, 13 цифр)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
