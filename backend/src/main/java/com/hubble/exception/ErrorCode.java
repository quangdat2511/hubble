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
    USER_EXISTED(1002, "Email hoặc username đã tồn tại", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    USERNAME_INVALID(1004, "Tên hiển thị phải có ít nhất {min} ký tự", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1005, "Mật khẩu phải có ít nhất {min} ký tự", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1006, "Email không hợp lệ", HttpStatus.BAD_REQUEST),
    CANNOT_DM_SELF(1007, "Bạn không thể tạo kênh DM với chính mình", HttpStatus.BAD_REQUEST),
    // Xác thực
    UNAUTHENTICATED(1007, "Chưa đăng nhập", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1008, "Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(1009, "Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(1010, "Email hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    PHONE_EXISTED(1011, "Số điện thoại đã được sử dụng", HttpStatus.BAD_REQUEST),
    INVALID_OTP(1012, "Mã OTP không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1013, "Mã OTP đã hết hạn", HttpStatus.BAD_REQUEST),
    GOOGLE_AUTH_FAILED(1014, "Xác thực Google thất bại", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID(1015, "Refresh token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    PASSWORDS_NOT_MATCH(1016, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),

    // Bạn bè
    FRIEND_REQUEST_EXISTED(2001, "Lời mời kết bạn đã tồn tại", HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_NOT_FOUND(2002, "Lời mời kết bạn không tồn tại", HttpStatus.NOT_FOUND),
    ALREADY_FRIENDS(2003, "Hai người đã là bạn bè", HttpStatus.BAD_REQUEST),
    CANNOT_FRIEND_SELF(2004, "Bạn không thể kết bạn với chính mình", HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_FORBIDDEN(2005, "Không thể gửi lời mời do quan hệ chặn", HttpStatus.BAD_REQUEST),

    // Server / Kênh
    SERVER_NOT_FOUND(3001, "Server không tồn tại", HttpStatus.NOT_FOUND),
    CHANNEL_NOT_FOUND(3002, "Kênh không tồn tại", HttpStatus.NOT_FOUND),
    INVITE_CODE_INVALID(3003, "Link mời không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),

    // File / Avatar
    INVALID_FILE(4001, "File không hợp lệ", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_FILE_TYPE(4002, "File không được hỗ trợ", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(4003, "File quá lớn", HttpStatus.BAD_REQUEST),
    AVATAR_NOT_FOUND(4004, "Avatar không tồn tại", HttpStatus.NOT_FOUND);

    final int code;
    final String message;
    final HttpStatusCode httpStatusCode;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}

