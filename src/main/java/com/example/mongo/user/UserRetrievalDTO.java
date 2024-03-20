package com.example.mongo.user;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRetrievalDTO {
    @Id
    private String id;
    private String firstName;
    private String lastName;
    private UserType role;
    private String phoneNumber;
    private String email;
    private LocalDate joinedOn;
}
