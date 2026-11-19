/**
 * Um alerta emitido a partir de uma previsao. Guarda tudo que explica o POR QUE do alerta:
 * qual sensor, qual valor foi previsto, qual limite foi atingido e por qual metodo.
 * Objeto imutavel (so getters).
 */
public class Alerta {

    private final String sensorNome;
    private final String sensorId;
    private final String tipoSensor;
    private final String unidade;
    private final String regiao;
    private final double valorPrevisto;
    private final NivelAlerta nivel;
    private final double limiteAtingido;
    private final double[] escala; // {atencao, alerta, critico}
    private final String estrategiaNome;
    private final String calculo;
    private final String contexto;

    public Alerta(String sensorNome, String sensorId, String tipoSensor, String unidade, String regiao,
                  double valorPrevisto, NivelAlerta nivel, double limiteAtingido, double[] escala,
                  String estrategiaNome, String calculo, String contexto) {
        this.sensorNome = sensorNome;
        this.sensorId = sensorId;
        this.tipoSensor = tipoSensor;
        this.unidade = unidade;
        this.regiao = regiao;
        this.valorPrevisto = valorPrevisto;
        this.nivel = nivel;
        this.limiteAtingido = limiteAtingido;
        this.escala = escala.clone();
        this.estrategiaNome = estrategiaNome;
        this.calculo = calculo;
        this.contexto = contexto;
    }

    public NivelAlerta getNivel() {
        return nivel;
    }

    public String getSensorNome() {
        return sensorNome;
    }

    public String getSensorId() {
        return sensorId;
    }

    public double getValorPrevisto() {
        return valorPrevisto;
    }

    public double getLimiteAtingido() {
        return limiteAtingido;
    }

    public String getEstrategiaNome() {
        return estrategiaNome;
    }

    /** Resumo curto, usado no painel informativo. */
    public String resumo() {
        return nivel.getRotulo() + ": " + sensorNome + " previsto em " + Formato.numero(valorPrevisto)
                + " " + unidade + " (limite " + Formato.numero(limiteAtingido) + " " + unidade + ")";
    }

    /** Explicacao completa para o console: o que, por que, e uma sugestao de apoio a decisao. */
    public String explicar() {
        StringBuilder texto = new StringBuilder();
        texto.append(nivel.getMarcador()).append(' ').append(nivel.getRotulo())
                .append(" | ").append(sensorNome).append(" [").append(sensorId).append("]")
                .append(" | regiao: ").append(regiao).append('\n');
        texto.append("     Por que: a previsao por ").append(estrategiaNome).append(" indica ")
                .append(Formato.numero(valorPrevisto)).append(' ').append(unidade)
                .append(", que atinge o limite de ").append(nivel.getRotulo()).append(" (")
                .append(Formato.numero(limiteAtingido)).append(' ').append(unidade).append(").\n");
        texto.append("     Calculo: ").append(calculo).append(".\n");
        texto.append("     Escala do sensor: ATENCAO ").append(Formato.numero(escala[0]))
                .append(" | ALERTA ").append(Formato.numero(escala[1]))
                .append(" | CRITICO ").append(Formato.numero(escala[2]))
                .append(' ').append(unidade).append('\n');
        texto.append("     Contexto: ").append(contexto).append('\n');
        texto.append("     Sugestao: ").append(sugestao()).append('\n');
        texto.append("     Lembrete: a decisao e da coordenacao local; este alerta so apoia, nao decide.");
        return texto.toString();
    }

    private String sugestao() {
        switch (tipoSensor) {
            case Sensor.TIPO_CHUVA:
                switch (nivel) {
                    case ATENCAO:
                        return "acompanhar a evolucao da chuva e conferir se bueiros e canais dos pontos vulneraveis estao desobstruidos.";
                    case ALERTA:
                        return "avisar as equipes comunitarias, deixar abrigos e rotas seguras prontos e reforcar a limpeza de galerias.";
                    default:
                        return "avaliar acionar a Defesa Civil e comunicar as familias das areas de risco.";
                }
            case Sensor.TIPO_NIVEL_RIO:
                switch (nivel) {
                    case ATENCAO:
                        return "acompanhar a cota do rio a cada leitura e avisar quem mora perto das margens.";
                    case ALERTA:
                        return "preparar a retirada de pessoas e bens das margens e alinhar com a Defesa Civil.";
                    default:
                        return "avaliar iniciar o plano de evacuacao das margens e acionar a Defesa Civil.";
                }
            case Sensor.TIPO_UMIDADE_SOLO:
                switch (nivel) {
                    case ATENCAO:
                        return "solo ficando saturado: observar encostas e pontos onde a agua se acumula.";
                    case ALERTA:
                        return "solo saturado: qualquer chuva nova tende a escorrer e alagar; reforcar a vigilancia.";
                    default:
                        return "solo praticamente encharcado: risco alto de alagamento; avaliar pedir apoio externo.";
                }
            default:
                return "acompanhar a situacao e consultar a coordenacao local.";
        }
    }
}
