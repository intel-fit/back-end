package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rto.intelfit.domain.User;
import rto.intelfit.domain.TempMealDomain.TempMealBundle;

import java.util.Optional;

public interface TempBundleRepo extends JpaRepository<TempMealBundle, Long> {
    Optional<TempMealBundle> findByUser(User user);
}
