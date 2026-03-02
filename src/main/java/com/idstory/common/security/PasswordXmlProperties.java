package com.idstory.common.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * resources/config/password-config.xml 을 읽어 비밀번호 암호화 설정을 제공합니다.
 *
 * <p>설정 파일 위치: src/main/resources/config/password-config.xml</p>
 * <ul>
 *   <li>algorithm             : SHA256 | SHA512</li>
 *   <li>encoding              : HEX | BASE64</li>
 *   <li>password-salt-enabled : true | false (기본: false)</li>
 *   <li>password-salt         : 솔트 문자열 (password-salt-enabled=true 일 때만 적용)</li>
 * </ul>
 *
 * <h3>비밀번호 해싱 규칙</h3>
 * <ul>
 *   <li>password-salt-enabled=true  : HASH(password-salt + 입력 비밀번호)</li>
 *   <li>password-salt-enabled=false : HASH(입력 비밀번호)</li>
 * </ul>
 */
@Slf4j
@Component
public class PasswordXmlProperties {

    private static final String CONFIG_PATH = "config/password-config.xml";

    /** MessageDigest 알고리즘 이름 (Java 표준: "SHA-256" / "SHA-512") */
    private String algorithm = "SHA-512";

    /** 인코딩 방식 (HEX 또는 BASE64) */
    private String encoding = "HEX";

    /** 비밀번호 솔트 사용 여부 */
    private boolean passwordSaltEnabled = false;

    /** 비밀번호 솔트 값 */
    private String passwordSalt = "";

    @PostConstruct
    public void init() {
        log.info("[PasswordXmlProperties] Loading password configuration from: {}", CONFIG_PATH);
        try {
            ClassPathResource resource = new ClassPathResource(CONFIG_PATH);
            if (!resource.exists()) {
                log.warn("[PasswordXmlProperties] Config file not found: {}. Using defaults.", CONFIG_PATH);
                return;
            }

            // XXE(XML External Entity) 공격 방지 설정
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(resource.getInputStream());
            doc.getDocumentElement().normalize();

            String algo = getTextContent(doc, "algorithm");
            if (!algo.isEmpty()) {
                this.algorithm = normalizeAlgorithm(algo);
            }

            String enc = getTextContent(doc, "encoding");
            if (!enc.isEmpty()) {
                this.encoding = enc.toUpperCase().trim();
            }

            String saltEnabledVal = getTextContent(doc, "password-salt-enabled");
            if (!saltEnabledVal.isEmpty()) {
                this.passwordSaltEnabled = Boolean.parseBoolean(saltEnabledVal);
            }

            this.passwordSalt = getTextContent(doc, "password-salt");

            log.info("[PasswordXmlProperties] Loaded - algorithm: {}, encoding: {}, passwordSaltEnabled: {}",
                    this.algorithm, this.encoding, this.passwordSaltEnabled);

            if (this.passwordSaltEnabled && this.passwordSalt.isBlank()) {
                log.warn("[PasswordXmlProperties] password-salt-enabled=true 이지만 password-salt 값이 비어 있습니다. " +
                         "password-config.xml 에 <password-salt> 값을 설정하세요.");
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "비밀번호 설정 XML 파일 로드 실패 (" + CONFIG_PATH + "): " + e.getMessage(), e);
        }
    }

    /**
     * XML에 입력된 알고리즘 이름을 Java MessageDigest 표준 이름으로 변환합니다.
     * <pre>
     *   SHA256  → SHA-256
     *   SHA512  → SHA-512
     *   SHA-256 → SHA-256 (그대로)
     *   SHA-512 → SHA-512 (그대로)
     * </pre>
     */
    private String normalizeAlgorithm(String algo) {
        String normalized = algo.toUpperCase().replace("-", "").trim();
        return switch (normalized) {
            case "SHA256" -> "SHA-256";
            case "SHA512" -> "SHA-512";
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 알고리즘: '" + algo + "'. 사용 가능한 값: SHA256, SHA512");
        };
    }

    private String getTextContent(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return "";
    }

    // ── Getters ─────────────────────────────────────────────────────────────────

    public String getAlgorithm() {
        return algorithm;
    }

    public String getEncoding() {
        return encoding;
    }

    public boolean isPasswordSaltEnabled() {
        return passwordSaltEnabled;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }
}
