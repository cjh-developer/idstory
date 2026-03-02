package com.idstory.position.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 직위 엔티티
 * 매핑 테이블: ids_iam_position
 */
@Entity
@Table(name = "ids_iam_position")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @Column(name = "position_oid", length = 18, nullable = false)
    private String positionOid;

    @Column(name = "position_code", length = 20, nullable = false, unique = true)
    private String positionCode;

    @Column(name = "position_name", length = 100, nullable = false)
    private String positionName;

    @Column(name = "position_desc", length = 500)
    private String positionDesc;

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
