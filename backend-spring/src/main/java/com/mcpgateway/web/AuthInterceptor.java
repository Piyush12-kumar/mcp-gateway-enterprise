package com.mcpgateway.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            String path = request.getRequestURI();
            String method = request.getMethod();
            // allow open user creation and schema endpoints
            if (path.equals("/users") && "POST".equalsIgnoreCase(method)) return true;
            if (path.startsWith("/schema")) return true;

            String uid = request.getHeader("X-User-Id");
            String oid = request.getHeader("X-Org-Id");
            if (uid == null || oid == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Missing authentication headers: X-User-Id and X-Org-Id");
                return false;
            }
            try { RequestContext.setUserId(Long.valueOf(uid)); } catch (Exception e) { RequestContext.setUserId(null); }
            try { RequestContext.setOrgId(Long.valueOf(oid)); } catch (Exception e) { RequestContext.setOrgId(null); }
            return true;
        } catch (Exception ex) {
            RequestContext.clear();
            return true;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        RequestContext.clear();
    }
}
