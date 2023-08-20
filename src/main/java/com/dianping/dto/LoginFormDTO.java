package com.dianping.dto;

import lombok.Data;


/**
 * 登录表
 */
@Data

public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;

}
