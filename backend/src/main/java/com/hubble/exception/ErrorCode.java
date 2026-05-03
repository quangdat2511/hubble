package com.hubble.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    // System
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),

    // User
    USER_EXISTED(1002, "Email or username already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "User does not exist", HttpStatus.NOT_FOUND),
    USERNAME_INVALID(1004, "Display name must have at least {min} characters", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1005, "Password must have at least {min} characters", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1006, "Invalid email", HttpStatus.BAD_REQUEST),

    FILE_EMPTY(1007, "File is empty", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(1008, "File exceeds maximum allowed size", HttpStatus.BAD_REQUEST),
    FILE_TYPE_NOT_ALLOWED(1009, "File type is not allowed", HttpStatus.BAD_REQUEST),
    UPLOAD_FAILED(1010, "Failed to upload file to storage", HttpStatus.BAD_REQUEST),

    CANNOT_DM_SELF(1017, "You cannot create a DM channel with yourself", HttpStatus.BAD_REQUEST),

    // Authentication
    UNAUTHENTICATED(1007, "Not authenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1008, "You do not have permission to perform this action", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(1009, "Token is invalid or expired", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(1010, "Incorrect email or password", HttpStatus.UNAUTHORIZED),
    PHONE_EXISTED(1011, "Phone number is already in use", HttpStatus.BAD_REQUEST),
    INVALID_OTP(1012, "OTP is invalid or expired", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1013, "OTP has expired", HttpStatus.BAD_REQUEST),
    GOOGLE_AUTH_FAILED(1014, "Google authentication failed", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID(1015, "Refresh token is invalid or expired", HttpStatus.UNAUTHORIZED),
    PASSWORDS_NOT_MATCH(1016, "Passwords do not match", HttpStatus.BAD_REQUEST),
    QR_TOKEN_INVALID(1018, "QR token is invalid or expired", HttpStatus.BAD_REQUEST),
    LOCALE_INVALID(1019, "Locale is invalid", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED(1020, "Email has not been verified. Please verify your email to continue.", HttpStatus.FORBIDDEN),

    // Friends
    FRIEND_REQUEST_EXISTED(2001, "Friend request already exists", HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_NOT_FOUND(2002, "Friend request not found", HttpStatus.NOT_FOUND),
    ALREADY_FRIENDS(2003, "Users are already friends", HttpStatus.BAD_REQUEST),
    CANNOT_FRIEND_SELF(2004, "You cannot friend yourself", HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_FORBIDDEN(2005, "Cannot send friend request because of block relationship", HttpStatus.BAD_REQUEST),

    // Server / Channel
    SERVER_NOT_FOUND(3001, "Server not found", HttpStatus.NOT_FOUND),
    CHANNEL_NOT_FOUND(3002, "Channel not found", HttpStatus.NOT_FOUND),
    INVITE_CODE_INVALID(3003, "Invite link is invalid or expired", HttpStatus.BAD_REQUEST),
    SERVER_MEMBER_NOT_FOUND(3004, "Server member not found", HttpStatus.NOT_FOUND),
    ALREADY_SERVER_MEMBER(3005, "User is already a server member", HttpStatus.BAD_REQUEST),
    CANNOT_KICK_OWNER(3006, "Cannot kick the server owner", HttpStatus.BAD_REQUEST),
    SERVER_INVITE_NOT_FOUND(3007, "Server invite not found", HttpStatus.NOT_FOUND),
    SERVER_INVITE_ALREADY_RESPONDED(3008, "Server invite has already been responded to", HttpStatus.BAD_REQUEST),
    SERVER_INVITE_EXPIRED(3009, "Server invite has expired", HttpStatus.BAD_REQUEST),
    CANNOT_INVITE_SELF(3010, "You cannot invite yourself", HttpStatus.BAD_REQUEST),
    SERVER_INVITE_ALREADY_SENT(3011, "Server invite was already sent", HttpStatus.BAD_REQUEST),

    // Misc
    NOT_FOUND(4001, "Resource not found", HttpStatus.NOT_FOUND),

    // Messages
    MESSAGE_NOT_FOUND(5001, "Message not found", HttpStatus.NOT_FOUND),
    MESSAGE_NOT_OWNER(5002, "You do not have permission to perform this action on this message", HttpStatus.FORBIDDEN),

    // Server icon
    NOT_SERVER_OWNER(3012, "You are not the server owner", HttpStatus.FORBIDDEN),
    SERVER_NAME_INVALID(3013, "Server name must be between 1 and 100 characters", HttpStatus.BAD_REQUEST),

    // Roles
    ROLE_NOT_FOUND(3020, "Role not found", HttpStatus.NOT_FOUND),
    ROLE_NAME_DUPLICATED(3021, "Role name already exists in this server", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_DEFAULT_ROLE(3022, "Cannot delete the default role", HttpStatus.BAD_REQUEST),
    CANNOT_MODIFY_DEFAULT_ROLE_NAME(3023, "Cannot rename the default role", HttpStatus.BAD_REQUEST),
    CANNOT_MANAGE_HIGHER_ROLE(3024, "Cannot manage a role positioned higher than yours", HttpStatus.FORBIDDEN),

    // File upload
    FILE_UPLOAD_FAILED(6001, "Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_TYPE(6002, "Invalid file type. Only image files are accepted (jpeg, png, gif, webp, svg)", HttpStatus.BAD_REQUEST),

    // Avatar upload
    INVALID_FILE(7001, "Invalid file", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_FILE_TYPE(7002, "Unsupported file type", HttpStatus.BAD_REQUEST),
    AVATAR_NOT_FOUND(7003, "Avatar not found", HttpStatus.NOT_FOUND),

    INVALID_THEME(8001, "Invalid theme", HttpStatus.FORBIDDEN);

    final int code;
    final String message;
    final HttpStatusCode httpStatusCode;

    ErrorCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}
