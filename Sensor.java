/**
 * INTERFACE: contrato que todo sensor do sistema precisa cumprir.
 * Ela so diz O QUE o sensor faz; cada classe decide COMO faz.
 */
public interface Sensor {

    // identificadores dos tipos de sensor (usados como chave dos limiares)
    String TIPO_CHUVA = "CHUVA";
    String TIPO_NIVEL_RIO = "NIVEL_RIO";
    String TIPO_UMIDADE_SOLO = "UMIDADE_SOLO";

    /** Gera (simula) uma nova leitura. Lanca IllegalStateException se o sensor estiver inativo. */
    double lerValor();

    /** Diz se o sensor esta em condicoes de medir. */
    boolean verificarFuncionamento();

    /** Retorna o tipo do sensor (uma das constantes TIPO_*). */
    String getTipo();

    String getId();

    String getNome();

    /** Unidade de medida, por exemplo "mm/h". */
    String getUnidade();

    /** Menor valor fisicamente possivel para este sensor. */
    double getFaixaMinima();

    /** Maior valor fisicamente possivel para este sensor. */
    double getFaixaMaxima();

    double getLimiteAlerta();

    void setLimiteAlerta(double limite);

    /**
     * Define a intensidade do cenario simulado, de 0 (tempo normal) a 1 (tempestade).
     * Cada sensor traduz isso para a sua propria escala.
     */
    void setIntensidadeCenario(double intensidade);

    /** Detecta quando um valor atinge ou passa do limite de alerta do sensor. */
    default boolean ultrapassouLimite(double valor) {
        return valor >= getLimiteAlerta();
    }
}
