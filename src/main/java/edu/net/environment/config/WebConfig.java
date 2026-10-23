package edu.net.environment.config;

import edu.net.environment.model.ApiException;
import edu.net.environment.model.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import edu.net.environment.mapper.EnvironmentMapper;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final EnvironmentMapper mapper;
    public WebConfig(EnvironmentMapper mapper) { this.mapper = mapper; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
                String path = req.getRequestURI();
                if (path.equals("/api/health") || path.equals("/api/auth/login")
                    || path.equals("/api/auth/register") || path.startsWith("/api/screen/")) return true;
                var session = req.getSession(false);
                var user = session == null ? null : (SessionUser) session.getAttribute("user");
                if (user == null) throw new ApiException(401, "请先登录");
                if (!user.role().equals("admin") && mapper.activeAccount(user.role(), user.id()) == 0) {
                    session.invalidate();
                    throw new ApiException(401, "账号已停用，请联系管理员");
                }
                String role = path.startsWith("/api/admin/") ? "admin"
                    : path.startsWith("/api/grid/") ? "grid"
                    : path.startsWith("/api/supervisor/") ? "supervisor" : null;
                if (role != null && !role.equals(user.role())) throw new ApiException(403, "没有此操作权限");
                // Same-origin POST/PUT/DELETE requests only; no permissive cross-origin CORS.
                if (!req.getMethod().equals("GET") && !req.getMethod().equals("HEAD")) {
                    String origin = req.getHeader("Origin");
                    String host = req.getHeader("Host");
                    if (origin != null && !origin.equals(req.getScheme() + "://" + host))
                        throw new ApiException(403, "请求来源不正确");
                }
                return true;
            }
        }).addPathPatterns("/api/**");
    }
}
