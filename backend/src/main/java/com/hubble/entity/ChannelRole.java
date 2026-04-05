package com.hubble.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "channel_roles")
@IdClass(ChannelRoleId.class)
public class ChannelRole {
    @Id
    @Column(name = "channel_id", nullable = false)
    UUID channelId;

    @Id
    @Column(name = "role_id", nullable = false)
    UUID roleId;
}
