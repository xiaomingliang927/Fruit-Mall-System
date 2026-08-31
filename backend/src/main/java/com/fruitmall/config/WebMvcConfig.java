package com.fruitmall.config;

import com.fruitmall.admin.AdminInterceptor;
import com.fruitmall.auth.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /** 允许跨域的前端来源（逗号分隔）；留空则默认仅放行本地三端，避免使用通配符 + 凭证的危险组合 */
    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsCsv;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 上传图片的磁盘目录映射（/uploads/** 直接静态访问）
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + Paths.get(uploadDir).toAbsolutePath() + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // C 端接口鉴权
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",       // 登录
                        "/api/v1/banners",       // 首页轮播（匿名可见）
                        "/api/v1/categories/**", // 分类浏览
                        "/api/v1/coupons/list",  // 领券中心（匿名可见）
                        "/api/v1/products/**"    // 商品浏览
                );
        // 管理端接口鉴权（独立身份，与 C 端 token 不通用）
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        if (origins.isEmpty()) {
            // 默认放行本地三端与线上 IP 站点；正式域名通过 FRUIT_CORS_ORIGINS 注入
            origins = List.of("http://localhost:5173", "http://localhost:5174", "http://localhost:5175",
                    "http://116.62.60.53:5173", "http://116.62.60.53:5174");
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
