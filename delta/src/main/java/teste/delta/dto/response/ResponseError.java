package teste.delta.dto.response;

import java.time.OffsetDateTime;

public record ResponseError(
    OffsetDateTime timestamp,
    int status,
    String error,
    String codigo,
    String message,
    String path
) {

}
