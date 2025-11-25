package ecommerce.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import jakarta.transaction.Transactional;

@Service
public class CompraService
{

	private final CarrinhoDeComprasService carrinhoService;
	private final ClienteService clienteService;

	private final IEstoqueExternal estoqueExternal;
	private final IPagamentoExternal pagamentoExternal;

	@Autowired
	public CompraService(CarrinhoDeComprasService carrinhoService, ClienteService clienteService,
			IEstoqueExternal estoqueExternal, IPagamentoExternal pagamentoExternal)
	{
		this.carrinhoService = carrinhoService;
		this.clienteService = clienteService;

		this.estoqueExternal = estoqueExternal;
		this.pagamentoExternal = pagamentoExternal;
	}

	@Transactional
	public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId)
	{
		Cliente cliente = clienteService.buscarPorId(clienteId);
		CarrinhoDeCompras carrinho = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

		List<Long> produtosIds = carrinho.getItens().stream().map(i -> i.getProduto().getId())
				.collect(Collectors.toList());
		List<Long> produtosQtds = carrinho.getItens().stream().map(i -> i.getQuantidade()).collect(Collectors.toList());

		DisponibilidadeDTO disponibilidade = estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds);

		if (!disponibilidade.disponivel())
		{
			throw new IllegalStateException("Itens fora de estoque.");
		}

		BigDecimal custoTotal = calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());

		PagamentoDTO pagamento = pagamentoExternal.autorizarPagamento(cliente.getId(), custoTotal.doubleValue());

		if (!pagamento.autorizado())
		{
			throw new IllegalStateException("Pagamento não autorizado.");
		}

		EstoqueBaixaDTO baixaDTO = estoqueExternal.darBaixa(produtosIds, produtosQtds);

		if (!baixaDTO.sucesso())
		{
			pagamentoExternal.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
			throw new IllegalStateException("Erro ao dar baixa no estoque.");
		}

		CompraDTO compraDTO = new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");

		return compraDTO;
	} 

	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente){
		if (carrinho == null|| carrinho.getItens() == null || carrinho.getItens().isEmpty()){
			return BigDecimal.ZERO;
		}

		// 1. Calcular Subtotal (Soma: Preço * Quantidade)
		BigDecimal subtotal = BigDecimal.ZERO;
		double pesoTotal = 0.0;
		long quantidadeFrageis = 0;

		for (var item : carrinho.getItens()) {
			BigDecimal preco = item.getProduto().getPreco();
			BigDecimal quantidade = BigDecimal.valueOf(item.getQuantidade());

			subtotal = subtotal.add(preco.multiply(quantidade));

			// Acumulara peso físcio total
			pesoTotal += item.getProduto().getPesoFisico().doubleValue() * item.getQuantidade();

			// Contar itens frágeis
			if (Boolean.TRUE.equals(item.getProduto().isFragil())) {
				quantidadeFrageis += item.getQuantidade();
			}
		}

		// 2. Aplicar Desconto por Valor Total
		// >= 1000: 20% | >= 500 e < 1000: 10% | Outros: 0%
		BigDecimal desconto = BigDecimal.ZERO;
		if (subtotal.compareTo(BigDecimal.valueOf(1000.00)) >= 0) {
			desconto = subtotal.multiply(BigDecimal.valueOf(0.20));
		} else if (subtotal.compareTo(BigDecimal.valueOf(500.00)) >= 0) {
			desconto = subtotal.multiply(BigDecimal.valueOf(0.10));
		}

		BigDecimal subtotalComDesconto = subtotal.subtract(desconto);

		// 3. Calcular Frete por Peso
		BigDecimal Valorfrete = BigDecimal.ZERO;

		if (pesoTotal > 50){
			Valorfrete = BigDecimal.valueOf(pesoTotal).multiply(new BigDecimal("7.00")); // Faixa D (50 ou mais)
		} else if (pesoTotal > 10){
			Valorfrete = BigDecimal.valueOf(pesoTotal).multiply(new BigDecimal("4.00")); // Faixa C (10-50kg)
		} else if (pesoTotal > 5){
			Valorfrete = BigDecimal.valueOf(pesoTotal).multiply(new BigDecimal("2.00")); // Faixa B (5-10kg)
		}
		// Faixa de A (0-5kg) está isenta de frete

		// 4. Adicionar Taxa para Itens Frágeis (5 reais por item)
		if (quantidadeFrageis > 0){
			BigDecimal adicionalFragil = BigDecimal.valueOf(quantidadeFrageis).multiply(new BigDecimal("5.00"));
			Valorfrete = Valorfrete.add(adicionalFragil);
		}

		//4. Total Final arrendodado
		BigDecimal totalFinal = subtotalComDesconto.add(Valorfrete);
		return totalFinal.setScale(2, java.math.RoundingMode.HALF_UP);
	}
}
