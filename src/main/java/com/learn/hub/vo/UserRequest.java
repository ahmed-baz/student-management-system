package com.learn.hub.vo;

import com.learn.hub.enums.UserRoleEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private UserRoleEnum role;

}

