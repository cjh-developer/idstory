package com.idstory.comprole.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 직책 등록 DTO
 */
@Getter
@Setter
public class CompRoleCreateDto {

    @NotBlank(message = "직책코드를 입력하세요.")
    private String compRoleCode;

    @NotBlank(message = "직책명을 입력하세요.")
    private String compRoleName;

    private String compRoleDesc;

    private int sortOrder = 0;

    private String useYn = "Y";
}
