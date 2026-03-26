package com.hubble.repository;

import com.hubble.entity.UserSettings;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {

    @Modifying
    @Query("""
            update UserSettings settings
               set settings.theme = :theme,
                   settings.updatedAt = :updatedAt
             where settings.userId = :userId
            """)
    int updateThemeByUserId(@Param("userId") UUID userId,
                            @Param("theme") String theme,
                            @Param("updatedAt") LocalDateTime updatedAt);
}
