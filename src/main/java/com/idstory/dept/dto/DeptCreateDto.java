package com.idstory.dept.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 부서 등록 DTO
 */
@Getter
@Setter
public class DeptCreateDto {

    @NotBlank(message = "부서코드는 필수입니다.")
    @Size(max = 20, message = "부서코드는 20자 이내로 입력하세요.")
    private String deptCode;

    @NotBlank(message = "부서명은 필수입니다.")
    @Size(max = 100, message = "부서명은 100자 이내로 입력하세요.")
    private String deptName;

    /** 상위 부서 OID (null=최상위) */
    private String parentDeptOid;

    private int sortOrder = 0;

    private String useYn = "Y";

    @Size(max = 20)
    private String deptType;

    @Size(max = 20)
    private String deptTel;

    @Size(max = 20)
    private String deptFax;

    @Size(max = 200)
    private String deptAddress;
}
