package com.example.mongo.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChangePassDTO {
    @Id
    private String id;
    private String oldPass;
    private String newPass;
}
