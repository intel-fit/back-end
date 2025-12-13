package rto.intelfit.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import rto.intelfit.domain.User;

import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor
public class CustomUserPrincipal implements UserDetails {

    private Long userPk;          // DB PK 
    private String userId;        // business userId
    private String name;
    private String email;
    private String password;
    private boolean emailVerified;

    public static CustomUserPrincipal create(User user) {
        return new CustomUserPrincipal(
                user.getId(),
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getEmailVerified()
        );
    }

    public Long getId() {
        return userPk;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
<<<<<<< HEAD
        return userId; // ⭐ userPk 말고 userId 유지 (JWT/로그인 안정성)
=======
        return userId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
>>>>>>> 239c4a2 (D)
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return emailVerified;
    }
}
