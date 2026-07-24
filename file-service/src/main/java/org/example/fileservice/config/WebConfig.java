package org.example.fileservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-path:./picture}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /pictures/** 映射到 uploadPath 目录
        String absolutePath = new File(uploadPath).getAbsolutePath() + File.separator;
        registry.addResourceHandler("/pictures/**")
                .addResourceLocations("file:" + absolutePath);
    }
}
