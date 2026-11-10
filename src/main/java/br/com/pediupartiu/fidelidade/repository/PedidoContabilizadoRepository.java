
package br.com.pediupartiu.fidelidade.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.pediupartiu.fidelidade.entity.PedidoContabilizado;



// Responsável pelas operações de consulta e persistência dos pedidos contabilizados.
public interface PedidoContabilizadoRepository
        extends JpaRepository<PedidoContabilizado, Long> {

    // Verifica se um pedido já foi contabilizado, evitando duplicações.
    boolean existsByIdPedido(Integer idPedido);

    // Busca os pedidos contabilizados de determinado cliente.
    List<PedidoContabilizado> findByIdCliente(Integer idCliente);
    
 // Localiza um pedido que já gerou estrela.
    Optional<PedidoContabilizado> findByIdPedido(Integer idPedido);

}
