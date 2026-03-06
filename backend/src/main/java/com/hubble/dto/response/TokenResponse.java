package com.hubble.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenResponse {

    String accessToken;
    String refreshToken;
    long expiresIn;
    UserResponse user;
}
