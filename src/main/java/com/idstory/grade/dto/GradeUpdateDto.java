package com.idstory.grade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 직급 수정 DTO
 */
@Getter
@Setter
public class GradeUpdateDto {

    @NotBlank(message = "직급명을 입력하세요.")
    private String gradeName;

    private String gradeDesc;

    private int sortOrder = 0;

    private String useYn = "Y";
}
