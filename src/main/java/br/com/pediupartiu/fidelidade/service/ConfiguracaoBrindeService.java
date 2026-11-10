package br.com.pediupartiu.fidelidade.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.pediupartiu.fidelidade.entity.ConfiguracaoBrinde;
import br.com.pediupartiu.fidelidade.repository.ConfiguracaoBrindeRepository;

import br.com.pediupartiu.fidelidade.integracao.MonolitoApiClient;

import java.math.BigDecimal;

// Controla o produto selecionado como brinde.
@Service
public class ConfiguracaoBrindeService {

    private final ConfiguracaoBrindeRepository configuracaoRepository;
    private final MonolitoApiClient monolitoApiClient;

    public ConfiguracaoBrindeService(
            ConfiguracaoBrindeRepository configuracaoRepository,
            MonolitoApiClient monolitoApiClient) {

        this.configuracaoRepository = configuracaoRepository;
        this.monolitoApiClient = monolitoApiClient;
    }


    // Consulta o brinde e limpa a seleção se o produto foi excluído.
    @Transactional
    public Integer consultarProdutoBrinde() {

        Integer idProduto = configuracaoRepository.findById(1L)
                .map(ConfiguracaoBrinde::getIdProduto)
                .orElse(null);

        if (idProduto == null) {
            return null;
        }

        if (!monolitoApiClient.produtoExiste(idProduto)) {
            configuracaoRepository.deleteById(1L);
            return null;
        }

        return idProduto;
    }


    // Substitui o produto selecionado anteriormente.
    @Transactional
    public void selecionarProdutoBrinde(Integer idProduto) {

        if (idProduto == null || idProduto <= 0) {
            throw new IllegalArgumentException("Produto inválido.");
        }
        
        // Impede selecionar produtos inexistentes no cardápio.
        if (!monolitoApiClient.produtoExiste(idProduto)) {
            throw new IllegalArgumentException(
                    "O produto selecionado não existe no cardápio.");
        }

        ConfiguracaoBrinde configuracao = configuracaoRepository
                .findById(1L)
                .orElseGet(ConfiguracaoBrinde::new);

        configuracao.setId(1L);
        configuracao.setIdProduto(idProduto);

        configuracaoRepository.save(configuracao);
    }
    
    public BigDecimal consultarPrecoBrinde() {
        Integer idProduto = consultarProdutoBrinde();

        if (idProduto == null) {
            throw new IllegalStateException("Nenhum brinde configurado.");
        }

        var produto = monolitoApiClient.buscarProduto(idProduto);

        if (produto == null || produto.get("precoUnitario") == null) {
            throw new IllegalStateException("Preço do brinde não encontrado.");
        }

        BigDecimal preco = new BigDecimal(
                produto.get("precoUnitario").toString()
        );

        if (preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("O brinde precisa ter preço positivo.");
        }

        return preco;
    }
}