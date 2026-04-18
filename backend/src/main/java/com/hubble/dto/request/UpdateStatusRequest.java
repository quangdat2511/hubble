package com.hubble.dto.request;

import com.hubble.enums.UserStatus;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    private UserStatus status;
}
