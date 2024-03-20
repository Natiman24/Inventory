package com.example.mongo.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailValidationResult {
    private String email;
    private String autocorrect;
    private String deliverability;
    @JsonProperty("quality_score")
    private String qualityScore;
    @JsonProperty("is_valid_format")
    private ValidationResult isValidFormat;
    @JsonProperty("is_free_email")
    private ValidationResult isFreeEmail;
    @JsonProperty("is_disposable_email")
    private ValidationResult isDisposableEmail;
    @JsonProperty("is_role_email")
    private ValidationResult isRoleEmail;
    @JsonProperty("is_catchall_email")
    private ValidationResult isCatchallEmail;
    @JsonProperty("is_mx_found")
    private ValidationResult isMxFound;
    @JsonProperty("is_smtp_valid")
    private ValidationResult isSmtpValid;
}

@Getter
@Setter
class ValidationResult {
    private boolean value;
    private String text;

    // Getters and setters
}