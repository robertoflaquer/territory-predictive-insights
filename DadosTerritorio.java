import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ENCAPSULAMENTO: guarda os dados do territorio e protege tudo com atributos privados.
 * - Toda alteracao passa por um setter que VALIDA o valor.
 * - Valor invalido lanca IllegalArgumentException (com mensagem clara) ANTES de gravar,
 *   entao o valor anterior e sempre mantido.
 * - Os limiares de alerta so mudam com o codigo de acesso. O codigo nunca e guardado
 *   em texto puro (so um hash com "sal") e nao existe getter para ele.
 */
public class DadosTerritorio {

    public static final int POPULACAO_MAXIMA = 30_000_000;
    public static final int PONTOS_VULNERAVEIS_MAXIMO = 10_000;
    public static final int TAMANHO_MINIMO_CODIGO = 4;
    private static final int TENTATIVAS_ANTES_DO_BLOQUEIO = 3;
    private static final long BLOQUEIO_MILISSEGUNDOS = 30_000L;

    private String regiao;
    private int populacao;
    private int pontosVulneraveis;

    // tipo de sensor -> {atencao, alerta, critico}
    private final Map<String, double[]> limiares = new LinkedHashMap<>();
    // tipo de sensor -> {minimo fisico, maximo fisico} (faixa em que um limiar faz sentido)
    private final Map<String, double[]> faixasFisicas = new LinkedHashMap<>();

    private String codigoSal;
    private String codigoHash;
    private int tentativasFalhas = 0;
    private long bloqueadoAte = 0L;

    public DadosTerritorio(String regiao, int populacao, int pontosVulneraveis, String codigoAcesso) {
        setRegiao(regiao);
        setPopulacao(populacao);
        setPontosVulneraveis(pontosVulneraveis);
        validarFormatoDoCodigo(codigoAcesso);
        guardarCodigo(codigoAcesso);
    }

