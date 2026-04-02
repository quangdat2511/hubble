package com.hubble.exception;

import com.hubble.dto.common.ApiResponse;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.Objects;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    // Bắt mọi lỗi không xác định
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleRuntimeException(Exception e) {
        log.error("Uncategorized Exception: ", e);
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    // Bắt lỗi nghiệp vụ do ứng dụng ném ra
    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<Void>> handleAppException(AppException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    // Bắt lỗi không có quyền truy cập
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("Data integrity violation: ", e);

        String detail = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : e.getMessage();

        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        if (detail != null) {
            if (detail.contains("users_phone_key")) {
                errorCode = ErrorCode.PHONE_EXISTED;
            } else if (detail.contains("users_email_key") || detail.contains("users_username_key")) {
                errorCode = ErrorCode.USER_EXISTED;
            }
        }

        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity.status(errorCode.getHttpStatusCode())
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    // Bắt lỗi validation (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)    ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String enumKey = e.getFieldError().getDefaultMessage();
        ErrorCode errorCode;
        Map<String, Object> attributes = null;

        try {
            errorCode = ErrorCode.valueOf(enumKey);
            var constraintViolation = e.getBindingResult()
                    .getAllErrors()
                    .getFirst()
                    .unwrap(ConstraintViolation.class);
            attributes = constraintViolation.getConstraintDescriptor().getAttributes();
            log.info("Validation attributes: {}", attributes);
        } catch (IllegalArgumentException ex) {
            errorCode = ErrorCode.INVALID_KEY;
        }

        String message = Objects.nonNull(attributes)
                ? mapAttribute(errorCode.getMessage(), attributes)
                : errorCode.getMessage();

        return ResponseEntity.badRequest()
                .body(ApiResponse.<Void>builder()
                        .code(errorCode.getCode())
                        .message(message)
                        .build());
    }

    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
