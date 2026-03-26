package com.hubble.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION(9999, "Loi khong xac dinh", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Message key khong hop le", HttpStatus.BAD_REQUEST),

    USER_EXISTED(1002, "Email hoac username da ton tai", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "Nguoi dung khong ton tai", HttpStatus.NOT_FOUND),
    USERNAME_INVALID(1004, "Ten hien thi phai co it nhat {min} ky tu", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1005, "Mat khau phai co it nhat {min} ky tu", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1006, "Email khong hop le", HttpStatus.BAD_REQUEST),
    CANNOT_DM_SELF(1007, "Ban khong the tao kenh DM voi chinh minh", HttpStatus.BAD_REQUEST),

    UNAUTHENTICATED(1008, "Chua dang nhap", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1009, "Ban khong co quyen thuc hien thao tac nay", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(1010, "Token khong hop le hoac da het han", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(1011, "Email hoac mat khau khong chinh xac", HttpStatus.UNAUTHORIZED),
    PHONE_EXISTED(1012, "So dien thoai da duoc su dung", HttpStatus.BAD_REQUEST),
    INVALID_OTP(1013, "Ma OTP khong hop le hoac da het han", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1014, "Ma OTP da het han", HttpStatus.BAD_REQUEST),
    GOOGLE_AUTH_FAILED(1015, "Xac thuc Google that bai", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID(1016, "Refresh token khong hop le hoac da het han", HttpStatus.UNAUTHORIZED),
    PASSWORDS_NOT_MATCH(1017, "Mat khau xac nhan khong khop", HttpStatus.BAD_REQUEST),

    FRIEND_REQUEST_EXISTED(2001, "Loi moi ket ban da ton tai", HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_NOT_FOUND(2002, "Loi moi ket ban khong ton tai", HttpStatus.NOT_FOUND),
    ALREADY_FRIENDS(2003, "Hai nguoi da la ban be", HttpStatus.BAD_REQUEST),
    CANNOT_FRIEND_SELF(2004, "Ban khong the ket ban voi chinh minh", HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_FORBIDDEN(2005, "Khong the gui loi moi do quan he chan", HttpStatus.BAD_REQUEST),

    SERVER_NOT_FOUND(3001, "Server khong ton tai", HttpStatus.NOT_FOUND),
    CHANNEL_NOT_FOUND(3002, "Kenh khong ton tai", HttpStatus.NOT_FOUND),
    INVITE_CODE_INVALID(3003, "Link moi khong hop le hoac da het han", HttpStatus.BAD_REQUEST),
    INVALID_THEME(3004, "Theme khong hop le", HttpStatus.BAD_REQUEST),

    NOT_FOUND(4001, "Tai nguyen khong ton tai", HttpStatus.NOT_FOUND);

    final int code;
    final String message;
    final HttpStatusCode httpStatusCode;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}
