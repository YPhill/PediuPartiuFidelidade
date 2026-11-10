
package br.com.pediupartiu.fidelidade;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.pediupartiu.fidelidade.entity.PedidoContabilizado;
import br.com.pediupartiu.fidelidade.entity.ProgressoFidelidade;
import br.com.pediupartiu.fidelidade.entity.ResgateBrinde;
import br.com.pediupartiu.fidelidade.repository.PedidoContabilizadoRepository;
import br.com.pediupartiu.fidelidade.repository.ProgressoFidelidadeRepository;
import br.com.pediupartiu.fidelidade.repository.ResgateBrindeRepository;
import br.com.pediupartiu.fidelidade.service.ConfiguracaoBrindeService;
import br.com.pediupartiu.fidelidade.service.FidelidadeService;
import br.com.pediupartiu.fidelidade.integracao.MonolitoApiClient;

class FidelidadeServiceTests {

	// simula a comunicação com o monólito nos testes
	private final MonolitoApiClient monolitoApiClient = mock(MonolitoApiClient.class);

	@Test
	void deveResgatarBrindeEReiniciarEstrelas() {

		// Simula os repositórios sem acessar o MySQL.
		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);

		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);

		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Prepara um cliente com 10 estrelas e brinde disponível.
		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(10);
		progresso.setBrindePendente(true);

		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		when(configuracaoBrindeService.consultarProdutoBrinde()).thenReturn(5);

		when(configuracaoBrindeService.consultarPrecoBrinde()).thenReturn(new BigDecimal("25.00"));

