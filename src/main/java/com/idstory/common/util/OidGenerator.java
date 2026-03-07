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

    /**
     * 지정 길이의 무작위 영숫자 문자열을 생성합니다.
     *
     * @param length 문자열 길이
     * @return 무작위 영숫자 문자열
     */
    public static String randomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
