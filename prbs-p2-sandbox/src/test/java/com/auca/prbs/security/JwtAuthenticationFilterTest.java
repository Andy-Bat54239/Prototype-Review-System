package com.auca.prbs.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StubController.class)
@Import({JwtAuthenticationFilter.class, SecurityConfig.class, JwtTokenProvider.class, RateLimitFilter.class})
@TestPropertySource(properties = {
        "jwt.secret=prbs-local-dev-secret-must-be-at-least-32-chars",
        "cors.allowed-origins=http://localhost:5173"
})
class JwtAuthenticationFilterTest {

    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider jwt;
    @Autowired RateLimitFilter rateLimitFilter;

    @BeforeEach
    void clearRateLimiter() {
        // Filter is a shared bean; counters accumulate across tests otherwise.
        rateLimitFilter.reset();
    }

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

    @Test
    void corsPreflight_fromAllowedOrigin_returnsAccessControlHeaders() throws Exception {
        mvc.perform(options("/api/v1/bookings")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void corsPreflight_fromDisallowedOrigin_isRejected() throws Exception {
        mvc.perform(options("/api/v1/bookings")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elevenRapidAuthRequests_fromSameClient_429sTheLastOne() throws Exception {
        for (int i = 0; i < RateLimitFilter.MAX_REQUESTS_PER_MINUTE; i++) {
            mvc.perform(post("/api/v1/auth/send-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        mvc.perform(post("/api/v1/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void rateLimiter_doesNotApplyToNonAuthRoutes() throws Exception {
        String token = jwt.generateAccessToken(1L, "STUDENT");

        // Hit /bookings 15 times — well past the auth-route limit — none should 429.
        for (int i = 0; i < 15; i++) {
            mvc.perform(get("/api/v1/bookings")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }
}
