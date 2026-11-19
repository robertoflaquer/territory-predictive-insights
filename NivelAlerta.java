/**
 * Niveis de alerta, do mais leve ao mais grave.
 * A ordem das constantes importa: quanto maior o ordinal, mais grave o nivel.
 */
public enum NivelAlerta {
    NENHUM("NORMAL", "[ ]"),
    ATENCAO("ATENCAO", "[!]"),
    ALERTA("ALERTA", "[!!]"),
    CRITICO("CRITICO", "[!!!]");

    private final String rotulo;
    private final String marcador;

    NivelAlerta(String rotulo, String marcador) {
        this.rotulo = rotulo;
        this.marcador = marcador;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getMarcador() {
        return marcador;
    }

    public boolean maisGraveQue(NivelAlerta outro) {
        return this.ordinal() > outro.ordinal();
    }
}
