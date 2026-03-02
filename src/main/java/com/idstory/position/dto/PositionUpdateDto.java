package com.idstory.position.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 직위 수정 DTO
 */
@Getter
@Setter
public class PositionUpdateDto {

    @NotBlank(message = "직위명을 입력하세요.")
    private String positionName;

    private String positionDesc;

    private int sortOrder = 0;

    private String useYn = "Y";
}
