package com.management.system.security.vo;

import com.management.system.enums.UserRoleEnum;
import lombok.*;


@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private Long id;
    private String email;
    private UserRoleEnum role;
    private String accessToken;

}


