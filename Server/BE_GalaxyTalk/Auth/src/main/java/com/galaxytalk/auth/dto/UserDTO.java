package com.galaxytalk.auth.dto;

import lombok.Getter;
import lombok.Setter;


//spring security 인증용 객체
@Getter
@Setter
public class UserDTO {

    private String role;
    private String name;
}
