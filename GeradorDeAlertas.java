import java.util.Optional;

/**
 * Transforma uma previsao em alerta, comparando o valor previsto com os limiares
 * guardados em DadosTerritorio. Se o valor esta abaixo de todos os limiares, nao ha alerta.
 */
public final class GeradorDeAlertas {

    private GeradorDeAlertas() {
    }

    public static Optional<Alerta> avaliar(Sensor sensor, EstrategiaPrevisao estrategia,
                                           double valorPrevisto, DadosTerritorio dados) {
        NivelAlerta nivel = dados.classificar(sensor.getTipo(), valorPrevisto);
        if (nivel == NivelAlerta.NENHUM) {
            return Optional.empty();
        }
        String contexto = "populacao da regiao: " + Formato.inteiro(dados.getPopulacao())
                + " | pontos vulneraveis cadastrados: " + dados.getPontosVulneraveis();
        Alerta alerta = new Alerta(
                sensor.getNome(), sensor.getId(), sensor.getTipo(), sensor.getUnidade(), dados.getRegiao(),
                valorPrevisto, nivel, dados.getLimiar(sensor.getTipo(), nivel), dados.getLimiares(sensor.getTipo()),
                estrategia.getNome(), estrategia.explicarUltimoCalculo(), contexto);
        return Optional.of(alerta);
    }
}
