package com.hubble.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerInviteRequest {

    @NotBlank(message = "inviteeUsername is required")
    private String inviteeUsername;
}


