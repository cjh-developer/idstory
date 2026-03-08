package com.idstory.client.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 클라이언트 수정 DTO
 */
@Getter
@Setter
public class ClientUpdateDto {

    @NotBlank(message = "클라이언트명을 입력하세요.")
    private String clientName;

    private String parentOid;

    private String description;

    private String appType = "IAM";

    private int sortOrder = 0;

    private String useYn = "Y";
}
