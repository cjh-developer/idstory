package com.idstory.grade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 직급 등록 DTO
 */
@Getter
@Setter
public class GradeCreateDto {

    @NotBlank(message = "직급코드를 입력하세요.")
    private String gradeCode;

    @NotBlank(message = "직급명을 입력하세요.")
    private String gradeName;

    private String gradeDesc;

    private int sortOrder = 0;

    private String useYn = "Y";
}
