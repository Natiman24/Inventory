package com.example.mongo.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Users")
@Builder
public class User {
    @Id
    private String id;
    private String firstName;
    private String lastName;
    private UserType role;
    private String phoneNumber;
    private String email;
    private String password;
    private boolean isFirstTime;
    private String otp;
    private LocalDate joinedOn;

    public boolean getFirstTime(){
        return isFirstTime;
    }

}
