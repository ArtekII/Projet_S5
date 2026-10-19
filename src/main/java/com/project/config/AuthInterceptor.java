package com.project.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
    HttpSession session = req.getSession(false);
    if (session == null || session.getAttribute("userId") == null) {
      res.sendRedirect("/login");
      return false;
    }
    String role = (String) session.getAttribute("userRole");
    String path = req.getRequestURI();
    // controle role par prefixe
    if (path.startsWith("/manager") && !"MANAGER".equals(role)) {
      res.sendRedirect("/login?error=role");
      return false;
    }
    if (path.startsWith("/rh") && !"RH".equals(role)) {
      res.sendRedirect("/login?error=role");
      return false;
    }
    if (path.startsWith("/finance") && !"FINANCE".equals(role)) {
      res.sendRedirect("/login?error=role");
      return false;
    }
    if (path.startsWith("/direction") && !"DIRECTION".equals(role)) {
      res.sendRedirect("/login?error=role");
      return false;
    }
    if (path.startsWith("/candidat") && !"CANDIDAT".equals(role)) {
      res.sendRedirect("/login?error=role");
      return false;
    }
    return true;
  }
}
