package com.idstory.policy.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * SystemPolicy 복합 PK 클래스
 */
public class SystemPolicyId implements Serializable {

    private String policyGroup;
    private String policyKey;

    public SystemPolicyId() {}

    public SystemPolicyId(String policyGroup, String policyKey) {
        this.policyGroup = policyGroup;
        this.policyKey = policyKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SystemPolicyId)) return false;
        SystemPolicyId that = (SystemPolicyId) o;
        return Objects.equals(policyGroup, that.policyGroup) &&
               Objects.equals(policyKey, that.policyKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyGroup, policyKey);
    }
}
