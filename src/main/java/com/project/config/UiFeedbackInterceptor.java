package com.project.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

/** Preserve ordinary form values for one redirect after a business validation error. */
@Component
public class UiFeedbackInterceptor implements HandlerInterceptor {
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler, ModelAndView modelAndView) {
        if (!"POST".equals(request.getMethod()) || modelAndView == null
                || modelAndView.getViewName() == null || !modelAndView.getViewName().startsWith("redirect:")) return;
        var flash = RequestContextUtils.getOutputFlashMap(request);
        String retourOffre = request.getParameter("retourOffre");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/rh/") && retourOffre != null && retourOffre.matches("[1-9][0-9]{0,8}")) {
            modelAndView.setViewName("redirect:/rh/offre/" + retourOffre + "/candidats");
        }
        if (!flash.containsKey("error")) return;
        Map<String, List<String>> values = new LinkedHashMap<>();
        request.getParameterMap().forEach((name, entries) -> {
            if (!name.toLowerCase(java.util.Locale.ROOT).matches(".*(password|csrf|token|revision).*")
                    && entries.length <= 100 && Arrays.stream(entries).allMatch(value -> value.length() <= 10000)) {
                values.put(name, List.copyOf(Arrays.asList(entries)));
            }
        });
        flash.put("saisieFormulaire", values);
        flash.put("actionFormulaire", request.getRequestURI());
    }
}
