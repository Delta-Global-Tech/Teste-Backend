package teste.delta.dto.response;

/**
 * Resposta já serializada, para que um reenvio com a mesma Idempotency-Key devolva exatamente o mesmo corpo.
 */
public record RespostaIdempotente(int status, String json) {}
