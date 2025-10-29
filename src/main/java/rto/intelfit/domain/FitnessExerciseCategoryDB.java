package rto.intelfit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(name = "exerciseCategory") // ✅ SQL 테이블명과 동일하게 맞춤
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FitnessExerciseCategoryDB {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ✅ SQL의 BIGINT AUTO_INCREMENT에 대응

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "body_part", length = 50)
    private String bodyPart;

    @Column(name = "target_muscle", length = 100)
    private String targetMuscle;

    @Column(name = "secondary_muscles", columnDefinition = "TEXT")
    private String secondaryMuscles;

    @Column(name = "equipment", length = 100)
    private String equipment;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;
}
