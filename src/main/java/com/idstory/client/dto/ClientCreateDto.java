package com.idstory.client.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 클라이언트 등록 DTO
 */
@Getter
@Setter
public class ClientCreateDto {

    @NotBlank(message = "클라이언트 코드를 입력하세요.")
    private String clientCode;

    @NotBlank(message = "클라이언트명을 입력하세요.")
    private String clientName;

    private String parentOid;

    private String description;

    private String appType = "IAM";

    private int sortOrder = 0;

    private String useYn = "Y";

    // SSO 설정 (appType=SSO 시)
    private String ssoClientId;
    private String ssoRedirectUris;
    private String authUri;
    private String authResult;
    private String noUseSso;
}
