
package br.com.pediupartiu.fidelidade.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

// Registra o histórico de brindes resgatados pelos clientes.
@Entity
@Table(name = "resgate_brinde")
public class ResgateBrinde {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;

    // Impede registrar dois resgates para o mesmo pedido.
    @Column(name = "id_pedido", nullable = false, unique = true)
    private Integer idPedido;

    @Column(name = "id_produto_brinde")
    private Integer idProdutoBrinde;

    @Column(name = "valor_desconto", precision = 10, scale = 2)
    private BigDecimal valorDesconto;

    @Column(name = "data_resgate", nullable = false)
    private LocalDateTime dataResgate;
    
    // indica se o brinde foi devolvido
    @Column(name = "devolvido", nullable = false)
    private Boolean devolvido = false;

    public ResgateBrinde() {
    }

    public ResgateBrinde(Integer idCliente, Integer idPedido) {
        this.idCliente = idCliente;
        this.idPedido = idPedido;
        this.dataResgate = LocalDateTime.now();
        this.devolvido = false;
    }

    public Long getId() {
        return id;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public Integer getIdPedido() {
        return idPedido;
    }

    public Integer getIdProdutoBrinde() {
        return idProdutoBrinde;
    }

    public void setIdProdutoBrinde(Integer idProdutoBrinde) {
        this.idProdutoBrinde = idProdutoBrinde;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(BigDecimal valorDesconto) {
        this.valorDesconto = valorDesconto;
    }

    public LocalDateTime getDataResgate() {
        return dataResgate;
    }
    
    public Boolean getDevolvido() {
        return devolvido;
    }

    public void setDevolvido(Boolean devolvido) {
        this.devolvido = devolvido;
    }
}
