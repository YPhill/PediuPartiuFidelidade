
package br.com.pediupartiu.fidelidade.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.pediupartiu.fidelidade.entity.PedidoContabilizado;
import br.com.pediupartiu.fidelidade.entity.ProgressoFidelidade;
import br.com.pediupartiu.fidelidade.entity.ResgateBrinde;
import br.com.pediupartiu.fidelidade.integracao.MonolitoApiClient;
import br.com.pediupartiu.fidelidade.repository.PedidoContabilizadoRepository;
import br.com.pediupartiu.fidelidade.repository.ProgressoFidelidadeRepository;
import br.com.pediupartiu.fidelidade.repository.ResgateBrindeRepository;
import java.util.Map;
import java.util.List;

// Centraliza as regras de negócio do programa de fidelidade.
@Service
public class FidelidadeService {

	private final ResgateBrindeRepository resgateRepository;
    private final PedidoContabilizadoRepository pedidoRepository;
    private final ProgressoFidelidadeRepository progressoRepository;
    private final ConfiguracaoBrindeService configuracaoBrindeService;
    private final MonolitoApiClient monolitoApiClient;
    

    // Recebe os repositórios necessários para acessar o banco.
    public FidelidadeService(
            PedidoContabilizadoRepository pedidoRepository,
            ProgressoFidelidadeRepository progressoRepository,
            ResgateBrindeRepository resgateRepository,
            ConfiguracaoBrindeService configuracaoBrindeService,
            MonolitoApiClient monolitoApiClient) {

        this.pedidoRepository = pedidoRepository;
        this.progressoRepository = progressoRepository;
        this.resgateRepository = resgateRepository;
        this.configuracaoBrindeService = configuracaoBrindeService;
        this.monolitoApiClient = monolitoApiClient;
    }

    

    
    
    // Contabiliza um pedido entregue no progresso do cliente.
    @Transactional
    public void contabilizarPedido(Integer idPedido, Integer idCliente) {
    	
    	// pedidos com brinde não geram estrelas
    	if (resgateRepository.existsByIdPedido(idPedido)) {
    	    return;
    	}

    	// verifica se o pedido já foi contabilizado
    	PedidoContabilizado pedidoExistente = pedidoRepository
    	        .findByIdPedido(idPedido)
    	        .orElse(null);

    	// impede contabilização duplicada
    	if (pedidoExistente != null && !pedidoExistente.getEstornado()) {
    	    return;
    	}
    	
    	// reutiliza o registro quando o pedido foi estornado
    	PedidoContabilizado pedidoContabilizado;

    	if (pedidoExistente != null) {
    	    pedidoContabilizado = pedidoExistente;
    	    pedidoContabilizado.setEstornado(false);
    	} else {
    	    pedidoContabilizado = new PedidoContabilizado(idPedido, idCliente);
    	}

        // Recupera o progresso ou cria um novo para o cliente.
        ProgressoFidelidade progresso = progressoRepository
                .findByIdCliente(idCliente)
                .orElseGet(() -> new ProgressoFidelidade(idCliente));

        // Enquanto houver brinde pendente, não acumula estrelas.
        if (progresso.getBrindePendente()) {
            return;
        }


        // Compensa primeiro os estornos pendentes.
        if (progresso.getEstrelasPendentesEstorno() > 0) {
            int pendencias = progresso.getEstrelasPendentesEstorno();
            progresso.setEstrelasPendentesEstorno(pendencias - 1);

            // Registra o pedido para impedir nova contabilização.
            progressoRepository.save(progresso);
            pedidoRepository.save(pedidoContabilizado);
            return;
        }

        // Sem pendências, o pedido gera uma nova estrela.
        int novasEstrelas = progresso.getEstrelas() + 1;
        progresso.setEstrelas(novasEstrelas);


        // Ao atingir 10 estrelas, libera um brinde.
        if (novasEstrelas >= 10) {
            progresso.setBrindePendente(true);
        }

        // Salva o progresso e registra o pedido contabilizado.
        progressoRepository.save(progresso);
        pedidoRepository.save(pedidoContabilizado);
    }
    


