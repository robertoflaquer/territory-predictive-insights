/**
 * Painel instalado na comunidade que exibe a mensagem de orientacao mais recente.
 * Segunda classe concreta que herda de ComponenteTerritorial.
 */
public class PainelInformativo extends ComponenteTerritorial {

    private static final int TAMANHO_MAXIMO_MENSAGEM = 200;

    private String mensagemAtual = "Sem avisos no momento.";

    public PainelInformativo(String id, String nome, String regiao) {
        super(id, nome, regiao);
    }

    public void atualizarMensagem(String mensagem) {
        if (!estaAtivo()) {
            throw new IllegalStateException("O painel esta inativo e nao exibe mensagens.");
        }
        if (mensagem == null || mensagem.trim().isEmpty()) {
            throw new IllegalArgumentException("A mensagem do painel nao pode ser vazia.");
        }
        String texto = mensagem.trim();
        if (texto.length() > TAMANHO_MAXIMO_MENSAGEM) {
            texto = texto.substring(0, TAMANHO_MAXIMO_MENSAGEM - 3) + "...";
        }
        this.mensagemAtual = texto;
    }

    public String getMensagemAtual() {
        return mensagemAtual;
    }

    @Override
    public String verificarStatus() {
        if (!estaAtivo()) {
            return getNome() + " [" + getId() + "] " + getStatus() + " | mensagem nao exibida";
        }
        return getNome() + " [" + getId() + "] " + getStatus() + " | exibindo: \"" + mensagemAtual + "\"";
    }
}
