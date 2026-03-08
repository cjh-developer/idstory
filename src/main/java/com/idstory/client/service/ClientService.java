package com.idstory.client.service;

import com.idstory.client.dto.ClientCreateDto;
import com.idstory.client.dto.ClientUpdateDto;
import com.idstory.client.entity.Client;
import com.idstory.client.repository.ClientRepository;
import com.idstory.common.util.OidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 클라이언트 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    // ─────────────────────────────────────────────────────────────
    //  조회
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Client> findAll() {
        return clientRepository.findByDeletedAtIsNullOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<Client> buildTree() {
        List<Client> all = findAll();
        return buildTree(all);
    }

    @Transactional(readOnly = true)
    public Client getByOid(String clientOid) {
        return clientRepository.findById(clientOid)
                .orElseThrow(() -> new IllegalArgumentException("클라이언트를 찾을 수 없습니다: " + clientOid));
    }

    // ─────────────────────────────────────────────────────────────
    //  등록
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Client create(ClientCreateDto dto, String performedBy) {
        String code = dto.getClientCode().toUpperCase().trim();
        if (clientRepository.existsByClientCode(code)) {
            throw new IllegalArgumentException("이미 사용 중인 클라이언트 코드입니다: " + code);
        }

        Client c = Client.builder()
                .clientOid(OidGenerator.generate())
                .clientCode(code)
                .clientName(dto.getClientName().trim())
                .parentOid(blankToNull(dto.getParentOid()))
                .description(blankToNull(dto.getDescription()))
                .appType(dto.getAppType() != null ? dto.getAppType() : "IAM")
                .sortOrder(dto.getSortOrder())
                .useYn(dto.getUseYn() != null ? dto.getUseYn() : "Y")
                .createdBy(performedBy)
                .build();

        clientRepository.save(c);
        log.info("[ClientService] 클라이언트 등록 - oid: {}, code: {}", c.getClientOid(), c.getClientCode());
        return c;
    }

    // ─────────────────────────────────────────────────────────────
    //  수정
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Client update(String clientOid, ClientUpdateDto dto, String performedBy) {
        Client c = getByOid(clientOid);

        // 자기 자신을 상위로 설정 방지
        String newParent = blankToNull(dto.getParentOid());
        if (clientOid.equals(newParent)) {
            throw new IllegalArgumentException("자기 자신을 상위 클라이언트로 설정할 수 없습니다.");
        }

        c.setClientName(dto.getClientName().trim());
        c.setParentOid(newParent);
        c.setDescription(blankToNull(dto.getDescription()));
        c.setAppType(dto.getAppType() != null ? dto.getAppType() : "IAM");
        c.setSortOrder(dto.getSortOrder());
        c.setUseYn(dto.getUseYn() != null ? dto.getUseYn() : "Y");
        c.setUpdatedBy(performedBy);

        clientRepository.save(c);
        log.info("[ClientService] 클라이언트 수정 - oid: {}", c.getClientOid());
        return c;
    }

    // ─────────────────────────────────────────────────────────────
    //  삭제 (소프트)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void softDelete(String clientOid, String performedBy) {
        Client c = getByOid(clientOid);
        if (c.getDeletedAt() != null) {
            throw new IllegalArgumentException("이미 삭제된 클라이언트입니다.");
        }
        c.setDeletedAt(LocalDateTime.now());
        c.setDeletedBy(performedBy);
        clientRepository.save(c);
        log.info("[ClientService] 클라이언트 삭제 - oid: {}", c.getClientOid());
    }

    // ─────────────────────────────────────────────────────────────
    //  내부 유틸
    // ─────────────────────────────────────────────────────────────

    /** 플랫 리스트 → 계층 트리 변환 */
    private List<Client> buildTree(List<Client> all) {
        Map<String, Client> map = new LinkedHashMap<>();
        all.forEach(c -> map.put(c.getClientOid(), c));

        List<Client> roots = new ArrayList<>();
        for (Client c : all) {
            c.setChildren(new ArrayList<>());
            if (c.getParentOid() == null || !map.containsKey(c.getParentOid())) {
                roots.add(c);
            } else {
                map.get(c.getParentOid()).getChildren().add(c);
            }
        }
        return roots;
    }

    public Map<String, Object> toMap(Client c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clientOid",   c.getClientOid());
        m.put("clientCode",  c.getClientCode());
        m.put("clientName",  c.getClientName());
        m.put("parentOid",   c.getParentOid());
        m.put("description", c.getDescription());
        m.put("appType",     c.getAppType());
        m.put("sortOrder",   c.getSortOrder());
        m.put("useYn",       c.getUseYn());
        m.put("createdAt",   c.getCreatedAt()  != null ? c.getCreatedAt().toString()  : null);
        m.put("createdBy",   c.getCreatedBy());
        m.put("updatedAt",   c.getUpdatedAt()  != null ? c.getUpdatedAt().toString()  : null);
        m.put("updatedBy",   c.getUpdatedBy());
        m.put("deletedAt",   c.getDeletedAt()  != null ? c.getDeletedAt().toString()  : null);
        m.put("deletedBy",   c.getDeletedBy());
        return m;
    }

    /** 트리 구조를 Map 형태로 직렬화 */
    public List<Map<String, Object>> toTreeMapList(List<Client> nodes) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Client c : nodes) {
            Map<String, Object> m = toMap(c);
            if (c.getChildren() != null && !c.getChildren().isEmpty()) {
                m.put("children", toTreeMapList(c.getChildren()));
            } else {
                m.put("children", new ArrayList<>());
            }
            result.add(m);
        }
        return result;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
