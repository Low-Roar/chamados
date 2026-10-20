import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GeneratedType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** ENTITY: representa a tabela do banco. Cada objeto = uma linha.
 *  Deixei anotado pra me ajudar na compreensão dps :P
 */

@Entity
@Table(name ="clientes")
public.class Cliente{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long.id;

    @Column(nullable = false, unique = true, lenght = 120) 
    private.String telefone;

    public Cliente() {}

    public Cliente(String nome, String email, String telefone){
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }

    public Long getId(){ return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setTelefone(String telefone) {this.telefone = telefone;}      
}

