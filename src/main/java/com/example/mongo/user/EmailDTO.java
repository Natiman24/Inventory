package com.example.mongo.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmailDTO {
    private String to;
    private String subject;
    private String text;
}
