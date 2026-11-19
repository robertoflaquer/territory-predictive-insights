import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;

/**
 * Classe principal: monta o territorio, os sensores e as previsoes e roda o menu interativo.
 *
 * Recorte escolhido: CLIMA LOCAL (chuva intensa e risco de alagamento).
 * Os valores dos sensores sao SIMULADOS (nao ha hardware real).
 */
public class SistemaMonitoramento {

    private static final String ARQUIVO_HISTORICO = "historico_leituras.csv";
    private static final String CODIGO_PADRAO = "citizen2026";
    private static final int JANELA_MEDIA_MOVEL = 5;
    private static final int PERIODO_COMPARACAO = 3;
    private static final int MAXIMO_LEITURAS_SIMULADAS = 50;

    private final Scanner entrada;
    private final Path arquivoHistorico;
    private final DadosTerritorio dados;
    private final HistoricoLeituras historico = new HistoricoLeituras();
    private final List<Sensor> sensores = new ArrayList<>();
    private final List<ComponenteTerritorial> componentes = new ArrayList<>();
    private final List<EstrategiaPrevisao> estrategias = new ArrayList<>();
    private final PainelInformativo painel;
    private String cenarioAtual = "tempo normal";

    public SistemaMonitoramento(Scanner entrada, Random random, Path arquivoHistorico) {
        this.entrada = entrada;
        this.arquivoHistorico = arquivoHistorico;

        // territorio descrito por regiao (nunca por pessoas)
        this.dados = new DadosTerritorio("Bairro Exemplo - Zona Leste", 45000, 6, CODIGO_PADRAO);
        String regiao = dados.getRegiao();

        SensorChuva chuva = new SensorChuva("SEN-CHUVA-01", regiao, random);
        SensorNivelRio rio = new SensorNivelRio("SEN-RIO-01", regiao, random);
        SensorUmidade umidade = new SensorUmidade("SEN-UMID-01", regiao, random);
        this.painel = new PainelInformativo("PAINEL-01", "Painel Informativo da Praca", regiao);

        sensores.add(chuva);
        sensores.add(rio);
        sensores.add(umidade);
        componentes.add(chuva);
        componentes.add(rio);
        componentes.add(umidade);
        componentes.add(painel);

        // limiares iniciais: {atencao, alerta, critico}
        dados.cadastrarTipoDeSensor(Sensor.TIPO_CHUVA, 0, 200, 25, 50, 75);
        dados.cadastrarTipoDeSensor(Sensor.TIPO_NIVEL_RIO, 0, 15, 2.5, 3.5, 4.5);
        dados.cadastrarTipoDeSensor(Sensor.TIPO_UMIDADE_SOLO, 0, 100, 70, 85, 95);
        sincronizarLimitesDosSensores();

        // as duas estrategias de previsao (polimorfismo: tratadas como EstrategiaPrevisao)
        estrategias.add(new PrevisaoMediaMovel(JANELA_MEDIA_MOVEL));
        estrategias.add(new PrevisaoComparacao(PERIODO_COMPARACAO));

        carregarHistorico();
    }

    // ==================================================================
    // Programa principal e loop do menu
    // ==================================================================

