
package br.com.pediupartiu.fidelidade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.pediupartiu.fidelidade.entity.ResgateBrinde;

// Acessa o histórico de brindes resgatados.
public interface ResgateBrindeRepository
        extends JpaRepository<ResgateBrinde, Long> {

    // Verifica se um pedido já teve um brinde resgatado.
    boolean existsByIdPedido(Integer idPedido);
    
 // consulta um resgate pelo pedido
    java.util.Optional<ResgateBrinde> findByIdPedido(Integer idPedido);
}


