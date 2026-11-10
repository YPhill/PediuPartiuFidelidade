
package br.com.pediupartiu.fidelidade.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.pediupartiu.fidelidade.entity.ProgressoFidelidade;

// Responsável por salvar e consultar o progresso dos clientes.
public interface ProgressoFidelidadeRepository
        extends JpaRepository<ProgressoFidelidade, Long> {

    // Busca o progresso de um cliente pelo ID do Pediu Partiu.
    Optional<ProgressoFidelidade> findByIdCliente(Integer idCliente);

}
