
package br.com.pediupartiu.fidelidade;

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.pediupartiu.fidelidade.integracao.MonolitoApiClient;
import br.com.pediupartiu.fidelidade.service.FidelidadeService;
import br.com.pediupartiu.fidelidade.service.SincronizacaoFidelidadeService;

public class SincronizacaoFidelidadeServiceTests {

    @Test
    void deveEncaminharPedidosEntreguesParaContabilizacao() {

        MonolitoApiClient monolito = mock(MonolitoApiClient.class);
        FidelidadeService fidelidade = mock(FidelidadeService.class);

        when(monolito.buscarPedidosEntregues()).thenReturn(List.of(
                Map.of("idPedido", 35, "idCliente", 34),
                Map.of("idPedido", 36, "idCliente", 34)
        ));

        SincronizacaoFidelidadeService sincronizacao =
                new SincronizacaoFidelidadeService(monolito, fidelidade);

        // Encaminha os pedidos para o serviço responsável.
        sincronizacao.sincronizarPedidos();

        verify(fidelidade).contabilizarPedido(35, 34);
        verify(fidelidade).contabilizarPedido(36, 34);
    }
}
