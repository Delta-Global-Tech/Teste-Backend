package com.financial.transfer.util;

public final class CpfUtil {

    private CpfUtil() {}

    /**
     * Mascara o CPF: "11122233344" → "111.***.***.44"
     */
    public static String mascarar(String cpf) {
        if (cpf == null || cpf.length() != 11) return "***.***.***-**";
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
    }
}
