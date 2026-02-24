package com.hubble.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {

    @Email(message = "EMAIL_INVALID")
    String email;

    @Size(min = 6, message = "PASSWORD_INVALID")
    String password;

    @Size(min = 2, message = "USERNAME_INVALID")
    String displayName;
}
