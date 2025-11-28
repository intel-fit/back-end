package rto.intelfit.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "에러 응답")
public class ErrorResponse {

    @Schema(description = "성공 여부", example = "false")
    private boolean success;

    @Schema(description = "에러 코드", example = "USER_001")
    private String code;

    @Schema(description = "에러 메시지", example = "이미 사용중인 아이디입니다")
    private String message;
}