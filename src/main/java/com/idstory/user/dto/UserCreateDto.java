package com.idstory.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 사용자 등록 DTO
 */
@Getter
@Setter
public class UserCreateDto {

    /** 로그인 계정 (sys_users.user_id) */
    @NotBlank(message = "아이디를 입력하세요.")
    private String userId;

    /** 이름 */
    @NotBlank(message = "이름을 입력하세요.")
    private String name;

    /** 휴대번호 */
    private String phone;

    /** 이메일 */
    @NotBlank(message = "이메일을 입력하세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    /** 부서코드 */
    private String deptCode;

    /** 초기 비밀번호 */
    @NotBlank(message = "비밀번호를 입력하세요.")
    private String password;

    /** 역할 */
    private String role = "USER";

    /** 계정 사용 시작일 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate validStartDate;

    /** 계정 사용 종료일 (null=무제한) */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate validEndDate;

    /** 담당 업무 */
    private String jobDuty;

    /** 겸직 가능 여부 Y|N */
    private String concurrentYn = "N";

    // ── 조직 정보 (선택, 등록 시 org_map 생성용) ──────────────────

    /** 직위 OID */
    private String positionOid;

    /** 직위명 (비정규화 저장용) */
    private String positionName;

    /** 직급 OID */
    private String gradeOid;

    /** 직급명 (비정규화 저장용) */
    private String gradeName;

    /** 직책 OID */
    private String compRoleOid;

    /** 직책명 (비정규화 저장용) */
    private String compRoleName;
}
