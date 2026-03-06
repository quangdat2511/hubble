package com.hubble.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PhoneVerifyOtpRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    String phone;

    @NotBlank(message = "Mã OTP không được để trống")
    @Size(min = 6, max = 6, message = "Mã OTP phải có 6 ký tự")
    String otpCode;

    // Username và password cho đăng ký mới
    String username;

    @Size(min = 6, message = "PASSWORD_INVALID")
    String password;
}
