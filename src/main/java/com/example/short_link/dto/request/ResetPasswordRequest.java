package com.example.short_link.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Pls fill the newPassword")
    private String newPassword;

    @NotBlank
    @JsonProperty("confirmPassword")
    private String confirmPassword;

    @AssertTrue(message = "passwords not match")
    private boolean isPasswordMatched() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
