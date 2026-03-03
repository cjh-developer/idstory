package com.idstory.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 사용자 수정 DTO
 */
@Getter
@Setter
public class UserUpdateDto {

    /** 이름 */
    @NotBlank(message = "이름을 입력하세요.")
    private String name;

    /** 이메일 */
    @NotBlank(message = "이메일을 입력하세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    /** 휴대번호 */
    private String phone;

    /** 부서코드 */
    private String deptCode;

    /** 역할 (USER | ADMIN) */
    private String role;

    /** 사용여부 (Y | N) */
    private String useYn;

    /** 상태 (ACTIVE | SLEEPER | OUT) */
    private String status;

    /** 잠금여부 (Y | N) — 관리자가 직접 잠금 해제 가능 */
    private String lockYn;

    /** 계정 사용 시작일 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate validStartDate;

    /** 계정 사용 종료일 (null=무제한) */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate validEndDate;

    /** 새 비밀번호 (빈 문자열 또는 null → 변경 안 함) */
    private String newPassword;

    /** 담당 업무 */
    private String jobDuty;

    /** 겸직 가능 여부 Y|N */
    private String concurrentYn;
}
