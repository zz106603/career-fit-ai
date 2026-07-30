package com.careerfit.identity.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthCredentialsRequest(
        @NotBlank(message = "이메일은 필수입니다.")
                @Size(max = 320, message = "이메일은 320자 이하여야 합니다.")
                String email,
        @NotBlank(message = "비밀번호는 필수입니다.")
                @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.")
                String password) {}
