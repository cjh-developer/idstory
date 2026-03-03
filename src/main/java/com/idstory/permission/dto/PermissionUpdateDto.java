package com.idstory.permission.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 권한 수정 DTO
 */
@Getter
@Setter
public class PermissionUpdateDto {

    @NotBlank(message = "권한명을 입력하세요.")
    private String permName;

    private String parentOid;

    private String description;

    private int sortOrder = 0;

    private String useYn = "Y";
}
