package br.com.pediupartiu.fidelidade.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracao_brinde")
public class ConfiguracaoBrinde {

	@Id
	private Long id;

    // Produto escolhido pelo restaurante para o brinde.
    private Integer idProduto;

    public ConfiguracaoBrinde() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(Integer idProduto) {
        this.idProduto = idProduto;
    }
}