package com.financial.transfer.concurrency;

import com.financial.transfer.config.CachingRequestBodyFilter;
import com.financial.transfer.util.HashUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;

/**
 * Utilitário para extrair o corpo da requisição e calcular o hash SHA-256
 * para uso no mecanismo de idempotência.
 */
public final class IdempotencyContext {

    private IdempotencyContext() {}

    /**
     * Extrai o corpo raw da requisição como String.
     * Requer que o CachingRequestBodyFilter tenha processado a requisição.
     */
    public static String extrairBody(HttpServletRequest request) {
        if (request instanceof CachingRequestBodyFilter.CachedBodyHttpServletRequest cached) {
            return new String(cached.getCachedBody(), StandardCharsets.UTF_8);
        }
        // Fallback: tenta ler normalmente (pode falhar se já foi lido)
        try {
            return new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static String calcularHash(HttpServletRequest request) {
        return HashUtil.sha256(extrairBody(request));
    }
}