    // ------------------------------------------------------------------
    // Regiao, populacao e pontos vulneraveis (getters e setters validados)
    // ------------------------------------------------------------------

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String novaRegiao) {
        if (novaRegiao == null || novaRegiao.trim().isEmpty()) {
            throw new IllegalArgumentException("A regiao nao pode ser vazia.");
        }
        String texto = novaRegiao.trim();
        if (texto.length() < 3 || texto.length() > 60) {
            throw new IllegalArgumentException("A regiao deve ter entre 3 e 60 caracteres.");
        }
        if (!texto.matches("[\\p{L}0-9 .,'\\-]+")) {
            throw new IllegalArgumentException(
                    "A regiao so pode ter letras, numeros, espacos e os sinais . , ' -");
        }
        this.regiao = texto;
    }

    public int getPopulacao() {
        return populacao;
    }

    public void setPopulacao(int novaPopulacao) {
        if (novaPopulacao <= 0) {
            throw new IllegalArgumentException(
                    "A populacao deve ser maior que zero (recebido: " + novaPopulacao + ").");
        }
        if (novaPopulacao > POPULACAO_MAXIMA) {
            throw new IllegalArgumentException("A populacao de um territorio nao pode passar de "
                    + Formato.inteiro(POPULACAO_MAXIMA) + " (recebido: " + novaPopulacao + ").");
        }
        this.populacao = novaPopulacao;
    }

    public int getPontosVulneraveis() {
        return pontosVulneraveis;
    }

    public void setPontosVulneraveis(int novoTotal) {
        if (novoTotal < 0 || novoTotal > PONTOS_VULNERAVEIS_MAXIMO) {
            throw new IllegalArgumentException("Os pontos vulneraveis devem estar entre 0 e "
                    + Formato.inteiro(PONTOS_VULNERAVEIS_MAXIMO) + " (recebido: " + novoTotal + ").");
        }
        this.pontosVulneraveis = novoTotal;
    }

    // ------------------------------------------------------------------
    // Limiares de alerta
    // ------------------------------------------------------------------

    /**
     * Configuracao INICIAL dos limiares de um tipo de sensor (feita uma unica vez, na montagem
     * do sistema). Depois disso, so alterarLimiares(...) com o codigo de acesso muda os valores.
     */
    public void cadastrarTipoDeSensor(String tipo, double minimoFisico, double maximoFisico,
                                      double atencao, double alerta, double critico) {
        if (tipo == null || tipo.trim().isEmpty()) {
            throw new IllegalArgumentException("O tipo de sensor nao pode ser vazio.");
        }
        if (limiares.containsKey(tipo)) {
            throw new IllegalStateException("O tipo " + tipo
                    + " ja foi cadastrado; use alterarLimiares com o codigo de acesso.");
        }
        validarLimiares(tipo, minimoFisico, maximoFisico, atencao, alerta, critico);
        faixasFisicas.put(tipo, new double[] {minimoFisico, maximoFisico});
        limiares.put(tipo, new double[] {atencao, alerta, critico});
    }

    public boolean temLimiaresPara(String tipo) {
        return limiares.containsKey(tipo);
    }

    public List<String> getTiposConfigurados() {
        return new ArrayList<>(limiares.keySet());
    }

    /** Devolve uma COPIA {atencao, alerta, critico}, para ninguem alterar o array interno. */
    public double[] getLimiares(String tipo) {
        double[] valores = limiares.get(tipo);
        if (valores == null) {
            throw new IllegalArgumentException("Nao ha limiares cadastrados para o tipo " + tipo + ".");
        }
        return valores.clone();
    }

    public double getLimiar(String tipo, NivelAlerta nivel) {
        double[] valores = getLimiares(tipo);
        switch (nivel) {
            case ATENCAO:
                return valores[0];
            case ALERTA:
                return valores[1];
            case CRITICO:
                return valores[2];
            default:
                throw new IllegalArgumentException("O nivel " + nivel + " nao tem limiar.");
        }
    }

    /** Compara um valor com os limiares do tipo (atinge ou passa = entra no nivel). */
    public NivelAlerta classificar(String tipo, double valor) {
        double[] valores = getLimiares(tipo);
        if (valor >= valores[2]) {
            return NivelAlerta.CRITICO;
        }
        if (valor >= valores[1]) {
            return NivelAlerta.ALERTA;
        }
        if (valor >= valores[0]) {
            return NivelAlerta.ATENCAO;
        }
        return NivelAlerta.NENHUM;
    }

    /**
     * Altera os tres limiares de um tipo de sensor de uma vez (precisa manter atencao < alerta < critico).
     * Exige o codigo de acesso. Se algo estiver errado, lanca excecao e NADA e alterado.
     *
     * @throws SecurityException        codigo errado ou acesso temporariamente bloqueado
     * @throws IllegalArgumentException valores invalidos
     */
    public void alterarLimiares(String codigoInformado, String tipo,
                                double atencao, double alerta, double critico) {
        conferirCodigo(codigoInformado);
        double[] faixa = faixasFisicas.get(tipo);
        if (faixa == null) {
            throw new IllegalArgumentException("Nao ha limiares cadastrados para o tipo " + tipo + ".");
        }
        validarLimiares(tipo, faixa[0], faixa[1], atencao, alerta, critico);
        limiares.put(tipo, new double[] {atencao, alerta, critico});
    }

    /** Troca o codigo de acesso (exige o codigo atual). */
    public void alterarCodigoAcesso(String codigoAtual, String novoCodigo) {
        conferirCodigo(codigoAtual);
        validarFormatoDoCodigo(novoCodigo);
        guardarCodigo(novoCodigo);
    }

    public boolean estaBloqueado() {
        return System.currentTimeMillis() < bloqueadoAte;
    }

    // ------------------------------------------------------------------
    // Internos (privados)
    // ------------------------------------------------------------------

    private void validarLimiares(String tipo, double minimo, double maximo,
                                 double atencao, double alerta, double critico) {
        double[] todos = {atencao, alerta, critico};
        for (double valor : todos) {
            if (Double.isNaN(valor) || Double.isInfinite(valor)) {
                throw new IllegalArgumentException("Os limiares precisam ser numeros validos.");
            }
            if (valor < minimo || valor > maximo) {
                throw new IllegalArgumentException("Limiar fora da faixa de " + tipo + ": "
                        + Formato.numero(valor) + " (aceito de " + Formato.numero(minimo)
                        + " ate " + Formato.numero(maximo) + ").");
            }
        }
        if (!(atencao < alerta && alerta < critico)) {
            throw new IllegalArgumentException(
                    "Os limiares devem crescer: ATENCAO < ALERTA < CRITICO (recebido: "
                            + Formato.numero(atencao) + " / " + Formato.numero(alerta) + " / "
                            + Formato.numero(critico) + ").");
        }
    }

    private void conferirCodigo(String codigoInformado) {
        long agora = System.currentTimeMillis();
        if (agora < bloqueadoAte) {
            long segundos = (bloqueadoAte - agora + 999) / 1000;
            throw new SecurityException("Acesso bloqueado por excesso de tentativas. Tente de novo em "
                    + segundos + " s.");
        }
        if (codigoInformado == null || !gerarHash(codigoSal, codigoInformado).equals(codigoHash)) {
            tentativasFalhas++;
            if (tentativasFalhas >= TENTATIVAS_ANTES_DO_BLOQUEIO) {
                tentativasFalhas = 0;
                bloqueadoAte = agora + BLOQUEIO_MILISSEGUNDOS;
                throw new SecurityException("Codigo de acesso incorreto. Acesso bloqueado por "
                        + (BLOQUEIO_MILISSEGUNDOS / 1000) + " s apos "
                        + TENTATIVAS_ANTES_DO_BLOQUEIO + " tentativas erradas.");
            }
            throw new SecurityException("Codigo de acesso incorreto. Tentativas restantes: "
                    + (TENTATIVAS_ANTES_DO_BLOQUEIO - tentativasFalhas) + ".");
        }
        tentativasFalhas = 0;
    }

    private void validarFormatoDoCodigo(String codigo) {
        if (codigo == null || codigo.trim().length() < TAMANHO_MINIMO_CODIGO) {
            throw new IllegalArgumentException("O codigo de acesso deve ter pelo menos "
                    + TAMANHO_MINIMO_CODIGO + " caracteres.");
        }
    }

    private void guardarCodigo(String codigo) {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        this.codigoSal = paraHexadecimal(sal);
        this.codigoHash = gerarHash(codigoSal, codigo);
    }

    private static String gerarHash(String sal, String codigo) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] resultado = sha.digest((sal + codigo).getBytes(StandardCharsets.UTF_8));
            return paraHexadecimal(resultado);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel nesta JVM.", e);
        }
    }

    private static String paraHexadecimal(byte[] bytes) {
        StringBuilder texto = new StringBuilder();
        for (byte b : bytes) {
            texto.append(String.format("%02x", b));
        }
        return texto.toString();
    }

    @Override
    public String toString() {
        return "Territorio: " + regiao + " | populacao: " + Formato.inteiro(populacao)
                + " | pontos vulneraveis: " + pontosVulneraveis;
    }
}
