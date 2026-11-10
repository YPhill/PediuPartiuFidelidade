
package br.com.pediupartiu.fidelidade.integracao;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import org.springframework.web.client.RestClientResponseException;

@Component
public class MonolitoApiClient {

    private final RestClient restClient;

    public MonolitoApiClient() {
        this.restClient = RestClient.create("http://localhost:8080");
    }


    // Consulta os pedidos entregues desde o início da promoção.
    public List<Map<String, Object>> buscarPedidosEntregues() {

        return restClient.get()
                .uri("/api/integracao/fidelidade/pedidos-entregues")
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<
                        List<Map<String, Object>>>() {});
    }
    
    
 // Verifica se o produto existe no cardápio do Pediu Partiu.
    public boolean produtoExiste(Integer idProduto) {
        try {
            restClient.get()
                    .uri("/api/integracao/produtos/{idProduto}/existe", idProduto)
                    .retrieve()
                    .toBodilessEntity();

            return true;

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return false;
            }

            throw e;
        }
    }
    
    public Map<String, Object> buscarProduto(Integer idProduto) {
        return restClient.get()
                .uri("/api/integracao/produtos/{idProduto}", idProduto)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<
                        Map<String, Object>>() {});
    }
    
 // consulta os dados de um pedido no monólito
    public Map<String, Object> buscarPedido(Integer idPedido) {
        return restClient.get()
                .uri("/api/integracao/fidelidade/pedidos/{idPedido}", idPedido)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<
                        Map<String, Object>>() {});
    }

}
