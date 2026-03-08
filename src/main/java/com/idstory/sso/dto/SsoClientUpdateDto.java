package com.idstory.sso.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SsoClientUpdateDto {

    @NotBlank(message = "Redirect URI는 필수입니다.")
    private String redirectUris;

    private String scopes     = "openid profile email";
    private String grantTypes = "authorization_code";

    private int  accessTokenValiditySec  = 3600;
    private int  refreshTokenValiditySec = 86400;
    private int  idTokenValiditySec      = 3600;
    private String useYn = "Y";

    private String authUri;
    private String authResult;
    private String noUseSso;
}
