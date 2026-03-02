package com.idstory.common.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * MVC 다국어(i18n) 설정 클래스
 *
 * <ul>
 *   <li>기본 로케일: 한국어 (ko)</li>
 *   <li>세션 기반 로케일 저장</li>
 *   <li>URL 파라미터 ?lang=ko / ?lang=en 으로 언어 전환</li>
 *   <li>메세지 파일: resources/messages/messages*.properties</li>
 * </ul>
 *
 * <p>메세지 소스(MessageSource)는 application.yml 의
 * spring.messages 설정으로 Spring Boot 가 자동 구성합니다.</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 세션 기반 로케일 리졸버
     * - 사용자가 선택한 언어를 세션에 저장
     * - 기본값: 한국어(ko)
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.KOREAN);
        return resolver;
    }

    /**
     * URL 파라미터로 언어를 변경하는 인터셉터
     * - 사용 예: /login?lang=ko  → 한국어
     * - 사용 예: /login?lang=en  → 영어
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
