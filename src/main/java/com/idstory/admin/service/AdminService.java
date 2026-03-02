package com.idstory.admin.service;

import com.idstory.admin.entity.SysAdmin;
import com.idstory.admin.repository.SysAdminRepository;
import com.idstory.common.util.OidGenerator;
import com.idstory.history.service.UserAccountHistoryService;
import com.idstory.user.dto.UserUpdateDto;
import com.idstory.user.entity.SysUser;
import com.idstory.user.repository.SysUserRepository;
import com.idstory.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 관리자 관리 서비스
 *
 * <ul>
 *   <li>관리자 등록 — 기존 USER를 ADMIN으로 승격 + sys_admins 레코드 생성</li>
 *   <li>관리자 수정 — SysUser 정보 + admin_note 업데이트</li>
 *   <li>관리자 해제 — sys_admins 레코드 삭제 + role → USER 강등</li>
 *   <li>관리자 목록 조회 — sys_admins JOIN sys_users 페이징</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final SysAdminRepository adminRepository;
    private final SysUserRepository userRepository;
    private final SysUserService sysUserService;
    private final UserAccountHistoryService historyService;

    /**
     * 관리자 등록 — 기존 USER를 ADMIN으로 승격합니다.
     *
     * @param userOid     승격 대상 사용자 OID
     * @param adminNote   관리자 비고
     * @param performedBy 처리자 user_id
     * @param ip          처리자 IP
     */
    @Transactional
    public SysAdmin registerAdmin(String userOid, String adminNote,
                                  String performedBy, String ip) {
        SysUser user = sysUserService.getUserByOid(userOid);

        if (adminRepository.existsByUser(user)) {
            throw new IllegalArgumentException("이미 관리자로 등록된 사용자입니다: " + user.getUserId());
        }

        // sys_users role = ADMIN
        user.setRole("ADMIN");
        user.setUpdatedBy(performedBy);
        userRepository.save(user);

        // sys_admins 생성
        SysAdmin admin = SysAdmin.builder()
                .adminOid(OidGenerator.generate())
                .user(user)
                .adminNote(blankToNull(adminNote))
                .grantedAt(LocalDateTime.now())
                .grantedBy(performedBy)
                .build();
        adminRepository.save(admin);

        historyService.log(userOid, user.getUserId(), "UPDATE",
                "관리자 등록 - userId: " + user.getUserId(), performedBy, ip);
        log.info("[AdminService] 관리자 등록 - adminOid: {}, userId: {}", admin.getAdminOid(), user.getUserId());
        return admin;
    }

    /**
     * 관리자 수정 — SysUser 기본 정보 + admin_note를 업데이트합니다.
     *
     * @param adminOid    대상 SysAdmin OID
     * @param dto         사용자 수정 DTO
     * @param adminNote   관리자 비고
     * @param performedBy 처리자 user_id
     * @param ip          처리자 IP
     */
    @Transactional
    public SysAdmin updateAdmin(String adminOid, UserUpdateDto dto, String adminNote,
                                String performedBy, String ip) {
        SysAdmin admin = getAdminByOid(adminOid);

        dto.setRole("ADMIN"); // 역할 유지
        sysUserService.updateUser(admin.getUser().getOid(), dto, performedBy, ip);

        admin.setAdminNote(blankToNull(adminNote));
        admin.setUpdatedAt(LocalDateTime.now());
        admin.setUpdatedBy(performedBy);
        adminRepository.save(admin);

        log.info("[AdminService] 관리자 수정 - adminOid: {}, userId: {}",
                adminOid, admin.getUser().getUserId());
        return admin;
    }

    /**
     * 관리자 해제 — sys_admins 레코드 삭제 + role → USER 강등
     *
     * @param adminOid    대상 SysAdmin OID
     * @param performedBy 처리자 user_id
     * @param ip          처리자 IP
     */
    @Transactional
    public SysUser demoteAdmin(String adminOid, String performedBy, String ip) {
        SysAdmin admin = getAdminByOid(adminOid);
        SysUser user = admin.getUser();

        user.setRole("USER");
        user.setUpdatedBy(performedBy);
        userRepository.save(user);

        adminRepository.delete(admin);

        historyService.log(user.getOid(), user.getUserId(), "UPDATE",
                "관리자 권한 해제 - userId: " + user.getUserId(), performedBy, ip);
        log.info("[AdminService] 관리자 해제 - adminOid: {}, userId: {}", adminOid, user.getUserId());
        return user;
    }

    /**
     * 관리자 목록 페이징 조회
     *
     * @param keyword  아이디/이름 키워드 (null=전체)
     * @param pageable 페이징 정보
     */
    @Transactional(readOnly = true)
    public Page<SysAdmin> findAdmins(String keyword, Pageable pageable) {
        return adminRepository.findByFilter(blankToNull(keyword), pageable);
    }

    /**
     * adminOid로 SysAdmin 단건 조회
     *
     * @throws IllegalArgumentException 존재하지 않는 경우
     */
    @Transactional(readOnly = true)
    public SysAdmin getAdminByOid(String adminOid) {
        return adminRepository.findById(adminOid)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다: " + adminOid));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
