package com.example.mongo.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPassDTO {
    private String phoneNumber;
    private String otp;
    private String newPass;
}
