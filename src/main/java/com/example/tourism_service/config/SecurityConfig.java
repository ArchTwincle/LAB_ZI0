package com.example.tourism_service.config;

import com.example.tourism_service.repository.UserSessionRepository;
import com.example.tourism_service.security.JwtAuthenticationFilter;
import com.example.tourism_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    // Репозиторий нужен для передачи в фильтр, чтобы проверять статус сессии в БД
    private final UserSessionRepository sessionRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Отключаем CSRF, так как используем JWT (Stateless)
                .csrf(csrf -> csrf.disable())

                // 2. Настраиваем сервер на работу без сохранения сессий в памяти
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 3. Правила доступа к эндпоинтам
                .authorizeHttpRequests(auth -> auth
                        // Разрешаем вход и обновление токенов всем
                        .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()

                        // Регистрация доступна только администратору
                        .requestMatchers("/api/auth/register").hasRole("ADMIN")

                        // Публичные и защищенные маршруты для туров
                        .requestMatchers(HttpMethod.GET, "/api/tours/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/tours/**").hasAnyRole("ADMIN", "GUIDE")

                        // Все остальные запросы требуют авторизации
                        .anyRequest().authenticated()
                );

        // 4. Добавляем JWT фильтр перед стандартным фильтром аутентификации.
        // Передаем ТРИ аргумента: провайдер, сервис пользователей и репозиторий сессий.
        http.addFilterBefore(
                new JwtAuthenticationFilter(tokenProvider, userDetailsService, sessionRepository),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}