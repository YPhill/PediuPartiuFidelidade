
package br.com.pediupartiu.fidelidade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Armazena o progresso atual de cada cliente na fidelidade.
@Entity
@Table(name = "progresso_fidelidade")
public class ProgressoFidelidade {

    // Identificador interno gerado pelo banco.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cada cliente possui apenas um registro de progresso.
    @Column(name = "id_cliente", nullable = false, unique = true)
    private Integer idCliente;

    // Quantidade de estrelas acumuladas, de 0 até 10.
    @Column(name = "estrelas", nullable = false)
    private Integer estrelas = 0;

    // Indica se o cliente já conquistou um brinde.
    @Column(name = "brinde_pendente", nullable = false)
    private Boolean brindePendente = false;
    
 // Estrelas que serão descontadas dos próximos pedidos válidos.
    @Column(name = "estrelas_pendentes_estorno", nullable = false)
    private Integer estrelasPendentesEstorno = 0;

    // Construtor vazio necessário para o JPA.
    public ProgressoFidelidade() {
    }

    // Cria o progresso inicial de um cliente.
    public ProgressoFidelidade(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public Long getId() {
        return id;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public Integer getEstrelas() {
        return estrelas;
    }

    public void setEstrelas(Integer estrelas) {
        this.estrelas = estrelas;
    }

    public Boolean getBrindePendente() {
        return brindePendente;
    }

    public void setBrindePendente(Boolean brindePendente) {
        this.brindePendente = brindePendente;
    }
    
 // Consulta a quantidade de estornos pendentes.
    public Integer getEstrelasPendentesEstorno() {
        return estrelasPendentesEstorno;
    }

    // Atualiza a quantidade de estornos pendentes.
    public void setEstrelasPendentesEstorno(Integer estrelasPendentesEstorno) {
        this.estrelasPendentesEstorno = estrelasPendentesEstorno;
    }
}
