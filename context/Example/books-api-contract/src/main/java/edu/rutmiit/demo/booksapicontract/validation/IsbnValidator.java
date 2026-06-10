package edu.rutmiit.demo.booksapicontract.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Проверяет, что строка является корректным ISBN-10 или ISBN-13.
 *
 * Дефисы и пробелы перед проверкой убираются.
 * ISBN-10: 9 цифр + цифра или X на конце.
 * ISBN-13: начинается с 978 или 979, 13 цифр.
 *
 * Алгоритмическая проверка контрольной цифры (алгоритм Луна) намеренно не выполняется:
 * валидатор должен быть быстрым, а точность важна на уровне интеграционных тестов.
 */
public class IsbnValidator implements ConstraintValidator<ValidIsbn, String> {

    private static final Pattern ISBN_10 = Pattern.compile("^\\d{9}[\\dX]$");
    private static final Pattern ISBN_13 = Pattern.compile("^97[89]\\d{10}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null и пустую строку пропускаем — за них отвечает @NotBlank
        if (value == null || value.isBlank()) {
            return true;
        }
        // Убираем дефисы и пробелы перед проверкой
        String cleaned = value.replaceAll("[\\s\\-]", "");
        return ISBN_10.matcher(cleaned).matches() || ISBN_13.matcher(cleaned).matches();
    }
}
