package com.example.mongo.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EditProfileDTO {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
}
