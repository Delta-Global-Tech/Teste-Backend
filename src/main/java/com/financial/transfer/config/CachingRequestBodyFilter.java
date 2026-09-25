package com.financial.transfer.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * Filtro que armazena o corpo da requisição em memória para permitir
 * leitura múltipla (necessário para idempotência: o body é lido
 * para calcular o hash SHA-256 e depois novamente pelo Jackson).
 */
@Component
@Order(1)
public class CachingRequestBodyFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            chain.doFilter(new CachedBodyHttpServletRequest(httpRequest), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    public static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override public int read() { return byteStream.read(); }
                @Override public boolean isFinished() { return byteStream.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener rl) {}
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }

        public byte[] getCachedBody() {
            return cachedBody;
        }
    }
}
