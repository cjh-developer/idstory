package com.idstory.sso.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SsoClientCreateDto {

    @NotBlank(message = "클라이언트 OID는 필수입니다.")
    private String clientOid;

    @NotBlank(message = "Client ID는 필수입니다.")
    private String clientId;

    @NotBlank(message = "Redirect URI는 필수입니다.")
    private String redirectUris;

    private String scopes    = "openid profile email";
    private String grantTypes = "authorization_code";

    private int accessTokenValiditySec  = 3600;
    private int refreshTokenValiditySec = 86400;
    private int idTokenValiditySec      = 3600;
}
