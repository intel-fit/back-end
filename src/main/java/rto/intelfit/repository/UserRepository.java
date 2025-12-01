package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUserId(String userId);

    boolean existsByEmail(String email);

    Optional<User> findByUserId(String userId);

    Optional<User> findByKakaoId(String kakaoId);

    Optional<User> findByEmail(String email);
}