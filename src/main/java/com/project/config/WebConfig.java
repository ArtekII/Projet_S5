package com.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AuthInterceptor authInterceptor;
  private final UiFeedbackInterceptor uiFeedbackInterceptor;

  public WebConfig(AuthInterceptor a, UiFeedbackInterceptor uiFeedbackInterceptor) {
    this.authInterceptor = a;
    this.uiFeedbackInterceptor = uiFeedbackInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(uiFeedbackInterceptor)
        .addPathPatterns("/manager/**", "/rh/**", "/finance/**", "/direction/**", "/candidat/**", "/candidatures/**");
    registry.addInterceptor(authInterceptor)
        .addPathPatterns("/manager/**", "/rh/**", "/finance/**", "/direction/**", "/candidat/**",
                "/espaceCandidat", "/dossier", "/dossier/**", "/espaceEmploye", "/espaceEmploye/**")
        .excludePathPatterns("/login", "/logout", "/css/**", "/h2-console/**");
  }
}
