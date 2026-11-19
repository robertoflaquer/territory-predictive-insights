import java.util.Random;

/**
 * Sensor de chuva (pluviometro). Mede a intensidade da chuva em mm/h.
 * E um ComponenteTerritorial (extends) e cumpre o contrato Sensor (implements).
 */
public class SensorChuva extends ComponenteTerritorial implements Sensor {

    private static final double FAIXA_MINIMA = 0.0;
    private static final double FAIXA_MAXIMA = 200.0;
    private static final double RUIDO = 4.0;                 // variacao aleatoria de cada leitura
    private static final double CHUVA_MAXIMA_CENARIO = 18.0; // empurrao por leitura em tempestade

    private final Random random;
    private double valorAtual = 2.0;
    private double limiteAlerta = 50.0;
    private double intensidadeCenario = 0.0;

    public SensorChuva(String id, String regiao, Random random) {
        super(id, "Sensor de Chuva", regiao);
        this.random = random;
    }

    @Override
    public double lerValor() {
        if (!verificarFuncionamento()) {
            throw new IllegalStateException(getNome() + " esta inativo e nao pode medir.");
        }
        double ruido = (random.nextDouble() - 0.5) * RUIDO;
        double empurrao = CHUVA_MAXIMA_CENARIO * intensidadeCenario;
        // a chuva "esfria" rapido (x0.8) se nada a alimenta
        double novo = valorAtual * 0.8 + empurrao + ruido;
        valorAtual = Math.round(Math.max(FAIXA_MINIMA, Math.min(FAIXA_MAXIMA, novo)) * 100.0) / 100.0;
        return valorAtual;
    }

    @Override
    public boolean verificarFuncionamento() {
        return estaAtivo();
    }

    @Override
    public String getTipo() {
        return TIPO_CHUVA;
    }

    @Override
    public String getUnidade() {
        return "mm/h";
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
            throw new IllegalArgumentException("Limite de chuva deve estar entre "
                    + FAIXA_MINIMA + " e " + FAIXA_MAXIMA + " mm/h.");
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
