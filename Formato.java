import java.util.Locale;

/**
 * Pequenos auxiliares de formatacao para o console (numeros no padrao brasileiro).
 * Os arquivos de historico NAO usam isso: la o decimal e sempre ponto.
 */
public final class Formato {

    private static final Locale PT_BR = new Locale("pt", "BR");

    private Formato() {
    }

    public static String numero(double valor) {
        return String.format(PT_BR, "%.1f", valor);
    }

    public static String inteiro(long valor) {
        return String.format(PT_BR, "%,d", valor);
    }
}
