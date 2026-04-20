package com.hubble.service;

import com.hubble.entity.User;
import com.hubble.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyAvatarMigrationRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AvatarStorageService avatarStorageService;

    @Override
    public void run(ApplicationArguments args) {
        List<User> legacyUsers = userRepository.findAllByAvatarUrlStartingWith(AvatarStorageService.LEGACY_AVATAR_URL_PREFIX);
        if (legacyUsers.isEmpty()) {
            return;
        }

        for (User user : legacyUsers) {
            String avatarUrl = user.getAvatarUrl();
            Path legacyPath = avatarStorageService.resolveLegacyPath(avatarUrl);
            if (legacyPath == null) {
                continue;
            }

            try {
                String migratedUrl = avatarStorageService.migrateLegacyAvatar(user.getId(), legacyPath);
                user.setAvatarUrl(migratedUrl);
                userRepository.save(user);
                avatarStorageService.deleteLegacyAvatarQuietly(avatarUrl);
            } catch (Exception e) {
                log.warn("Could not migrate legacy avatar for user {}", user.getId(), e);
            }
        }
    }
}
