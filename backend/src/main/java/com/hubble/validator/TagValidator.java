package com.hubble.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Logic kiểm tra cho @TagConstraint.
 * Tag hợp lệ: chỉ gồm chữ thường (a-z), số (0-9), dấu gạch dưới (_).
 * Độ dài từ 3 đến 32 ký tự.
 */
public class TagValidator implements ConstraintValidator<TagConstraint, String> {

    private static final String TAG_PATTERN = "^[a-z0-9_]{3,32}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Cho phép null (kết hợp với @NotNull ở nơi khác nếu cần)
        if (value == null) return true;
        return value.matches(TAG_PATTERN);
    }
}
