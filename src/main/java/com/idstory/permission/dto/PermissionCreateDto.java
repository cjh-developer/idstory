package com.idstory.permission.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 권한 등록 DTO
 */
@Getter
@Setter
public class PermissionCreateDto {

    @NotBlank(message = "클라이언트 OID를 입력하세요.")
    private String clientOid;

    @NotBlank(message = "권한 코드를 입력하세요.")
    private String permCode;

    @NotBlank(message = "권한명을 입력하세요.")
    private String permName;

    private String parentOid;

    private String description;

    private int sortOrder = 0;

    private String useYn = "Y";
}
