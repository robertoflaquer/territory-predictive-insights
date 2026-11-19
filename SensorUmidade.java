import java.util.Random;

/**
 * Sensor de umidade do solo, em porcentagem (0 a 100).
 * Solo encharcado nao absorve mais chuva e aumenta o risco de alagamento.
 */
public class SensorUmidade extends ComponenteTerritorial implements Sensor {

    private static final double FAIXA_MINIMA = 0.0;
    private static final double FAIXA_MAXIMA = 100.0;
    private static final double UMIDADE_NORMAL = 40.0;
    private static final double RUIDO = 3.0;
    private static final double ENCHARCAMENTO_CENARIO = 9.0; // pontos por leitura em tempestade

    private final Random random;
    private double valorAtual = UMIDADE_NORMAL;
    private double limiteAlerta = 85.0;
    private double intensidadeCenario = 0.0;

    public SensorUmidade(String id, String regiao, Random random) {
        super(id, "Sensor de Umidade do Solo", regiao);
        this.random = random;
    }

    @Override
    public double lerValor() {
        if (!verificarFuncionamento()) {
            throw new IllegalStateException(getNome() + " esta inativo e nao pode medir.");
        }
        double ruido = (random.nextDouble() - 0.5) * RUIDO;
        double secagem = 0.15 * (UMIDADE_NORMAL - valorAtual); // solo seca devagar
        double encharcamento = ENCHARCAMENTO_CENARIO * intensidadeCenario;
        double novo = valorAtual + secagem + encharcamento + ruido;
        valorAtual = Math.round(Math.max(FAIXA_MINIMA, Math.min(FAIXA_MAXIMA, novo)) * 100.0) / 100.0;
        return valorAtual;
    }

    @Override
    public boolean verificarFuncionamento() {
        return estaAtivo();
    }

    @Override
    public String getTipo() {
        return TIPO_UMIDADE_SOLO;
    }

    @Override
    public String getUnidade() {
        return "%";
    }

    @Override
    public double getFaixaMinima() {
        return FAIXA_MINIMA;
    }

    @Override
    public double getFaixaMaxima() {
        return FAIXA_MAXIMA;
    }

    @Override
    public double getLimiteAlerta() {
        return limiteAlerta;
    }

    @Override
    public void setLimiteAlerta(double limite) {
        if (limite < FAIXA_MINIMA || limite > FAIXA_MAXIMA) {
            throw new IllegalArgumentException("Limite de umidade deve estar entre "
                    + FAIXA_MINIMA + " e " + FAIXA_MAXIMA + " %.");
        }
        this.limiteAlerta = limite;
    }

    @Override
    public void setIntensidadeCenario(double intensidade) {
        if (intensidade < 0.0 || intensidade > 1.0) {
            throw new IllegalArgumentException("A intensidade do cenario deve estar entre 0 e 1.");
        }
        this.intensidadeCenario = intensidade;
    }

    @Override
    public String verificarStatus() {
        return getNome() + " [" + getId() + "] " + getStatus()
                + " | ultimo valor: " + Formato.numero(valorAtual) + " " + getUnidade()
                + " | limite de alerta: " + Formato.numero(limiteAlerta) + " " + getUnidade();
    }
}
