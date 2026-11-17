package com.example.app.results;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ResultReleaseSecurityTest {
    @Autowired private WebApplicationContext context;
    private MockMvc mvc;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test void receptionistCannotReadDrafts() throws Exception {
        mvc.perform(get("/api/v1/result-releases/1").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
                .andExpect(status().isForbidden());
    }
    @Test void noRoleCanPublishWhileGateIsUnavailable() throws Exception {
        for (String role : new String[]{"ROLE_ADMIN", "ROLE_TECHNICIAN"}) {
            mvc.perform(post("/api/v1/result-releases/1/decision").with(csrf()).with(jwt().authorities(new SimpleGrantedAuthority(role))))
                    .andExpect(status().isForbidden());
        }
    }
    @Test void onlyTechnicianCanCreateDraft() throws Exception {
        mvc.perform(post("/api/v1/result-releases").with(csrf()).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }
}
