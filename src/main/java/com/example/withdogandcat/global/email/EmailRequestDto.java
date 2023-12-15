package com.example.withdogandcat.global.email;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailRequestDto {
    private String email;
    private String verificationCode;

}