    // Confirma o resgate do brinde e reinicia as estrelas.

@Transactional
public void resgatarBrinde(Integer idPedido, Integer idCliente,
                          Integer idProdutoBrinde, BigDecimal valorDesconto) {
	
	// valida os identificadores recebidos
	if (idPedido == null || idPedido <= 0 || idCliente == null || idCliente <= 0) {
	    throw new IllegalArgumentException("Pedido ou cliente inválido.");
	}

    // Impede resgates duplicados.
    if (resgateRepository.existsByIdPedido(idPedido)) {
        throw new IllegalStateException(
                "Este pedido já possui um brinde resgatado.");
    }

    // Verifica se o cliente possui brinde disponível.
    ProgressoFidelidade progresso = progressoRepository
            .findByIdCliente(idCliente)
            .orElseThrow(() -> new IllegalStateException(
                    "Cliente sem progresso de fidelidade."));

    if (!progresso.getBrindePendente()) {
        throw new IllegalStateException(
                "O cliente não possui brinde disponível.");
    }

    // Valida os dados recebidos.
    if (idProdutoBrinde == null || idProdutoBrinde <= 0) {
        throw new IllegalArgumentException(
                "Produto do brinde inválido.");
    }

    if (valorDesconto == null ||
            valorDesconto.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException(
                "Valor do desconto inválido.");
    }

    // Confere o produto configurado.
    Integer produtoConfigurado =
            configuracaoBrindeService.consultarProdutoBrinde();

    if (produtoConfigurado == null ||
            !produtoConfigurado.equals(idProdutoBrinde)) {
        throw new IllegalArgumentException(
                "O produto informado não é o brinde configurado.");
    }

    // Confere o preço real no monólito.
    BigDecimal precoReal =
            configuracaoBrindeService.consultarPrecoBrinde();

    if (valorDesconto.compareTo(precoReal) != 0) {
        throw new IllegalArgumentException(
                "O desconto deve corresponder ao preço do brinde.");
    }

 // valida os dados reais do pedido no monólito

    Map<String, Object> dadosPedido = monolitoApiClient.buscarPedido(idPedido);

    if (dadosPedido == null || dadosPedido.get("idCliente") == null) {
        throw new IllegalStateException("Pedido não encontrado no monólito.");
    }

    Integer clientePedido = ((Number) dadosPedido.get("idCliente")).intValue();

    if (!clientePedido.equals(idCliente)) {
        throw new IllegalStateException(
                "O pedido não pertence ao cliente informado.");
    }

    // verifica os produtos e as quantidades do pedido
    Object itensRecebidos = dadosPedido.get("itens");

    if (!(itensRecebidos instanceof List<?>)) {
        throw new IllegalStateException("Pedido sem itens válidos.");
    }

    List<?> itens = (List<?>) itensRecebidos;

    boolean encontrouBrinde = false;
    boolean possuiItemPago = false;

    for (Object objetoItem : itens) {

        if (!(objetoItem instanceof Map<?, ?>)) {
            throw new IllegalStateException("Item do pedido inválido.");
        }

        Map<?, ?> item = (Map<?, ?>) objetoItem;

        Number produto = (Number) item.get("idProduto");
        Number quantidade = (Number) item.get("quantidade");
        Object precoRecebido = item.get("precoUnitario");

        if (produto == null || quantidade == null || precoRecebido == null) {
            throw new IllegalStateException("Dados incompletos do pedido.");
        }

        Integer idProduto = produto.intValue();
        int quantidadeItem = quantidade.intValue();
        BigDecimal precoUnitario = new BigDecimal(precoRecebido.toString());

        if (quantidadeItem <= 0 || precoUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Quantidade ou preço inválido.");
        }

        if (idProduto.equals(idProdutoBrinde)) {

            encontrouBrinde = true;

            // apenas uma unidade será gratuita
            if (quantidadeItem > 1 && precoUnitario.compareTo(BigDecimal.ZERO) > 0) {
                possuiItemPago = true;
            }

        } else if (precoUnitario.compareTo(BigDecimal.ZERO) > 0) {
            possuiItemPago = true;
        }
    }

    if (!encontrouBrinde) {
        throw new IllegalStateException(
                "O pedido não contém o produto configurado como brinde.");
    }

    if (!possuiItemPago) {
        throw new IllegalStateException(
                "O pedido precisa conter pelo menos um item pago além do brinde.");
    }
    
    // Registra o resgate somente após todas as validações.
    ResgateBrinde resgate = new ResgateBrinde(idCliente, idPedido);
    resgate.setIdProdutoBrinde(idProdutoBrinde);
    resgate.setValorDesconto(precoReal);

    resgateRepository.save(resgate);

    // Reinicia a contagem.
    progresso.setEstrelas(0);
    progresso.setBrindePendente(false);

    progressoRepository.save(progresso);
}



// devolve o direito ao brinde de um pedido cancelado
@Transactional
public void devolverBrinde(Integer idPedido) {

    if (idPedido == null || idPedido <= 0) {
        throw new IllegalArgumentException("Pedido inválido.");
    }

    // consulta o resgate registrado
    ResgateBrinde resgate = resgateRepository.findByIdPedido(idPedido)
            .orElseThrow(() -> new IllegalStateException(
                    "Este pedido não possui brinde resgatado."));

    // impede a devolução duplicada
    if (Boolean.TRUE.equals(resgate.getDevolvido())) {
        return;
    }

    // verifica os dados atuais no monólito
    Map<String, Object> dadosPedido = monolitoApiClient.buscarPedido(idPedido);

    if (dadosPedido == null
            || !(dadosPedido.get("idCliente") instanceof Number)
            || !(dadosPedido.get("idStatus") instanceof Number)
            || !(dadosPedido.get("possuiPedidoPosterior") instanceof Boolean)) {
        throw new IllegalStateException(
                "Não foi possível validar os dados do pedido.");
    }

    Integer idCliente = ((Number) dadosPedido.get("idCliente")).intValue();
    Integer idStatus = ((Number) dadosPedido.get("idStatus")).intValue();

    boolean possuiPedidoPosterior =
            (Boolean) dadosPedido.get("possuiPedidoPosterior");

    if (!idCliente.equals(resgate.getIdCliente())) {
        throw new IllegalStateException(
                "O cliente do pedido não corresponde ao resgate.");
    }

    if (!idStatus.equals(9)) {
        throw new IllegalStateException(
                "O pedido precisa estar cancelado para devolver o brinde.");
    }

    if (possuiPedidoPosterior) {
        throw new IllegalStateException(
                "O cliente possui pedidos posteriores. Devolução não permitida.");
    }

    // recupera o progresso do cliente
    ProgressoFidelidade progresso = progressoRepository
            .findByIdCliente(idCliente)
            .orElseThrow(() -> new IllegalStateException(
                    "Progresso de fidelidade não encontrado."));

    // evita sobrescrever outro benefício
    if (Boolean.TRUE.equals(progresso.getBrindePendente())
            || progresso.getEstrelas() != 0
            || progresso.getEstrelasPendentesEstorno() != 0) {
        throw new IllegalStateException(
                "O progresso do cliente não permite devolver o brinde.");
    }

    // restaura o direito ao brinde
    progresso.setEstrelas(10);
    progresso.setBrindePendente(true);

    resgate.setDevolvido(true);

    progressoRepository.save(progresso);
    resgateRepository.save(resgate);
}



    

