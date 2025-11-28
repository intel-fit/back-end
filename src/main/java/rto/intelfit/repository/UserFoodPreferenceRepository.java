package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rto.intelfit.domain.User;
import rto.intelfit.domain.UserFoodPreference;

import java.util.Optional;

public interface UserFoodPreferenceRepository extends JpaRepository<UserFoodPreference, Long> {

    Optional<UserFoodPreference> findByUser(User user);

    boolean existsByUser(User user);
    void deleteAllByUser(User user);

}
