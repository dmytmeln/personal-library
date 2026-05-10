package org.example.library.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.library.auth.domain.RefreshToken;
import org.example.library.auth.domain.RefreshToken_;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(name = "users_seq", sequenceName = "users_seq", allocationSize = 20)
    @Column(name = "user_id")
    private Integer id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @OneToMany(mappedBy = RefreshToken_.USER)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User user)) return false;
        return Objects.equals(id, user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
