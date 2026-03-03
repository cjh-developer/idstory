package com.idstory.role.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 역할 등록 DTO
 */
@Getter
@Setter
public class RoleCreateDto {

    @NotBlank(message = "역할 코드를 입력하세요.")
    private String roleCode;

    @NotBlank(message = "역할명을 입력하세요.")
    private String roleName;

    private String parentOid;

    private String description;

    private int sortOrder = 0;

    private String useYn = "Y";
}
