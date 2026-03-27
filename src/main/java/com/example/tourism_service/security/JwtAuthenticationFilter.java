package com.example.tourism_service.security;

import com.example.tourism_service.entity.SessionStatus;
import com.example.tourism_service.entity.UserSession;
import com.example.tourism_service.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserSessionRepository sessionRepository; // Добавляем репозиторий для проверки статуса

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            if (tokenProvider.validateToken(token)) {
                username = tokenProvider.getUsernameFromToken(token);
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            /* ЛОГИКА ПРОВЕРКИ СЕССИИ (Чтобы старый токен не работал):
               Мы ищем в базе сессию, которой принадлежит этот access-токен.
               Если статус сессии USED или REVOKED — мы НЕ пускаем пользователя.
            */
            var sessionOpt = sessionRepository.findByAccessToken(token);

            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();

                // Если сессия уже была использована для рефреша или отозвана
                if (session.getStatus() != SessionStatus.ACTIVE) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Token is no longer valid (session expired or refreshed)");
                    return; // Прерываем цепочку, пользователь не авторизован
                }

                // Если сессия ACTIVE, продолжаем стандартную загрузку пользователя
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // Если мы храним access-токены в БД, и токена там нет — тоже 401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}