    // Estorna a estrela de um pedido contabilizado.
    @Transactional
    public void estornarPedido(Integer idPedido) {

        // Localiza o pedido no histórico da fidelidade.
        PedidoContabilizado pedido = pedidoRepository
                .findByIdPedido(idPedido)
                .orElse(null);

        // Se não foi contabilizado ou já foi estornado, não faz nada.
        if (pedido == null || pedido.getEstornado()) {
            return;
        }
        

        // Localiza o progresso do cliente responsável pelo pedido.
        ProgressoFidelidade progresso = progressoRepository
                .findByIdCliente(pedido.getIdCliente())
                .orElseThrow(() -> new IllegalStateException(
                        "Cliente sem progresso de fidelidade."));


        // Retira uma estrela ou registra uma pendência de estorno.
        int estrelasAtualizadas = progresso.getEstrelas();

        if (estrelasAtualizadas > 0) {
            estrelasAtualizadas--;
            progresso.setEstrelas(estrelasAtualizadas);
        } else {
            // Sem estrelas disponíveis, o desconto fica pendente.
            int pendencias = progresso.getEstrelasPendentesEstorno();
            progresso.setEstrelasPendentesEstorno(pendencias + 1);
        }


        // Se o cliente não possui mais 10 estrelas, remove o brinde pendente.
        if (estrelasAtualizadas < 10) {
            progresso.setBrindePendente(false);
        }

        // Marca o pedido como estornado para evitar descontos duplicados.
        pedido.setEstornado(true);

        // Salva as alterações no banco de dados.
        progressoRepository.save(progresso);
        pedidoRepository.save(pedido);

    }



}
