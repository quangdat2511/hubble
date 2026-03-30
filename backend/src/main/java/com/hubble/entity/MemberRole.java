package com.hubble.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "member_roles")
@IdClass(MemberRole.MemberRoleId.class)
public class MemberRole {

    @Id
    @Column(name = "member_id", nullable = false)
    UUID memberId;

    @Id
    @Column(name = "role_id", nullable = false)
    UUID roleId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class MemberRoleId implements Serializable {
        UUID memberId;
        UUID roleId;
    }
}
