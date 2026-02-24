package com.hubble.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom constraint kiểm tra tag người dùng hợp lệ.
 * Tag phải có dạng: chỉ chứa chữ thường, số, dấu gạch dưới.
 * Ví dụ hợp lệ: "john_doe", "user123"
 */
@Documented
@Constraint(validatedBy = TagValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TagConstraint {

    String message() default "TAG_INVALID";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
