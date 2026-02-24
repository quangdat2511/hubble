package com.hubble.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    // Lỗi hệ thống
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Message key không hợp lệ", HttpStatus.BAD_REQUEST),

    // Người dùng
    USER_EXISTED(1002, "Email hoặc tag đã tồn tại", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    USERNAME_INVALID(1004, "Tên hiển thị phải có ít nhất {min} ký tự", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1005, "Mật khẩu phải có ít nhất {min} ký tự", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1006, "Email không hợp lệ", HttpStatus.BAD_REQUEST),

    // Xác thực
    UNAUTHENTICATED(1007, "Chưa đăng nhập", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1008, "Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(1009, "Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),

    // Bạn bè
    FRIEND_REQUEST_EXISTED(2001, "Lời mời kết bạn đã tồn tại", HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_NOT_FOUND(2002, "Lời mời kết bạn không tồn tại", HttpStatus.NOT_FOUND),
    ALREADY_FRIENDS(2003, "Hai người đã là bạn bè", HttpStatus.BAD_REQUEST),

    // Server / Kênh
    SERVER_NOT_FOUND(3001, "Server không tồn tại", HttpStatus.NOT_FOUND),
    CHANNEL_NOT_FOUND(3002, "Kênh không tồn tại", HttpStatus.NOT_FOUND),
    INVITE_CODE_INVALID(3003, "Link mời không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),

    // Tin nhắn
    MESSAGE_NOT_FOUND(4001, "Tin nhắn không tồn tại", HttpStatus.NOT_FOUND),
    MESSAGE_NOT_OWNER(4002, "Bạn không phải chủ tin nhắn này", HttpStatus.FORBIDDEN);

    final int code;
    final String message;
    final HttpStatusCode httpStatusCode;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}
