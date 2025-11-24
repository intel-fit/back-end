package rto.intelfit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rto.intelfit.domain.SocialAccount;

import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderId(
            SocialAccount.Provider provider,
            String providerId
    );

    boolean existsByProviderAndProviderId(
            SocialAccount.Provider provider,
            String providerId
    );
}