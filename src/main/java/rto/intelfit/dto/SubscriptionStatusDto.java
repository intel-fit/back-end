package rto.intelfit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionStatusDto {
    private boolean hasActiveSubscription;
    private String status;
    private LocalDateTime currentPeriodEnd;
    private String provider;
    private String providerSubscriptionId;
}
