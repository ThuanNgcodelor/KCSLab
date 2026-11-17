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
    @Test void onlyAdminCanDecideAndTechnicianCannotPublishOrRevoke() throws Exception {
        for (String action : new String[]{"publish", "revoke", "return"}) {
            mvc.perform(post("/api/v1/result-releases/1/" + action).with(csrf())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/result-releases/1/" + action).with(csrf())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
                    .andExpect(status().isForbidden());
        }
    }
    @Test void receptionistCannotPrepareDossiersAndUnknownDecisionPathIsDenied() throws Exception {
        for (String action : new String[]{"submit", "revisions"}) {
            mvc.perform(post("/api/v1/result-releases/1/" + action).with(csrf())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(put("/api/v1/result-releases/1/report").with(csrf()).contentType("application/json").content("{}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/result-releases/1/decision").with(csrf())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }
    @Test void receptionistAndAnonymousCannotListOrDownload() throws Exception {
        mvc.perform(get("/api/v1/result-releases").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/result-releases/1/files/1/content")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/result-releases/1")).andExpect(status().isUnauthorized());
    }
    @Test void receptionistCannotCreateDraft() throws Exception {
        mvc.perform(post("/api/v1/result-releases").with(csrf()).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
                .andExpect(status().isForbidden());
    }
}
