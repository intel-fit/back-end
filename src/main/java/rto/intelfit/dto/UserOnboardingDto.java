package rto.intelfit.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import rto.intelfit.domain.User;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOnboardingDto {

    @NotNull
    private LocalDate birthDate;

    @NotNull
    private Boolean agreePrivacy;

    @NotNull
    private Boolean agreeTerms;

    @NotNull
    private User.Gender gender;

    @NotNull
    @Min(100) @Max(250)
    private Integer height;

    @NotNull
    @Min(30) @Max(200)
    private Integer weight;

    @NotNull
    @Min(30) @Max(200)
    private Integer weightGoal;

    @NotNull
    private User.HealthGoal healthGoal;

    @Size(max = 20)
    private String workoutDaysPerWeek;
}
