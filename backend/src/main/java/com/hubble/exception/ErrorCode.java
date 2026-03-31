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

    FILE_EMPTY(1007, "File is empty", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(1008, "File exceeds maximum allowed size", HttpStatus.BAD_REQUEST),
    FILE_TYPE_NOT_ALLOWED(1009, "File type is not allowed", HttpStatus.BAD_REQUEST),
    UPLOAD_FAILED(1010, "Failed to upload file to storage", HttpStatus.BAD_REQUEST),

    CANNOT_DM_SELF(1017, "Bạn không thể tạo kênh DM với chính mình", HttpStatus.BAD_REQUEST),
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
    SERVER_MEMBER_NOT_FOUND(3004, "Thành viên không tồn tại trong server", HttpStatus.NOT_FOUND),
    CANNOT_KICK_OWNER(3006, "Không thể kick chủ sở hữu server", HttpStatus.BAD_REQUEST),
    ALREADY_SERVER_MEMBER(3005, "Người dùng đã là thành viên của server", HttpStatus.BAD_REQUEST),
    SERVER_INVITE_NOT_FOUND(3007, "Lời mời không tồn tại", HttpStatus.NOT_FOUND),
    SERVER_INVITE_ALREADY_RESPONDED(3008, "Lời mời đã được phản hồi", HttpStatus.BAD_REQUEST),
    SERVER_INVITE_EXPIRED(3009, "Lời mời đã hết hạn", HttpStatus.BAD_REQUEST),
    CANNOT_INVITE_SELF(3010, "Bạn không thể mời chính mình", HttpStatus.BAD_REQUEST),
    SERVER_INVITE_ALREADY_SENT(3011, "Lời mời đã được gửi trước đó", HttpStatus.BAD_REQUEST),

    // Tin nhắn
    MESSAGE_NOT_FOUND(5001, "Tin nhắn không tồn tại", HttpStatus.NOT_FOUND),
    MESSAGE_NOT_OWNER(5002, "Bạn không có quyền thực hiện thao tác này với tin nhắn", HttpStatus.FORBIDDEN),

    NOT_FOUND(4001, "Tài nguyên không tồn tại", HttpStatus.NOT_FOUND),

    // Server icon
    NOT_SERVER_OWNER(3012, "Bạn không phải chủ sở hữu server", HttpStatus.FORBIDDEN),

    // Roles
    ROLE_NOT_FOUND(3020, "Vai trò không tồn tại", HttpStatus.NOT_FOUND),
    ROLE_NAME_DUPLICATED(3021, "Tên vai trò đã tồn tại trong server", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_DEFAULT_ROLE(3022, "Không thể xoá vai trò mặc định", HttpStatus.BAD_REQUEST),
    CANNOT_MODIFY_DEFAULT_ROLE_NAME(3023, "Không thể đổi tên vai trò mặc định", HttpStatus.BAD_REQUEST),
    CANNOT_MANAGE_HIGHER_ROLE(3024, "Không thể quản lý vai trò có vị trí cao hơn", HttpStatus.FORBIDDEN),

    FILE_UPLOAD_FAILED(6001, "Không thể tải file lên", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_TYPE(6002, "Định dạng file không hợp lệ. Chỉ chấp nhận ảnh (jpeg, png, gif, webp, svg)", HttpStatus.BAD_REQUEST),

    INVALID_FILE(7001, "File không hợp lệ", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_FILE_TYPE(7002, "File không được hỗ trợ", HttpStatus.BAD_REQUEST),
    AVATAR_NOT_FOUND(7003, "Avatar không tồn tại", HttpStatus.NOT_FOUND);

    final int code;
    final String message;
    final HttpStatusCode httpStatusCode;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}
