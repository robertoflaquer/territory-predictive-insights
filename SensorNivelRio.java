import java.util.Random;

/**
 * Sensor de nivel do rio (ou canal). Mede a altura da agua em metros.
 * O rio reage devagar: sobe aos poucos quando o cenario e de chuva forte.
 */
public class SensorNivelRio extends ComponenteTerritorial implements Sensor {

    private static final double FAIXA_MINIMA = 0.0;
    private static final double FAIXA_MAXIMA = 15.0;
    private static final double NIVEL_NORMAL = 1.2;          // para onde o rio "volta" sem chuva
    private static final double RUIDO = 0.1;
    private static final double SUBIDA_MAXIMA_CENARIO = 0.4; // metros por leitura em tempestade

    private final Random random;
    private double valorAtual = NIVEL_NORMAL;
    private double limiteAlerta = 3.5;
    private double intensidadeCenario = 0.0;

    public SensorNivelRio(String id, String regiao, Random random) {
        super(id, "Sensor de Nivel do Rio", regiao);
        this.random = random;
    }

    @Override
    public double lerValor() {
        if (!verificarFuncionamento()) {
            throw new IllegalStateException(getNome() + " esta inativo e nao pode medir.");
        }
        double ruido = (random.nextDouble() - 0.5) * RUIDO;
        double volta = 0.1 * (NIVEL_NORMAL - valorAtual); // tende ao nivel normal
        double subida = SUBIDA_MAXIMA_CENARIO * intensidadeCenario;
        double novo = valorAtual + volta + subida + ruido;
        valorAtual = Math.round(Math.max(FAIXA_MINIMA, Math.min(FAIXA_MAXIMA, novo)) * 100.0) / 100.0;
        return valorAtual;
    }

    @Override
    public boolean verificarFuncionamento() {
        return estaAtivo();
    }

    @Override
    public String getTipo() {
        return TIPO_NIVEL_RIO;
    }

    @Override
    public String getUnidade() {
        return "m";
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
            throw new IllegalArgumentException("Limite do nivel do rio deve estar entre "
                    + FAIXA_MINIMA + " e " + FAIXA_MAXIMA + " m.");
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
