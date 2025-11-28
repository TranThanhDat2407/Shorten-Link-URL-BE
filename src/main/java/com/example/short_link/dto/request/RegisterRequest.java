package com.example.short_link.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Please fill the email")
    @Email(message = "Invalid Email")
    private String email;

    @NotBlank(message = "Please fill the password")
    private String password;


}
