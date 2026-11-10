
package br.com.pediupartiu.fidelidade;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.pediupartiu.fidelidade.integracao.MonolitoApiClient;

public class MonolitoApiClientTests {

    @Test
    void deveConsultarPedidosEntregues() {

        MonolitoApiClient client = new MonolitoApiClient();

        // Consulta os pedidos sem alterar os dados.
        List<Map<String, Object>> pedidos =
                client.buscarPedidosEntregues();

        assertNotNull(pedidos);

        // Confere se os pedidos possuem os IDs necessários.
        for (Map<String, Object> pedido : pedidos) {
            assertNotNull(pedido.get("idPedido"));
            assertNotNull(pedido.get("idCliente"));
        }
    }
}
