package rto.intelfit.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EmailDomainValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmailDomain {
    String message() default "Gmail 또는 Naver 이메일만 사용 가능합니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}