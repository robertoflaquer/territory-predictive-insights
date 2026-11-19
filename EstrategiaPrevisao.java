import java.util.List;

/**
 * HERANCA: classe abstrata com o que TODA forma de prever tem em comum.
 * As subclasses herdam o nome, o minimo de leituras e os metodos auxiliares,
 * e so precisam implementar o que muda: o calculo (prever) e a explicacao.
 */
public abstract class EstrategiaPrevisao {

    private final String nome;
    private final int minimoLeituras;

    protected EstrategiaPrevisao(String nome, int minimoLeituras) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da estrategia de previsao nao pode ser vazio.");
        }
        if (minimoLeituras < 1) {
            throw new IllegalArgumentException("O minimo de leituras deve ser pelo menos 1.");
        }
        this.nome = nome.trim();
        this.minimoLeituras = minimoLeituras;
    }

    /** Calcula o valor previsto para a proxima leitura a partir do historico (da mais antiga a mais nova). */
    public abstract double prever(List<Double> leituras);

    /** Explica, em uma frase, como o ultimo valor foi calculado (usado nos alertas). */
    public abstract String explicarUltimoCalculo();

    public String getNome() {
        return nome;
    }

    public int getMinimoLeituras() {
        return minimoLeituras;
    }

    public boolean temLeiturasSuficientes(List<Double> leituras) {
        return leituras != null && leituras.size() >= minimoLeituras;
    }

    /** Descricao geral; as subclasses chamam este metodo com super.descreverMetodo() e acrescentam detalhes. */
    public String descreverMetodo() {
        return nome + " (minimo de " + minimoLeituras + " leituras)";
    }

    /** O sistema deve se calar quando ha poucas leituras: previsao com pouco dado engana. */
    protected void exigirLeiturasSuficientes(List<Double> leituras) {
        if (!temLeiturasSuficientes(leituras)) {
            int quantas = (leituras == null) ? 0 : leituras.size();
            throw new IllegalStateException(nome + " precisa de pelo menos " + minimoLeituras
                    + " leituras, mas ha apenas " + quantas + ".");
        }
    }

    /**
     * Media de um trecho das leituras: {@code quantidade} valores, terminando
     * {@code deslocamentoDoFim} posicoes antes da ultima leitura.
     */
    protected double mediaDoTrecho(List<Double> leituras, int quantidade, int deslocamentoDoFim) {
        int fim = leituras.size() - deslocamentoDoFim;
        int inicio = fim - quantidade;
        double soma = 0.0;
        for (int i = inicio; i < fim; i++) {
            soma += leituras.get(i);
        }
        return soma / quantidade;
    }
}
