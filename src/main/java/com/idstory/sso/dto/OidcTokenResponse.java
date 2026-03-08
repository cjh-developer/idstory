package com.idstory.sso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * OIDC Token Endpoint 응답 DTO
 * RFC 6749 / OpenID Connect Core 1.0 준수
 */
@Getter
@Builder
public class OidcTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private int expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("scope")
    private String scope;
}
