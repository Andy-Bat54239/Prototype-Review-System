package com.auca.prbs.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StubController.class)
@Import({JwtAuthenticationFilter.class, SecurityConfig.class, JwtTokenProvider.class})
@TestPropertySource(properties = "jwt.secret=prbs-local-dev-secret-must-be-at-least-32-chars")
class JwtAuthenticationFilterTest {

    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider jwt;

    @Test
    void validToken_returns200() throws Exception {
        String token = jwt.generateAccessToken(1L, "STUDENT");

        mvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void missingToken_returns401() throws Exception {
        mvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRole_returns403() throws Exception {
        String studentToken = jwt.generateAccessToken(1L, "STUDENT");

        mvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void authRoutes_arePublic() throws Exception {
        mvc.perform(post("/api/v1/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@university.ac.rw\"}"))
                .andExpect(status().isOk());
    }
}
