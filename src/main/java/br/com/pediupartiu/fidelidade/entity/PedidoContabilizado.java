
package br.com.pediupartiu.fidelidade.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Registra os pedidos contabilizados no programa de fidelidade.
@Entity
@Table(name = "pedido_contabilizado")
public class PedidoContabilizado {

    // Identificador gerado automaticamente pelo banco.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID do pedido no Pediu Partiu. Não permite duplicações.
    @Column(name = "id_pedido", nullable = false, unique = true)
    private Integer idPedido;

    // Identifica o cliente responsável pelo pedido.
    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;

    // Registra quando o pedido foi contabilizado.
    @Column(name = "data_contabilizacao", nullable = false)
    private LocalDateTime dataContabilizacao;
    

    // Indica se a estrela deste pedido já foi estornada.
    @Column(name = "estornado", nullable = false)
    private Boolean estornado = false;

    

    // Construtor vazio exigido pelo JPA.
    public PedidoContabilizado() {
    }

    // Construtor para registrar um pedido contabilizado.
    public PedidoContabilizado(Integer idPedido, Integer idCliente) {
        this.idPedido = idPedido;
        this.idCliente = idCliente;
        this.dataContabilizacao = LocalDateTime.now();
    }

    // Métodos para consultar os dados do registro.
    public Long getId() {
        return id;
    }

    public Integer getIdPedido() {
        return idPedido;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public LocalDateTime getDataContabilizacao() {
        return dataContabilizacao;
    }


    public Boolean getEstornado() {
        return estornado;
    }

    public void setEstornado(Boolean estornado) {
        this.estornado = estornado;
    }


}
