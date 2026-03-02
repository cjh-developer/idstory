package com.idstory.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * XML 설정 기반 커스텀 비밀번호 인코더.
 *
 * <p>설정 파일: {@code resources/config/password-config.xml}</p>
 *
 * <h3>비밀번호 해싱 규칙 (password 컬럼 저장 형식 = 순수 해시)</h3>
 * <ul>
 *   <li>password-salt-enabled=true  : HASH(password-salt + 입력 비밀번호)</li>
 *   <li>password-salt-enabled=false : HASH(입력 비밀번호)</li>
 * </ul>
 *
 * <h3>password_salt 컬럼</h3>
 * <p>salt 사용 시 XML {@code <password-salt>} 값이 별도 컬럼에 저장됩니다.
 * 실제 저장 처리는 서비스 레이어({@code SysUserService})에서 담당합니다.</p>
 */
@Slf4j
@Component
public class CustomPasswordEncoder implements PasswordEncoder {

    private final PasswordXmlProperties props;

    public CustomPasswordEncoder(PasswordXmlProperties props) {
        this.props = props;
    }

    /**
     * 비밀번호를 해시합니다.
     *
     * <ul>
     *   <li>password-salt-enabled=true  → HASH(salt + rawPassword)</li>
     *   <li>password-salt-enabled=false → HASH(rawPassword)</li>
     * </ul>
     *
     * @param rawPassword 원문 비밀번호
     * @return 해시된 비밀번호 문자열 (HEX 또는 BASE64, 순수 해시)
     */
    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("비밀번호는 null일 수 없습니다.");
        }
        String input = buildInput(rawPassword.toString());
        return hash(input);
    }

    /**
     * 입력된 원문 비밀번호와 저장된 해시값을 비교합니다.
     *
     * @param rawPassword     로그인 시 입력된 원문 비밀번호
     * @param encodedPassword DB에 저장된 해시된 비밀번호
     * @return 일치 여부
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        return encode(rawPassword).equalsIgnoreCase(encodedPassword);
    }

    /**
     * XML {@code password-salt-enabled=true} 시 솔트 값을 반환합니다.
     * 서비스 레이어에서 {@code password_salt} 컬럼에 저장할 때 사용합니다.
     *
     * @return salt 값, 또는 비활성화 시 {@code null}
     */
    public String getConfiguredSalt() {
        return props.isPasswordSaltEnabled() ? props.getPasswordSalt() : null;
    }

    /**
     * salt 사용 여부에 따라 해시 입력 문자열을 구성합니다.
     */
    private String buildInput(String rawPassword) {
        if (props.isPasswordSaltEnabled() && !props.getPasswordSalt().isBlank()) {
            return props.getPasswordSalt() + rawPassword;
        }
        return rawPassword;
    }

    /**
     * MessageDigest로 해싱 후 설정된 인코딩(HEX/BASE64)으로 변환합니다.
     */
    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(props.getAlgorithm());
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return switch (props.getEncoding().toUpperCase()) {
                case "BASE64" -> Base64.getEncoder().encodeToString(hashBytes);
                case "HEX"   -> toHexString(hashBytes);
                default -> throw new IllegalStateException(
                        "지원하지 않는 인코딩: '" + props.getEncoding() + "'. 사용 가능한 값: HEX, BASE64");
            };
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("알고리즘을 찾을 수 없습니다: " + props.getAlgorithm(), e);
        }
    }

    /**
     * 바이트 배열을 소문자 16진수 문자열로 변환합니다.
     */
    private String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
}
