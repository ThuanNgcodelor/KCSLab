package com.example.app.transfer.receiving;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ReceivingSecurityTest {
    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository users;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void receptionistCannotCallReceivingEndpoints() throws Exception {
        mvc.perform(get("/api/v1/receiving/dashboard")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void technicianCannotCallFullReceiptApi() throws Exception {
        mvc.perform(get("/api/v1/receipts/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotCallReceivingEndpoints() throws Exception {
        mvc.perform(get("/api/v1/receiving/transfers")).andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void technicianCanReachReceivingService() throws Exception {
        users.saveAndFlush(new User("kcs-security@example.test", "KCS", "unused", Role.TECHNICIAN));
        mvc.perform(get("/api/v1/receiving/transfers/0").with(jwt()
                        .jwt(token -> token.subject("kcs-security@example.test"))
                        .authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void adminCanReachReceivingService() throws Exception {
        users.saveAndFlush(new User("admin-security@example.test", "Admin", "unused", Role.ADMIN));
        mvc.perform(get("/api/v1/receiving/transfers/0").with(jwt()
                        .jwt(token -> token.subject("admin-security@example.test"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }
}
