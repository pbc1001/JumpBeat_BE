package com.jumpbeat

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthApiTests(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `health API returns status up`() {
        mockMvc.get("/api/v1/health")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("UP") }
            }
    }
}
