package com.idstory.client.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 클라이언트(시스템/서비스) 엔티티
 * 매핑 테이블: ids_iam_client
 */
@Entity
@Table(name = "ids_iam_client")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @Column(name = "client_oid", length = 18, nullable = false)
    private String clientOid;

    @Column(name = "client_code", length = 50, nullable = false, unique = true)
    private String clientCode;

    @Column(name = "client_name", length = 100, nullable = false)
    private String clientName;

    @Column(name = "parent_oid", length = 18)
    private String parentOid;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "use_yn", length = 1, nullable = false)
    @Builder.Default
    private String useYn = "Y";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 50)
    private String deletedBy;

    /** 트리 구성용 (DB 컬럼 아님) */
    @Transient
    private List<Client> children;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
