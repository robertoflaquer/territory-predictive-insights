import java.util.List;

/**
 * Previsao por COMPARACAO: compara o periodo atual (ultimas P leituras) com o periodo
 * anterior (as P leituras antes dessas) e projeta a mesma variacao para o proximo periodo.
 * Atributos proprios: tamanhoPeriodo (o P) e o resultado da ultima comparacao.
 */
public class PrevisaoComparacao extends EstrategiaPrevisao {

    public static final int PERIODO_MINIMO = 3;
    public static final int PERIODO_MAXIMO = 10;
    private static final double LIMITE_ESTABILIDADE = 0.05; // variacao de ate 5% = estavel

    private final int tamanhoPeriodo;
    private double ultimaMediaAtual;
    private double ultimaMediaAnterior;
    private String ultimaTendencia = "sem calculo";

    public PrevisaoComparacao(int tamanhoPeriodo) {
        // precisa de dois periodos completos: o atual e o anterior
        super("Comparacao de Periodos", 2 * validarPeriodo(tamanhoPeriodo));
        this.tamanhoPeriodo = tamanhoPeriodo;
    }

    private static int validarPeriodo(int periodo) {
        if (periodo < PERIODO_MINIMO || periodo > PERIODO_MAXIMO) {
            throw new IllegalArgumentException("O periodo da comparacao deve ficar entre "
                    + PERIODO_MINIMO + " e " + PERIODO_MAXIMO + " leituras (recebido: " + periodo + ").");
        }
        return periodo;
    }

    public int getTamanhoPeriodo() {
        return tamanhoPeriodo;
    }

    public String getUltimaTendencia() {
        return ultimaTendencia;
    }

    @Override
    public double prever(List<Double> leituras) {
        super.exigirLeiturasSuficientes(leituras); // regra da classe mae
        ultimaMediaAtual = mediaDoTrecho(leituras, tamanhoPeriodo, 0);
        ultimaMediaAnterior = mediaDoTrecho(leituras, tamanhoPeriodo, tamanhoPeriodo);
        double variacao = ultimaMediaAtual - ultimaMediaAnterior;

        double variacaoRelativa;
        if (ultimaMediaAnterior > 0) {
            variacaoRelativa = variacao / ultimaMediaAnterior;
        } else {
            variacaoRelativa = (variacao > 0) ? 1.0 : 0.0;
        }
        if (variacaoRelativa > LIMITE_ESTABILIDADE) {
            ultimaTendencia = "em alta";
        } else if (variacaoRelativa < -LIMITE_ESTABILIDADE) {
            ultimaTendencia = "em queda";
        } else {
            ultimaTendencia = "estavel";
        }
        // projeta a mesma variacao para o proximo periodo (nunca abaixo de zero)
        return Math.max(0.0, ultimaMediaAtual + variacao);
    }

    @Override
    public String descreverMetodo() {
        return super.descreverMetodo() + " - ultimas " + tamanhoPeriodo
                + " leituras contra as " + tamanhoPeriodo + " anteriores";
    }

    @Override
    public String explicarUltimoCalculo() {
        return "periodo atual " + Formato.numero(ultimaMediaAtual) + " contra periodo anterior "
                + Formato.numero(ultimaMediaAnterior) + " (tendencia " + ultimaTendencia
                + "), mesma variacao projetada para o proximo periodo";
    }
}
