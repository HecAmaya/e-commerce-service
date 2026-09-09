package com.example.ecommerce.common;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {
    private static final Pattern HTML_TAG = Pattern.compile("<\\s*/?\\s*[a-z][^>]*>", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || !HTML_TAG.matcher(value).find();
    }
}
