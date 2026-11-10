
package br.com.pediupartiu.fidelidade.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.com.pediupartiu.fidelidade.integracao.MonolitoApiClient;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClientException;

@Service
public class SincronizacaoFidelidadeService {

    private final MonolitoApiClient monolitoApiClient;
    
    private final FidelidadeService fidelidadeService;
    

    public SincronizacaoFidelidadeService(
            MonolitoApiClient monolitoApiClient,
            FidelidadeService fidelidadeService) {

        this.monolitoApiClient = monolitoApiClient;
        this.fidelidadeService = fidelidadeService;
    }

    // Consulta os pedidos entregues no sistema principal.
    public List<Map<String, Object>> consultarPedidos() {
        return monolitoApiClient.buscarPedidosEntregues();
    }
    

    
    // Recupera pedidos entregues ainda não contabilizados.
    public void sincronizarPedidos() {

        List<Map<String, Object>> pedidos = consultarPedidos();

        for (Map<String, Object> pedido : pedidos) {

            Integer idPedido =
                    ((Number) pedido.get("idPedido")).intValue();

            Integer idCliente =
                    ((Number) pedido.get("idCliente")).intValue();

            fidelidadeService.contabilizarPedido(idPedido, idCliente);
        }
    }
    

    // Recupera pedidos pendentes a cada 60 segundos.
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void sincronizarAutomaticamente() {
        try {
            sincronizarPedidos();
            System.out.println("Fidelidade: sincronização automática executada.");
        } catch (RestClientException e) {
            // O Pediu Partiu pode estar temporariamente desligado.
            System.out.println(
                "Fidelidade: aguardando conexão com o Pediu Partiu."
            );
        }
    }


}
