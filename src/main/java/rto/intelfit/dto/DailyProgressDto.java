package rto.intelfit.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyProgressDto {
    private LocalDate date;
    private double exerciseRate;
    private double totalCalorie;
}
