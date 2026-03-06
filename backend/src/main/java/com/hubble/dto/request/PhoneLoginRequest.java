package com.hubble.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PhoneLoginRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    String phone;

    @NotBlank(message = "PASSWORD_INVALID")
    String password;
}
