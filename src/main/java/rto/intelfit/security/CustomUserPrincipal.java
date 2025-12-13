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

    // ✅ 이거 없어서 터진 거임
    public Long getId() {
        return userPk;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return String.valueOf(userPk);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return emailVerified; }
}
