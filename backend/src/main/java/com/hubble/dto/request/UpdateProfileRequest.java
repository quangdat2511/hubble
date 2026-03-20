package com.hubble.dto.request;

import com.hubble.enums.UserStatus;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String displayName;
    private String phone;
    private String bio;
    private UserStatus status;
}