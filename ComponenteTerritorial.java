/**
 * Classe ABSTRATA: molde de tudo que e instalado no territorio
 * (sensores, paineis informativos...). Nao da para criar um objeto
 * ComponenteTerritorial diretamente, so das subclasses concretas.
 */
public abstract class ComponenteTerritorial {

    private final String id;
    private String nome;
    private String regiao;
    private StatusComponente status;

    protected ComponenteTerritorial(String id, String nome, String regiao) {
        this.id = exigirTexto(id, "O id do componente");
        this.nome = exigirTexto(nome, "O nome do componente");
        this.regiao = exigirTexto(regiao, "A regiao do componente");
        this.status = StatusComponente.ATIVO;
    }

    // ---- metodos concretos (iguais para todos os componentes) ----

    public void ativar() {
        this.status = StatusComponente.ATIVO;
    }

    public void desativar() {
        this.status = StatusComponente.INATIVO;
    }

    public boolean estaAtivo() {
        return status == StatusComponente.ATIVO;
    }

    // ---- metodo ABSTRATO: cada subclasse DEVE implementar do seu jeito ----

    /** Descreve, em uma linha, a situacao atual do componente. */
    public abstract String verificarStatus();

    // ---- getters e setters ----

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) {
        this.regiao = exigirTexto(regiao, "A regiao do componente");
    }

    public StatusComponente getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return nome + " [" + id + "] - " + status;
    }

    private static String exigirTexto(String texto, String campo) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException(campo + " nao pode ser vazio.");
        }
        return texto.trim();
    }
}
