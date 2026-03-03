package com.idstory.role.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 역할 수정 DTO
 */
@Getter
@Setter
public class RoleUpdateDto {

    @NotBlank(message = "역할명을 입력하세요.")
    private String roleName;

    private String parentOid;

    private String description;

    private int sortOrder = 0;

    private String useYn = "Y";
}
