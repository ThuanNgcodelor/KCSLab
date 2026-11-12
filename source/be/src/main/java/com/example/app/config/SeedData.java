package com.example.app.config;

import com.example.app.user.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedData {
    @Bean
    CommandLineRunner seedAdmin(UserRepository repository, PasswordEncoder encoder,
                                @Value("${app.seed-admin.email:}") String email,
                                @Value("${app.seed-admin.password:}") String password) {
        return args -> {
            if (email.isBlank() || password.isBlank() || repository.existsByEmail(email)) return;
            repository.save(new User(email.toLowerCase(), "Quản trị viên", encoder.encode(password), Role.ADMIN));
        };
    }
}
