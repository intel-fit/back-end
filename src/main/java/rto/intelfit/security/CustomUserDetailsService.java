package rto.intelfit.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import rto.intelfit.domain.User;
import rto.intelfit.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userPkString) throws UsernameNotFoundException {
        // JWT 토큰에서 추출한 userPk (Long ID)를 사용하여 사용자 조회
        try {
            Long userPk = Long.parseLong(userPkString);
            User user = userRepository.findById(userPk)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userPk));
            return CustomUserPrincipal.create(user);
        } catch (NumberFormatException e) {
            // userPk가 숫자가 아닌 경우 userId로 시도 (하위 호환성)
            User user = userRepository.findByUserId(userPkString)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userPkString));
            return CustomUserPrincipal.create(user);
        }
    }
}
