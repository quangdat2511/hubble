package com.hubble.dto.request;

import com.hubble.validator.TagConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateUserRequest {

    @NotBlank(message = "Firebase UID không được để trống")
    String firebaseUid;

    @NotBlank(message = "USERNAME_INVALID")
    @TagConstraint
    String username;

    String displayName;

    @Email(message = "EMAIL_INVALID")
    String email;

    String phone;
}
