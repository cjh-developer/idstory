package com.idstory.common.util;

import java.security.SecureRandom;

/**
 * OID(Object ID) 생성 유틸리티.
 *
 * <p>형식: {@code ids_} + 영숫자(A-Za-z0-9) 14자 = 총 18자</p>
 * <p>예: {@code ids_a8F3kL9xP2qR7t}</p>
 */
public final class OidGenerator {

    private static final String CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private OidGenerator() {}

    /**
     * 고유한 OID를 생성합니다.
     *
     * @return {@code ids_} 접두사를 포함한 18자 OID
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder("ids_");
        for (int i = 0; i < 14; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
