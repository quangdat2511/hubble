package com.hubble.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {

    // Chỉ cho phép sửa các trường hồ sơ, không sửa email / password tại đây
    String displayName;
    String bio;
    String customStatus;
    String avatarUrl;
    String language;
}