//simula um pedido válido com brinde e item pago
		Map<String, Object> pedidoSimulado = Map.of("idPedido", 1012, "idCliente", 1, "itens",
				List.of(Map.of("idProduto", 5, "quantidade", 1, "precoUnitario", 25.00),
						Map.of("idProduto", 8, "quantidade", 1, "precoUnitario", 18.00)));

		when(monolitoApiClient.buscarPedido(1012)).thenReturn(pedidoSimulado);

		// Simula a confirmação do resgate no pedido 1012.
		service.resgatarBrinde(1012, 1, 5, new BigDecimal("25.00"));

		// Verifica se o brinde foi registrado e as estrelas zeradas.
		assertEquals(0, progresso.getEstrelas());
		assertFalse(progresso.getBrindePendente());

		verify(resgateRepository).save(any(ResgateBrinde.class));
		verify(progressoRepository).save(progresso);
	}

	@Test
	void naoDeveResgatarSemBrindeDisponivel() {

		// Simula os repositórios.
		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);

		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);

		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Cliente com apenas 5 estrelas e nenhum brinde disponível.
		ProgressoFidelidade progresso = new ProgressoFidelidade(2);
		progresso.setEstrelas(5);
		progresso.setBrindePendente(false);

		when(progressoRepository.findByIdCliente(2)).thenReturn(Optional.of(progresso));

		// Verifica se o resgate é recusado.
		assertThrows(IllegalStateException.class, () -> service.resgatarBrinde(2001, 2, 5, new BigDecimal("25.00")));

		// Nenhum resgate deve ser salvo.
		verify(resgateRepository, never()).save(any(ResgateBrinde.class));
		assertEquals(5, progresso.getEstrelas());
	}

	@Test
	void naoDeveResgatarDuasVezesNoMesmoPedido() {

		// Simula os repositórios sem acessar o banco.
		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);

		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);

		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Simula um pedido que já possui resgate registrado.
		when(resgateRepository.existsByIdPedido(1012)).thenReturn(true);

		// O sistema deve impedir um segundo resgate.
		assertThrows(IllegalStateException.class, () -> service.resgatarBrinde(1012, 1, 5, new BigDecimal("25.00")));

		// Nenhum novo resgate ou progresso deve ser salvo.
		verify(resgateRepository, never()).save(any(ResgateBrinde.class));
		verify(progressoRepository, never()).save(any(ProgressoFidelidade.class));
	}

	@Test
	void deveEstornarEstrelaDePedidoCancelado() {

		// Simula os repositórios sem acessar o MySQL.
		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);

		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);

		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Simula um pedido contabilizado para o cliente 1.
		PedidoContabilizado pedido = new PedidoContabilizado(1001, 1);

		// O cliente possui 10 estrelas e um brinde pendente.
		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(10);
		progresso.setBrindePendente(true);

		when(pedidoRepository.findByIdPedido(1001)).thenReturn(Optional.of(pedido));

		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		// Simula o cancelamento do pedido.
		service.estornarPedido(1001);

		// Verifica se a estrela foi retirada.
		assertEquals(9, progresso.getEstrelas());
		assertFalse(progresso.getBrindePendente());

		// Verifica se o pedido foi marcado como estornado.
		assertTrue(pedido.getEstornado());

		// Confirma que as alterações foram enviadas aos repositórios.
		verify(progressoRepository).save(progresso);
		verify(pedidoRepository).save(pedido);
	}

	@Test
	void naoDeveEstornarPedidoDuasVezes() {

		// Simula os repositórios sem acessar o MySQL.
		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);

		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);

		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Simula um pedido cuja estrela já foi estornada.
		PedidoContabilizado pedido = new PedidoContabilizado(1001, 1);
		pedido.setEstornado(true);

		when(pedidoRepository.findByIdPedido(1001)).thenReturn(Optional.of(pedido));

		// O cliente possui 9 estrelas após o primeiro estorno.
		service.estornarPedido(1001);

		// Um novo estorno não deve alterar o progresso.
		verify(progressoRepository, never()).findByIdCliente(anyInt());

		verify(progressoRepository, never()).save(any(ProgressoFidelidade.class));

		verify(pedidoRepository, never()).save(any(PedidoContabilizado.class));
	}

	@Test
	void naoDeveEstornarPedidoInexistente() {

		// Simula os repositórios sem acessar o MySQL.
		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);

		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);

		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Simula um pedido que não existe no histórico.
		when(pedidoRepository.findByIdPedido(9999)).thenReturn(Optional.empty());

		// O cancelamento deve ser ignorado sem gerar erros.
		assertDoesNotThrow(() -> service.estornarPedido(9999));

		// Nenhum progresso ou pedido deve ser alterado.
		verify(progressoRepository, never()).save(any(ProgressoFidelidade.class));

		verify(pedidoRepository, never()).save(any(PedidoContabilizado.class));
	}

	@Test
	void deveEstornarPedidoAntigoAposResgate() {

		// Simula os repositórios sem acessar o MySQL.
		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);

		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);

		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Pedido antigo, contabilizado antes do resgate do brinde.
		PedidoContabilizado pedidoAntigo = new PedidoContabilizado(1001, 1);

		// Cliente já resgatou um brinde e acumulou 3 novas estrelas.
		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(3);
		progresso.setBrindePendente(false);

		when(pedidoRepository.findByIdPedido(1001)).thenReturn(Optional.of(pedidoAntigo));

		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		// Cancela um pedido pertencente ao ciclo anterior.
		service.estornarPedido(1001);

		// Deve retirar uma estrela do progresso atual.
		assertEquals(2, progresso.getEstrelas());
		assertFalse(progresso.getBrindePendente());

		// O pedido antigo deve ser marcado como estornado.
		assertTrue(pedidoAntigo.getEstornado());

		// O histórico de resgates não deve ser alterado.
		verifyNoInteractions(resgateRepository);

		// Confirma que as alterações foram salvas.
		verify(progressoRepository).save(progresso);
		verify(pedidoRepository).save(pedidoAntigo);
	}

	@Test
	void naoDeveDeixarEstrelasNegativasAoEstornar() {

		// Simula os repositórios sem acessar o MySQL.
		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);

		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);

		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Pedido antigo, contabilizado antes do resgate.
		PedidoContabilizado pedido = new PedidoContabilizado(1001, 1);

		// Cliente já resgatou o brinde e está com zero estrelas.
		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(0);
		progresso.setBrindePendente(false);

		when(pedidoRepository.findByIdPedido(1001)).thenReturn(Optional.of(pedido));

		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		// Simula o cancelamento do pedido antigo.
		service.estornarPedido(1001);

		// A quantidade de estrelas não pode ficar negativa.
		assertEquals(0, progresso.getEstrelas());
		assertEquals(1, progresso.getEstrelasPendentesEstorno());
		assertFalse(progresso.getBrindePendente());

		// O pedido deve ser marcado como estornado.
		assertTrue(pedido.getEstornado());

		// Confirma que as alterações foram salvas.
		verify(progressoRepository).save(progresso);
		verify(pedidoRepository).save(pedido);
	}

	@Test
	void deveCompensarEstornoPendente() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Cliente com zero estrelas e um estorno pendente.
		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(0);
		progresso.setEstrelasPendentesEstorno(1);

		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		// O pedido deve compensar a pendência sem gerar estrela.
		service.contabilizarPedido(1012, 1);

		assertEquals(0, progresso.getEstrelas());
		assertEquals(0, progresso.getEstrelasPendentesEstorno());

		verify(pedidoRepository).save(any(PedidoContabilizado.class));
		verify(progressoRepository).save(progresso);
	}

	@Test
	void deveGerarEstrelaAposCompensarEstorno() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Cliente já compensou a pendência e está com zero estrelas.
		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(0);
		progresso.setEstrelasPendentesEstorno(0);

		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		// Um novo pedido válido deve gerar uma estrela.
		service.contabilizarPedido(1013, 1);

		assertEquals(1, progresso.getEstrelas());
		assertEquals(0, progresso.getEstrelasPendentesEstorno());
		assertFalse(progresso.getBrindePendente());

		verify(pedidoRepository).save(any(PedidoContabilizado.class));
		verify(progressoRepository).save(progresso);
	}

	@Test
	void naoDeveAcumularEstrelasComBrindePendente() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Cliente com 10 estrelas e brinde disponível.
		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(10);
		progresso.setBrindePendente(true);

		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		// Um novo pedido não deve gerar outra estrela.
		service.contabilizarPedido(1014, 1);

		assertEquals(10, progresso.getEstrelas());
		assertTrue(progresso.getBrindePendente());

		// O pedido não deve ser contabilizado enquanto houver brinde.
		verify(pedidoRepository, never()).save(any(PedidoContabilizado.class));

		verify(progressoRepository, never()).save(any(ProgressoFidelidade.class));
	}

	@Test
	void deveLiberarBrindeAoCompletarDezEstrelas() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Cliente possui 9 estrelas e nenhum brinde pendente.
		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(9);
		progresso.setBrindePendente(false);

		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		// O décimo pedido deve liberar o brinde.
		service.contabilizarPedido(1015, 1);

		assertEquals(10, progresso.getEstrelas());
		assertTrue(progresso.getBrindePendente());

		verify(progressoRepository).save(progresso);
		verify(pedidoRepository).save(any(PedidoContabilizado.class));
	}

	@Test
	void naoDeveContabilizarPedidoDuplicado() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Simula um pedido que já foi contabilizado.
		when(pedidoRepository.existsByIdPedido(1016)).thenReturn(true);

		// Tenta contabilizar novamente o mesmo pedido.
		service.contabilizarPedido(1016, 1);

		// Não deve consultar nem alterar o progresso.
		verifyNoInteractions(progressoRepository);

		// Não deve registrar outro pedido.
		verify(pedidoRepository, never()).save(any(PedidoContabilizado.class));
	}

	@Test
	void deveCriarProgressoParaClienteNovo() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Cliente ainda não possui progresso cadastrado.
		when(progressoRepository.findByIdCliente(2)).thenReturn(Optional.empty());

		// Primeiro pedido entregue do cliente.
		service.contabilizarPedido(2001, 2);

		// Verifica se o progresso foi criado com uma estrela.
		verify(progressoRepository).save(argThat(progresso -> progresso.getIdCliente().equals(2)
				&& progresso.getEstrelas() == 1 && !progresso.getBrindePendente()));

		verify(pedidoRepository).save(any(PedidoContabilizado.class));
	}

	@Test
	void deveCompensarUmEstornoPorPedido() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Cliente com duas pendências e nenhuma estrela.
		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(0);
		progresso.setEstrelasPendentesEstorno(2);

		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		// Primeiro pedido compensa uma pendência.
		service.contabilizarPedido(1020, 1);

		assertEquals(0, progresso.getEstrelas());
		assertEquals(1, progresso.getEstrelasPendentesEstorno());

		// Segundo pedido compensa a pendência restante.
		service.contabilizarPedido(1021, 1);

		assertEquals(0, progresso.getEstrelas());
		assertEquals(0, progresso.getEstrelasPendentesEstorno());

		verify(pedidoRepository, times(2)).save(any(PedidoContabilizado.class));
	}

	@Test
	void deveAcumularEstornosPendentes() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Cliente sem estrelas disponíveis.
		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(0);

		PedidoContabilizado pedido1 = new PedidoContabilizado(1001, 1);
		PedidoContabilizado pedido2 = new PedidoContabilizado(1002, 1);

		when(pedidoRepository.findByIdPedido(1001)).thenReturn(Optional.of(pedido1));
		when(pedidoRepository.findByIdPedido(1002)).thenReturn(Optional.of(pedido2));
		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		// Cancela dois pedidos antigos.
		service.estornarPedido(1001);
		service.estornarPedido(1002);

		assertEquals(0, progresso.getEstrelas());
		assertEquals(2, progresso.getEstrelasPendentesEstorno());
		assertTrue(pedido1.getEstornado());
		assertTrue(pedido2.getEstornado());

		verify(pedidoRepository, times(2)).save(any(PedidoContabilizado.class));
	}

	@Test
	void naoDeveAcumularEstornoAoCancelarDuasVezes() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(0);

		PedidoContabilizado pedido = new PedidoContabilizado(1001, 1);

		when(pedidoRepository.findByIdPedido(1001)).thenReturn(Optional.of(pedido));
		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		// Tenta cancelar o mesmo pedido duas vezes.
		service.estornarPedido(1001);
		service.estornarPedido(1001);

		// Apenas uma pendência deve ser registrada.
		assertEquals(1, progresso.getEstrelasPendentesEstorno());
		assertTrue(pedido.getEstornado());

		verify(progressoRepository, times(1)).save(progresso);
		verify(pedidoRepository, times(1)).save(pedido);
	}

	@Test
	void deveAdicionarUmaEstrelaPorPedido() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Cliente possui quatro estrelas.
		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(4);

		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		// Contabiliza um novo pedido.
		service.contabilizarPedido(1030, 1);

		assertEquals(5, progresso.getEstrelas());
		assertFalse(progresso.getBrindePendente());

		verify(progressoRepository, times(1)).save(progresso);
		verify(pedidoRepository, times(1)).save(any(PedidoContabilizado.class));
	}

	@Test
	void deveAcumularEstrelasAposResgatarBrinde() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Cliente já possui um brinde disponível.
		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(10);
		progresso.setBrindePendente(true);

		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		when(configuracaoBrindeService.consultarProdutoBrinde()).thenReturn(5);

		when(configuracaoBrindeService.consultarPrecoBrinde()).thenReturn(new BigDecimal("25.00"));

		// simula o pedido de resgate com outro item pago
		Map<String, Object> pedidoSimulado = Map.of(
		        "idPedido", 1040,
		        "idCliente", 1,
		        "itens", List.of(
		                Map.of(
		                        "idProduto", 5,
		                        "quantidade", 1,
		                        "precoUnitario", 25.00
		                ),
		                Map.of(
		                        "idProduto", 8,
		                        "quantidade", 1,
		                        "precoUnitario", 18.00
		                )
		        )
		);

		when(monolitoApiClient.buscarPedido(1040))
		        .thenReturn(pedidoSimulado);
		
		// Resgata o brinde e reinicia a contagem.
		service.resgatarBrinde(1040, 1, 5, new BigDecimal("25.00"));

		assertEquals(0, progresso.getEstrelas());
		assertFalse(progresso.getBrindePendente());

		// Um novo pedido gera a primeira estrela do próximo ciclo.
		service.contabilizarPedido(1041, 1);

		assertEquals(1, progresso.getEstrelas());
		assertFalse(progresso.getBrindePendente());

		verify(resgateRepository).save(any(ResgateBrinde.class));
		verify(pedidoRepository).save(any(PedidoContabilizado.class));
	}

	@Test
	void deveManterBrindePendenteSemResgate() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Cliente possui um brinde disponível.
		ProgressoFidelidade progresso = new ProgressoFidelidade(1);
		progresso.setEstrelas(10);
		progresso.setBrindePendente(true);

		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(progresso));

		// Pedido entregue sem utilizar o brinde.
		service.contabilizarPedido(1050, 1);

		// O benefício permanece disponível.
		assertEquals(10, progresso.getEstrelas());
		assertTrue(progresso.getBrindePendente());

		verifyNoInteractions(resgateRepository);
		verify(pedidoRepository, never()).save(any(PedidoContabilizado.class));
	}

	@Test
	void naoDeveAlterarProgressoDeOutroCliente() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);

		// Cliente 1 possui um brinde disponível.
		ProgressoFidelidade cliente1 = new ProgressoFidelidade(1);
		cliente1.setEstrelas(10);
		cliente1.setBrindePendente(true);

		// Cliente 2 possui cinco estrelas.
		ProgressoFidelidade cliente2 = new ProgressoFidelidade(2);
		cliente2.setEstrelas(5);

		when(progressoRepository.findByIdCliente(1)).thenReturn(Optional.of(cliente1));

		when(configuracaoBrindeService.consultarProdutoBrinde()).thenReturn(5);

		when(configuracaoBrindeService.consultarPrecoBrinde()).thenReturn(new BigDecimal("25.00"));

		
		// simula um pedido do cliente 1 com brinde e item pago
		Map<String, Object> pedidoSimulado = Map.of(
		        "idPedido", 1060,
		        "idCliente", 1,
		        "itens", List.of(
		                Map.of(
		                        "idProduto", 5,
		                        "quantidade", 1,
		                        "precoUnitario", 25.00
		                ),
		                Map.of(
		                        "idProduto", 8,
		                        "quantidade", 1,
		                        "precoUnitario", 18.00
		                )
		        )
		);

		when(monolitoApiClient.buscarPedido(1060))
		        .thenReturn(pedidoSimulado);
		
		// Resgata somente o brinde do cliente 1.
		service.resgatarBrinde(1060, 1, 5, new BigDecimal("25.00"));

		assertEquals(0, cliente1.getEstrelas());
		assertFalse(cliente1.getBrindePendente());

		assertEquals(5, cliente2.getEstrelas());
		assertFalse(cliente2.getBrindePendente());

		verify(progressoRepository).save(cliente1);
		verify(progressoRepository, never()).save(cliente2);
	}

	@Test
	void naoDeveResgatarBrindeDeClienteSemProgresso() {

		PedidoContabilizadoRepository pedidoRepository = mock(PedidoContabilizadoRepository.class);
		ProgressoFidelidadeRepository progressoRepository = mock(ProgressoFidelidadeRepository.class);
		ResgateBrindeRepository resgateRepository = mock(ResgateBrindeRepository.class);

		ConfiguracaoBrindeService configuracaoBrindeService = mock(ConfiguracaoBrindeService.class);

		FidelidadeService service = new FidelidadeService(pedidoRepository, progressoRepository, resgateRepository,
				configuracaoBrindeService, monolitoApiClient);
		// Cliente não possui progresso cadastrado.
		when(progressoRepository.findByIdCliente(99)).thenReturn(Optional.empty());

		// O resgate deve ser recusado.
		assertThrows(IllegalStateException.class, () -> service.resgatarBrinde(1070, 99, 5, new BigDecimal("25.00")));

		// Nenhum brinde ou progresso deve ser salvo.
		verify(resgateRepository, never()).save(any(ResgateBrinde.class));

		verify(progressoRepository, never()).save(any(ProgressoFidelidade.class));
	}

}
