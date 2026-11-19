import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guarda as ultimas leituras de cada sensor em ArrayList e sabe salvar/recarregar
 * tudo em um arquivo de texto (BONUS: historico persistente).
 * Formato do arquivo: uma leitura por linha, "idDoSensor;valor" (decimal sempre com ponto).
 */
public class HistoricoLeituras {

    public static final int MAXIMO_POR_SENSOR = 100;

    private final Map<String, ArrayList<Double>> leituras = new LinkedHashMap<>();

    public void registrar(String idSensor, double valor) {
        ArrayList<Double> lista = leituras.get(idSensor);
        if (lista == null) {
            lista = new ArrayList<>();
            leituras.put(idSensor, lista);
        }
        lista.add(valor);
        if (lista.size() > MAXIMO_POR_SENSOR) {
            lista.remove(0); // descarta a mais antiga
        }
    }

    /** Devolve uma COPIA, da leitura mais antiga para a mais nova. */
    public List<Double> getLeituras(String idSensor) {
        ArrayList<Double> lista = leituras.get(idSensor);
        return (lista == null) ? new ArrayList<Double>() : new ArrayList<>(lista);
    }

    public int quantidade(String idSensor) {
        ArrayList<Double> lista = leituras.get(idSensor);
        return (lista == null) ? 0 : lista.size();
    }

    public void limpar() {
        leituras.clear();
    }

    public void salvar(Path arquivo) throws IOException {
        List<String> linhas = new ArrayList<>();
        linhas.add("# historico de leituras - idDoSensor;valor");
        for (Map.Entry<String, ArrayList<Double>> entrada : leituras.entrySet()) {
            for (Double valor : entrada.getValue()) {
                linhas.add(entrada.getKey() + ";" + valor);
            }
        }
        Files.write(arquivo, linhas, StandardCharsets.UTF_8);
    }

    /**
     * Recarrega o historico. Linhas invalidas (formato errado, sensor desconhecido ou valor fora
     * da faixa do sensor) sao ignoradas.
     *
     * @return quantas linhas foram ignoradas
     */
    public int carregar(Path arquivo, List<? extends Sensor> sensores) throws IOException {
        leituras.clear();
        int ignoradas = 0;
        if (!Files.exists(arquivo)) {
            return 0;
        }
        for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
            String texto = linha.trim();
            if (texto.isEmpty() || texto.startsWith("#")) {
                continue;
            }
            String[] partes = texto.split(";");
            if (partes.length != 2) {
                ignoradas++;
                continue;
            }
            Sensor dono = procurar(sensores, partes[0].trim());
            double valor;
            try {
                valor = Double.parseDouble(partes[1].trim());
            } catch (NumberFormatException e) {
                ignoradas++;
                continue;
            }
            boolean valido = dono != null && !Double.isNaN(valor) && !Double.isInfinite(valor)
                    && valor >= dono.getFaixaMinima() && valor <= dono.getFaixaMaxima();
            if (valido) {
                registrar(dono.getId(), valor);
            } else {
                ignoradas++;
            }
        }
        return ignoradas;
    }

    private static Sensor procurar(List<? extends Sensor> sensores, String id) {
        for (Sensor sensor : sensores) {
            if (sensor.getId().equals(id)) {
                return sensor;
            }
        }
        return null;
    }
}
