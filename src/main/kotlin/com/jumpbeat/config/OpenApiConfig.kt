package com.jumpbeat.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun jumpBeatOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("JumpBeat API")
                .description("노래를 들으며 가사를 입력하는 리듬형 타자 연습 서비스 API")
                .version("v1"),
        )
}