    public static void main(String[] args) {
        long semente = System.nanoTime();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--seed") && i + 1 < args.length) {
                try {
                    semente = Long.parseLong(args[i + 1]);
                } catch (NumberFormatException e) {
                    System.out.println("Semente invalida, usando uma aleatoria.");
                }
            }
        }
        Scanner scanner = new Scanner(System.in);
        SistemaMonitoramento sistema = new SistemaMonitoramento(
                scanner, new Random(semente), Paths.get(ARQUIVO_HISTORICO));
        sistema.executar();
    }

    public void executar() {
        System.out.println();
        System.out.println("==========================================================");
        System.out.println("  TERRITORY PREDICTIVE INSIGHTS - monitoramento territorial");
        System.out.println("  Recorte: clima local (chuva e risco de alagamento)");
        System.out.println("==========================================================");
        System.out.println(dados);

        boolean rodando = true;
        try {
            while (rodando) {
                exibirMenu();
                String opcao = lerLinha("Escolha uma opcao: ").trim();
                switch (opcao) {
                    case "1":
                        verificarSensores();
                        break;
                    case "2":
                        rodarPrevisoes();
                        break;
                    case "3":
                        gerenciarDadosDoTerritorio();
                        break;
                    case "4":
                        simularProximasLeituras();
                        break;
                    case "5":
                        exibirStatusCompleto();
                        break;
                    case "6":
                        alternarComponente();
                        break;
                    case "7":
                        limparHistorico();
                        break;
                    case "0":
                        rodando = false;
                        break;
                    default:
                        System.out.println("Opcao invalida. Digite um numero de 0 a 7.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada encerrada.");
        } finally {
            salvarHistorico();
            System.out.println("Historico salvo em " + arquivoHistorico + ". Ate logo!");
        }
    }

    private void exibirMenu() {
        System.out.println();
        System.out.println("------------------------ MENU ------------------------");
        System.out.println(" 1 - Verificar sensores (faz uma nova leitura)");
        System.out.println(" 2 - Rodar previsoes e alertas");
        System.out.println(" 3 - Gerenciar dados do territorio");
        System.out.println(" 4 - Simular proximas leituras");
        System.out.println(" 5 - Exibir status completo");
        System.out.println(" 6 - Ativar / desativar um componente");
        System.out.println(" 7 - Limpar historico de leituras");
        System.out.println(" 0 - Sair");
        System.out.println("------------------------------------------------------");
    }

    // ==================================================================
    // 1 - Verificar sensores
    // ==================================================================

    private void verificarSensores() {
        System.out.println("\n=== VERIFICACAO DOS SENSORES ===");
        for (Sensor sensor : sensores) {
            if (!sensor.verificarFuncionamento()) {
                System.out.println(" [INATIVO] " + sensor.getNome() + " [" + sensor.getId()
                        + "]: nao esta medindo. Ative-o na opcao 6.");
                continue;
            }
            double valor = sensor.lerValor();
            historico.registrar(sensor.getId(), valor);
            String situacao = sensor.ultrapassouLimite(valor)
                    ? "ACIMA do limite de alerta (" + Formato.numero(sensor.getLimiteAlerta()) + ")"
                    : "dentro do limite de alerta (" + Formato.numero(sensor.getLimiteAlerta()) + ")";
            System.out.println(" [OK] " + sensor.getNome() + " [" + sensor.getId() + "]: "
                    + Formato.numero(valor) + " " + sensor.getUnidade() + " - " + situacao
                    + " | leituras guardadas: " + historico.quantidade(sensor.getId()));
        }
        System.out.println("Obs.: o limite de alerta do sensor e o limiar ALERTA do territorio.");
    }

    // ==================================================================
    // 2 - Previsoes e alertas
    // ==================================================================

    private void rodarPrevisoes() {
        System.out.println("\n=== PREVISOES ===");
        List<Alerta> alertas = new ArrayList<>();
        for (Sensor sensor : sensores) {
            System.out.println("\n" + sensor.getNome() + " [" + sensor.getId() + "] ("
                    + sensor.getUnidade() + ")");
            if (!sensor.verificarFuncionamento()) {
                System.out.println("   Sensor inativo: sem previsao (leituras antigas nao sao confiaveis).");
                continue;
            }
            List<Double> leituras = historico.getLeituras(sensor.getId());
            for (EstrategiaPrevisao estrategia : estrategias) {
                System.out.println("   - " + estrategia.descreverMetodo());
                if (!estrategia.temLeiturasSuficientes(leituras)) {
                    System.out.println("     SEM PREVISAO: ha " + leituras.size() + " leitura(s) e o minimo e "
                            + estrategia.getMinimoLeituras() + ". Com tao pouco dado a previsao enganaria,"
                            + " entao o sistema se cala. Use as opcoes 1 ou 4 para gerar leituras.");
                    continue;
                }
                double previsto = preverLimitado(sensor, estrategia, leituras);
                NivelAlerta nivel = dados.classificar(sensor.getTipo(), previsto);
                System.out.println("     Previsto: " + Formato.numero(previsto) + " " + sensor.getUnidade()
                        + " -> " + interpretar(sensor, nivel));
                System.out.println("     Como: " + estrategia.explicarUltimoCalculo() + ".");
                Optional<Alerta> alerta = GeradorDeAlertas.avaliar(sensor, estrategia, previsto, dados);
                if (alerta.isPresent()) {
                    alertas.add(alerta.get());
                }
            }
        }
        exibirAlertas(alertas);
        atualizarPainel(alertas);
    }

    private double preverLimitado(Sensor sensor, EstrategiaPrevisao estrategia, List<Double> leituras) {
        double previsto = estrategia.prever(leituras);
        // uma previsao nunca pode passar do que o sensor consegue medir (ex.: 100% de umidade)
        return Math.max(sensor.getFaixaMinima(), Math.min(sensor.getFaixaMaxima(), previsto));
    }

    private String interpretar(Sensor sensor, NivelAlerta nivel) {
        if (nivel == NivelAlerta.NENHUM) {
            return "normal (abaixo do limite de ATENCAO, "
                    + Formato.numero(dados.getLimiar(sensor.getTipo(), NivelAlerta.ATENCAO)) + " "
                    + sensor.getUnidade() + ")";
        }
        return "nivel " + nivel.getRotulo() + " (limite de " + nivel.getRotulo() + ": "
                + Formato.numero(dados.getLimiar(sensor.getTipo(), nivel)) + " " + sensor.getUnidade() + ")";
    }

    /** Coleta os alertas atuais sem imprimir as previsoes (usado no status completo). */
    private List<Alerta> coletarAlertas() {
        List<Alerta> alertas = new ArrayList<>();
        for (Sensor sensor : sensores) {
            if (!sensor.verificarFuncionamento()) {
                continue;
            }
            List<Double> leituras = historico.getLeituras(sensor.getId());
            for (EstrategiaPrevisao estrategia : estrategias) {
                if (estrategia.temLeiturasSuficientes(leituras)) {
                    double previsto = preverLimitado(sensor, estrategia, leituras);
                    GeradorDeAlertas.avaliar(sensor, estrategia, previsto, dados).ifPresent(alertas::add);
                }
            }
        }
        return alertas;
    }

    private void exibirAlertas(List<Alerta> alertas) {
        System.out.println("\n=== ALERTAS ===");
        if (alertas.isEmpty()) {
            System.out.println("Nenhum alerta: as previsoes disponiveis estao abaixo dos limiares de ATENCAO.");
            System.out.println("(Sensores sem leituras suficientes nao geram previsao nem alerta.)");
            return;
        }
        // do mais grave para o menos grave
        Collections.sort(alertas, (a, b) -> b.getNivel().compareTo(a.getNivel()));
        System.out.println(alertas.size() + " alerta(s), do mais grave para o menos grave:\n");
        for (Alerta alerta : alertas) {
            System.out.println(alerta.explicar());
            System.out.println();
        }
    }

    private void atualizarPainel(List<Alerta> alertas) {
        if (!painel.estaAtivo()) {
            System.out.println("(Painel informativo inativo: mensagem nao atualizada.)");
            return;
        }
        if (alertas.isEmpty()) {
            painel.atualizarMensagem("Sem alertas no momento. Situacao normal.");
        } else {
            Alerta maisGrave = alertas.get(0);
            for (Alerta alerta : alertas) {
                if (alerta.getNivel().maisGraveQue(maisGrave.getNivel())) {
                    maisGrave = alerta;
                }
            }
            painel.atualizarMensagem(maisGrave.resumo() + ". Acompanhe orientacoes da coordenacao local.");
        }
        System.out.println("Painel informativo atualizado: \"" + painel.getMensagemAtual() + "\"");
    }

    // ==================================================================
    // 3 - Gerenciar dados do territorio
    // ==================================================================

    private void gerenciarDadosDoTerritorio() {
        boolean noSubmenu = true;
        while (noSubmenu) {
            System.out.println("\n--- DADOS DO TERRITORIO ---");
            System.out.println(" 1 - Ver dados e limiares");
            System.out.println(" 2 - Alterar regiao");
            System.out.println(" 3 - Alterar populacao");
            System.out.println(" 4 - Alterar pontos vulneraveis");
            System.out.println(" 5 - Alterar limiares de alerta (exige codigo de acesso)");
            System.out.println(" 6 - Alterar codigo de acesso");
            System.out.println(" 0 - Voltar");
            String opcao = lerLinha("Escolha: ").trim();
            switch (opcao) {
                case "1":
                    exibirDadosELimiares();
                    break;
                case "2":
                    alterarRegiao();
                    break;
                case "3":
                    alterarPopulacao();
                    break;
                case "4":
                    alterarPontosVulneraveis();
                    break;
                case "5":
                    alterarLimiares();
                    break;
                case "6":
                    alterarCodigoDeAcesso();
                    break;
                case "0":
                    noSubmenu = false;
                    break;
                default:
                    System.out.println("Opcao invalida.");
            }
        }
    }

    private void exibirDadosELimiares() {
        System.out.println(dados);
        for (Sensor sensor : sensores) {
            double[] l = dados.getLimiares(sensor.getTipo());
            System.out.println(" " + sensor.getNome() + ": ATENCAO " + Formato.numero(l[0])
                    + " | ALERTA " + Formato.numero(l[1]) + " | CRITICO " + Formato.numero(l[2])
                    + " " + sensor.getUnidade());
        }
    }

    private void alterarRegiao() {
        String anterior = dados.getRegiao();
        try {
            dados.setRegiao(lerLinha("Nova regiao: "));
            for (ComponenteTerritorial componente : componentes) {
                componente.setRegiao(dados.getRegiao());
            }
            System.out.println("Regiao alterada para: " + dados.getRegiao());
        } catch (IllegalArgumentException e) {
            rejeitar(e.getMessage(), "regiao", anterior);
        }
    }

    private void alterarPopulacao() {
        String anterior = Formato.inteiro(dados.getPopulacao());
        Long valor = lerNumeroInteiro("Nova populacao: ");
        if (valor == null) {
            rejeitar("digite um numero inteiro", "populacao", anterior);
            return;
        }
        try {
            if (valor > Integer.MAX_VALUE || valor < Integer.MIN_VALUE) {
                throw new IllegalArgumentException("numero fora do intervalo aceito (maximo "
                        + Formato.inteiro(DadosTerritorio.POPULACAO_MAXIMA) + ").");
            }
            dados.setPopulacao(valor.intValue());
            System.out.println("Populacao alterada para: " + Formato.inteiro(dados.getPopulacao()));
        } catch (IllegalArgumentException e) {
            rejeitar(e.getMessage(), "populacao", anterior);
        }
    }

    private void alterarPontosVulneraveis() {
        String anterior = String.valueOf(dados.getPontosVulneraveis());
        Long valor = lerNumeroInteiro("Novo total de pontos vulneraveis: ");
        if (valor == null) {
            rejeitar("digite um numero inteiro", "pontos vulneraveis", anterior);
            return;
        }
        try {
            if (valor > Integer.MAX_VALUE || valor < Integer.MIN_VALUE) {
                throw new IllegalArgumentException("numero fora do intervalo aceito.");
            }
            dados.setPontosVulneraveis(valor.intValue());
            System.out.println("Pontos vulneraveis alterados para: " + dados.getPontosVulneraveis());
        } catch (IllegalArgumentException e) {
            rejeitar(e.getMessage(), "pontos vulneraveis", anterior);
        }
    }

    private void alterarLimiares() {
        if (dados.estaBloqueado()) {
            System.out.println("Acesso temporariamente bloqueado por excesso de tentativas. Aguarde e tente de novo.");
            return;
        }
        System.out.println("Tipos disponiveis:");
        for (int i = 0; i < sensores.size(); i++) {
            Sensor sensor = sensores.get(i);
            System.out.println(" " + (i + 1) + " - " + sensor.getNome() + " (" + sensor.getUnidade() + ")");
        }
        Long indice = lerNumeroInteiro("Escolha o sensor: ");
        if (indice == null || indice < 1 || indice > sensores.size()) {
            System.out.println("Alteracao rejeitada: sensor invalido. Nada foi alterado.");
            return;
        }
        Sensor sensor = sensores.get((int) (indice - 1));
        Double atencao = lerNumeroDecimal("Novo limiar de ATENCAO (" + sensor.getUnidade() + "): ");
        Double alerta = lerNumeroDecimal("Novo limiar de ALERTA (" + sensor.getUnidade() + "): ");
        Double critico = lerNumeroDecimal("Novo limiar de CRITICO (" + sensor.getUnidade() + "): ");
        if (atencao == null || alerta == null || critico == null) {
            System.out.println("Alteracao rejeitada: digite numeros validos (ex.: 12.5). Limiares mantidos.");
            return;
        }
        String codigo = lerLinha("Codigo de acesso (sera exibido na tela): ");
        try {
            dados.alterarLimiares(codigo, sensor.getTipo(), atencao, alerta, critico);
            sincronizarLimitesDosSensores();
            System.out.println("Limiares de " + sensor.getNome() + " atualizados.");
        } catch (SecurityException e) {
            System.out.println("Alteracao rejeitada: " + e.getMessage() + " Limiares mantidos.");
        } catch (IllegalArgumentException e) {
            System.out.println("Alteracao rejeitada: " + e.getMessage() + " Limiares mantidos.");
        }
    }

    private void alterarCodigoDeAcesso() {
        if (dados.estaBloqueado()) {
            System.out.println("Acesso temporariamente bloqueado por excesso de tentativas. Aguarde e tente de novo.");
            return;
        }
        String atual = lerLinha("Codigo atual: ");
        String novo = lerLinha("Novo codigo (minimo " + DadosTerritorio.TAMANHO_MINIMO_CODIGO + " caracteres): ");
        try {
            dados.alterarCodigoAcesso(atual, novo);
            System.out.println("Codigo de acesso alterado.");
        } catch (SecurityException e) {
            System.out.println("Alteracao rejeitada: " + e.getMessage() + " Codigo mantido.");
        } catch (IllegalArgumentException e) {
            System.out.println("Alteracao rejeitada: " + e.getMessage() + " Codigo mantido.");
        }
    }

    private void rejeitar(String motivo, String campo, String valorMantido) {
        String semPonto = motivo.endsWith(".") ? motivo.substring(0, motivo.length() - 1) : motivo;
        System.out.println("Alteracao rejeitada: " + semPonto + ". Valor de " + campo
                + " mantido: " + valorMantido);
    }

    /** O limite de alerta de cada sensor passa a ser o limiar ALERTA configurado no territorio. */
    private void sincronizarLimitesDosSensores() {
        for (Sensor sensor : sensores) {
            sensor.setLimiteAlerta(dados.getLimiar(sensor.getTipo(), NivelAlerta.ALERTA));
        }
    }

    // ==================================================================
    // 4 - Simular proximas leituras
    // ==================================================================

    private void simularProximasLeituras() {
        System.out.println("\n=== SIMULAR PROXIMAS LEITURAS ===");
        Long quantidade = lerNumeroInteiro("Quantas leituras simular (1 a " + MAXIMO_LEITURAS_SIMULADAS + ")? ");
        if (quantidade == null || quantidade < 1 || quantidade > MAXIMO_LEITURAS_SIMULADAS) {
            System.out.println("Quantidade invalida. Digite um inteiro de 1 a " + MAXIMO_LEITURAS_SIMULADAS + ".");
            return;
        }
        System.out.println("Cenario: 1 - tempo normal | 2 - chuva moderada | 3 - tempestade chegando");
        String escolha = lerLinha("Cenario: ").trim();
        double intensidade;
        switch (escolha) {
            case "1":
                intensidade = 0.0;
                cenarioAtual = "tempo normal";
                break;
            case "2":
                intensidade = 0.5;
                cenarioAtual = "chuva moderada";
                break;
            case "3":
                intensidade = 1.0;
                cenarioAtual = "tempestade chegando";
                break;
            default:
                System.out.println("Cenario invalido. Nada foi simulado.");
                return;
        }
        for (Sensor sensor : sensores) {
            sensor.setIntensidadeCenario(intensidade);
        }
        List<List<Double>> geradas = new ArrayList<>();
        for (int i = 0; i < sensores.size(); i++) {
            geradas.add(new ArrayList<Double>());
        }
        for (int rodada = 0; rodada < quantidade; rodada++) {
            for (int i = 0; i < sensores.size(); i++) {
                Sensor sensor = sensores.get(i);
                if (sensor.verificarFuncionamento()) {
                    double valor = sensor.lerValor();
                    historico.registrar(sensor.getId(), valor);
                    geradas.get(i).add(valor);
                }
            }
        }
        System.out.println("Cenario \"" + cenarioAtual + "\", " + quantidade + " leitura(s) por sensor ativo:");
        for (int i = 0; i < sensores.size(); i++) {
            Sensor sensor = sensores.get(i);
            if (!sensor.verificarFuncionamento()) {
                System.out.println(" " + sensor.getNome() + ": inativo, nada simulado.");
                continue;
            }
            StringBuilder linha = new StringBuilder(" " + sensor.getNome() + " (" + sensor.getUnidade() + "):");
            for (Double valor : geradas.get(i)) {
                linha.append(' ').append(Formato.numero(valor));
            }
            System.out.println(linha);
        }
        salvarHistorico();
        // alertas automaticos: depois de simular, o sistema ja compara previsoes com os limiares
        rodarPrevisoes();
    }

    // ==================================================================
    // 5 - Status completo
    // ==================================================================

    private void exibirStatusCompleto() {
        System.out.println("\n=== STATUS COMPLETO ===");
        System.out.println(dados);
        System.out.println("Cenario da simulacao: " + cenarioAtual);
        System.out.println("\nComponentes:");
        for (ComponenteTerritorial componente : componentes) {
            System.out.println(" - " + componente.verificarStatus());
        }
        System.out.println("\nUltimas leituras (ate 5 por sensor):");
        for (Sensor sensor : sensores) {
            List<Double> leituras = historico.getLeituras(sensor.getId());
            StringBuilder linha = new StringBuilder(" " + sensor.getNome() + " - total guardado: "
                    + leituras.size() + " |");
            int inicio = Math.max(0, leituras.size() - 5);
            for (int i = inicio; i < leituras.size(); i++) {
                linha.append(' ').append(Formato.numero(leituras.get(i)));
            }
            if (leituras.isEmpty()) {
                linha.append(" (sem leituras)");
            }
            System.out.println(linha);
        }
        System.out.println("\nEstrategias de previsao:");
        for (EstrategiaPrevisao estrategia : estrategias) {
            System.out.println(" - " + estrategia.descreverMetodo());
        }
        List<Alerta> alertas = coletarAlertas();
        exibirAlertas(alertas);
        System.out.println("Mensagem atual do painel: \"" + painel.getMensagemAtual() + "\"");
    }

    // ==================================================================
    // 6 e 7 - Componentes e historico
    // ==================================================================

    private void alternarComponente() {
        System.out.println("\nComponentes:");
        for (int i = 0; i < componentes.size(); i++) {
            System.out.println(" " + (i + 1) + " - " + componentes.get(i));
        }
        Long indice = lerNumeroInteiro("Qual alternar (ativo <-> inativo)? ");
        if (indice == null || indice < 1 || indice > componentes.size()) {
            System.out.println("Componente invalido. Nada foi alterado.");
            return;
        }
        ComponenteTerritorial escolhido = componentes.get((int) (indice - 1));
        if (escolhido.estaAtivo()) {
            escolhido.desativar();
        } else {
            escolhido.ativar();
        }
        System.out.println("Agora: " + escolhido);
    }

    private void limparHistorico() {
        String resposta = lerLinha("Apagar TODAS as leituras guardadas? (s/n): ").trim().toLowerCase();
        if (resposta.equals("s")) {
            historico.limpar();
            salvarHistorico();
            System.out.println("Historico apagado. As previsoes voltam a exigir novas leituras.");
        } else {
            System.out.println("Nada foi apagado.");
        }
    }

    // ==================================================================
    // Arquivo de historico (bonus)
    // ==================================================================

    private void carregarHistorico() {
        try {
            int ignoradas = historico.carregar(arquivoHistorico, sensores);
            int total = 0;
            for (Sensor sensor : sensores) {
                total += historico.quantidade(sensor.getId());
            }
            if (total > 0) {
                System.out.println("Historico recarregado de " + arquivoHistorico + ": " + total + " leitura(s).");
            }
            if (ignoradas > 0) {
                System.out.println("Aviso: " + ignoradas + " linha(s) invalida(s) do arquivo foram ignoradas.");
            }
        } catch (IOException e) {
            System.out.println("Aviso: nao foi possivel ler o historico (" + e.getMessage() + "). Comecando vazio.");
        }
    }

    private void salvarHistorico() {
        try {
            historico.salvar(arquivoHistorico);
        } catch (IOException e) {
            System.out.println("Aviso: nao foi possivel salvar o historico (" + e.getMessage() + ").");
        }
    }

    // ==================================================================
    // Leitura de entrada
    // ==================================================================

    /** Le uma linha; se a entrada acabar, lanca NoSuchElementException (tratada no loop principal). */
    private String lerLinha(String mensagem) {
        System.out.print(mensagem);
        return entrada.nextLine();
    }

    private Long lerNumeroInteiro(String mensagem) {
        String texto = lerLinha(mensagem).trim();
        try {
            return Long.parseLong(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Aceita ponto ou virgula como separador decimal. */
    private Double lerNumeroDecimal(String mensagem) {
        String texto = lerLinha(mensagem).trim().replace(',', '.');
        try {
            double valor = Double.parseDouble(texto);
            if (Double.isNaN(valor) || Double.isInfinite(valor)) {
                return null;
            }
            return valor;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
