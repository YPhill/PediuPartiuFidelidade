package br.com.pediupartiu.fidelidade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.pediupartiu.fidelidade.entity.ConfiguracaoBrinde;

// Acessa a configuração do produto escolhido como brinde.
public interface ConfiguracaoBrindeRepository
        extends JpaRepository<ConfiguracaoBrinde, Long> {
}