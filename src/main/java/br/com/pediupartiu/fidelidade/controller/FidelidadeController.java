
package br.com.pediupartiu.fidelidade.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.pediupartiu.fidelidade.entity.ProgressoFidelidade;
import br.com.pediupartiu.fidelidade.repository.ProgressoFidelidadeRepository;
import br.com.pediupartiu.fidelidade.service.ConfiguracaoBrindeService;
import br.com.pediupartiu.fidelidade.service.FidelidadeService;
import br.com.pediupartiu.fidelidade.service.SincronizacaoFidelidadeService;
import java.math.BigDecimal;
import br.com.pediupartiu.fidelidade.repository.ResgateBrindeRepository;

// Disponibiliza os endpoints HTTP do programa de fidelidade.
@RestController
@RequestMapping("/api/fidelidade")
public class FidelidadeController {

	private final ProgressoFidelidadeRepository progressoRepository;
	private final FidelidadeService fidelidadeService;
	private final SincronizacaoFidelidadeService sincronizacaoService;
	private final ConfiguracaoBrindeService configuracaoBrindeService;
	private final ResgateBrindeRepository resgateRepository;

	public FidelidadeController(ProgressoFidelidadeRepository progressoRepository, FidelidadeService fidelidadeService,
			SincronizacaoFidelidadeService sincronizacaoService, ConfiguracaoBrindeService configuracaoBrindeService,
			ResgateBrindeRepository resgateRepository) {

		this.progressoRepository = progressoRepository;
		this.fidelidadeService = fidelidadeService;
		this.sincronizacaoService = sincronizacaoService;
		this.configuracaoBrindeService = configuracaoBrindeService;
		this.resgateRepository = resgateRepository;
	}

	// Consulta as estrelas e o brinde pendente de um cliente.
	@GetMapping("/clientes/{idCliente}")
	public ResponseEntity<ProgressoFidelidade> consultarProgresso(@PathVariable Integer idCliente) {

		return progressoRepository.findByIdCliente(idCliente).map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	// Endpoint temporário para simular um pedido entregue.
	@PostMapping("/teste/pedido-entregue")
	public ResponseEntity<String> testarPedidoEntregue(@RequestParam Integer idPedido,
			@RequestParam Integer idCliente) {

		fidelidadeService.contabilizarPedido(idPedido, idCliente);

		return ResponseEntity.ok("Pedido de teste processado.");
	}

	// Executa a recuperação manual dos pedidos entregues.
	@PostMapping("/teste/sincronizar")
	public ResponseEntity<String> sincronizarPedidos() {
		sincronizacaoService.sincronizarPedidos();
		return ResponseEntity.ok("Sincronização concluída.");
	}

	// Consulta o produto selecionado como brinde.
	@GetMapping("/brinde")
	public ResponseEntity<Integer> consultarBrinde() {

		Integer idProduto = configuracaoBrindeService.consultarProdutoBrinde();

		if (idProduto == null) {
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.ok(idProduto);
	}

	// Define o produto que será oferecido como brinde.
	@PostMapping("/brinde")
	public ResponseEntity<String> selecionarBrinde(@RequestParam Integer idProduto) {

		configuracaoBrindeService.selecionarProdutoBrinde(idProduto);

		return ResponseEntity.ok("Produto selecionado como brinde.");
	}

	// registra o resgate do brinde
	@PostMapping("/resgatar")
	public ResponseEntity<String> resgatarBrinde(@RequestParam Integer idPedido, @RequestParam Integer idCliente,
			@RequestParam Integer idProdutoBrinde, @RequestParam BigDecimal valorDesconto) {

		fidelidadeService.resgatarBrinde(idPedido, idCliente, idProdutoBrinde, valorDesconto);

		return ResponseEntity.ok("Brinde resgatado com sucesso.");
	}

	// consulta se o pedido já possui resgate
	@GetMapping("/resgates/pedidos/{idPedido}")
	public ResponseEntity<Boolean> consultarResgate(@PathVariable Integer idPedido) {

		return ResponseEntity.ok(resgateRepository.existsByIdPedido(idPedido));
	}

	// consulta o desconto registrado para um pedido
	@GetMapping("/resgates/pedidos/{idPedido}/desconto")
	public ResponseEntity<BigDecimal> consultarDescontoPedido(@PathVariable Integer idPedido) {

		return resgateRepository.findByIdPedido(idPedido).map(resgate -> ResponseEntity.ok(resgate.getValorDesconto()))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	// estorna a estrela de um pedido contabilizado
	@PostMapping("/pedidos/{idPedido}/estornar")
	public ResponseEntity<String> estornarPedido(@PathVariable Integer idPedido) {

	    fidelidadeService.estornarPedido(idPedido);

	    return ResponseEntity.ok("Estorno processado com sucesso.");
	}
	

    // devolve o brinde de um pedido cancelado
    @PostMapping("/resgates/pedidos/{idPedido}/devolver")
    public ResponseEntity<String> devolverBrinde(
            @PathVariable Integer idPedido) {

        fidelidadeService.devolverBrinde(idPedido);

        return ResponseEntity.ok(
                "Devolução do brinde processada com sucesso.");
    }
    
    
 // consulta se o brinde do pedido foi devolvido
    @GetMapping("/resgates/pedidos/{idPedido}/devolvido")
    public ResponseEntity<Boolean> consultarBrindeDevolvido(
            @PathVariable Integer idPedido) {

        return resgateRepository.findByIdPedido(idPedido)
                .map(resgate -> ResponseEntity.ok(
                        Boolean.TRUE.equals(resgate.getDevolvido())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    


}


