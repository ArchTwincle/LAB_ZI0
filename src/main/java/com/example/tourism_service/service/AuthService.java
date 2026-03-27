package com.example.tourism_service.service;

import com.example.tourism_service.dto.JwtResponse;
import com.example.tourism_service.entity.SessionStatus;
import com.example.tourism_service.entity.UserSession;
import com.example.tourism_service.repository.UserSessionRepository;
import com.example.tourism_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider tokenProvider;
    private final UserSessionRepository sessionRepository;

    @Transactional
    public JwtResponse login(String username) {
        // Генерируем новую пару токенов
        String accessToken = tokenProvider.generateAccessToken(username);
        String refreshToken = tokenProvider.generateRefreshToken(username);

        // Сохраняем сессию. Теперь мы сохраняем и accessToken тоже!
        UserSession session = UserSession.builder()
                .userEmail(username)
                .accessToken(accessToken) // Это позволит фильтру "убивать" старый access
                .refreshToken(refreshToken)
                .refreshTokenExpiry(Instant.now().plus(7, ChronoUnit.DAYS))
                .status(SessionStatus.ACTIVE)
                .build();

        sessionRepository.save(session);

        return new JwtResponse(accessToken, refreshToken);
    }

    @Transactional
    public JwtResponse refresh(String oldRefreshToken) {
        // 1. Проверка валидности JWT (подпись и время)
        if (!tokenProvider.validateToken(oldRefreshToken)) {
            throw new RuntimeException("Refresh токен невалиден или просрочен");
        }

        // 2. Поиск сессии в БД
        UserSession session = sessionRepository.findByRefreshToken(oldRefreshToken)
                .orElseThrow(() -> new RuntimeException("Сессия не найдена в базе данных"));

        // 3. Reuse Detection (Защита от повторного использования)
        if (session.getStatus() == SessionStatus.USED) {
            // Если кто-то пытается использовать уже обновленный токен — это признак взлома
            session.setStatus(SessionStatus.REVOKED);
            sessionRepository.save(session);
            throw new RuntimeException("Обнаружено повторное использование токена! Доступ заблокирован.");
        }

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new RuntimeException("Эта сессия больше не активна");
        }

        // 4. "Убиваем" текущую сессию
        session.setStatus(SessionStatus.USED);
        sessionRepository.save(session);

        // 5. Создаем новую сессию (новую пару токенов)
        return login(session.getUserEmail());
    }
}