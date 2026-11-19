import java.util.List;

/**
 * Previsao por MEDIA MOVEL: o proximo valor sera a media das ultimas N leituras.
 * Atributo proprio: tamanhoJanela (o N).
 */
public class PrevisaoMediaMovel extends EstrategiaPrevisao {

    public static final int JANELA_MINIMA = 5;
    public static final int JANELA_MAXIMA = 20;

    private final int tamanhoJanela;
    private double ultimaMedia;

    public PrevisaoMediaMovel(int tamanhoJanela) {
        // super(...) monta a parte herdada: aqui o minimo de leituras e igual a janela
        super("Media Movel", validarJanela(tamanhoJanela));
        this.tamanhoJanela = tamanhoJanela;
    }

    private static int validarJanela(int janela) {
        if (janela < JANELA_MINIMA || janela > JANELA_MAXIMA) {
            throw new IllegalArgumentException("A janela da media movel deve ficar entre "
                    + JANELA_MINIMA + " e " + JANELA_MAXIMA + " leituras (recebido: " + janela + ").");
        }
        return janela;
    }

    public int getTamanhoJanela() {
        return tamanhoJanela;
    }

    @Override
    public double prever(List<Double> leituras) {
        super.exigirLeiturasSuficientes(leituras); // regra da classe mae
        ultimaMedia = mediaDoTrecho(leituras, tamanhoJanela, 0);
        return ultimaMedia;
    }

    @Override
    public String descreverMetodo() {
        return super.descreverMetodo() + " - media das ultimas " + tamanhoJanela + " leituras";
    }

    @Override
    public String explicarUltimoCalculo() {
        return "media das ultimas " + tamanhoJanela + " leituras = " + Formato.numero(ultimaMedia);
    }
}
