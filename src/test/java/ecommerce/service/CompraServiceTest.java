package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;

class CompraServiceTest extends CompraServiceBaseTest{
	/* 
	@Test
	public void calcularCustoTotal()
	{
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();

		List<ItemCompra> itens = new ArrayList<>();

		ItemCompra item1 = new ItemCompra();
		ItemCompra item2 = new ItemCompra();
		ItemCompra item3 = new ItemCompra();
		// To-Do : falta setar os atributos dos itens
		itens.add(item1);
		itens.add(item2);
		itens.add(item3);
		carrinho.setItens(itens);

		BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.NORDESTE, TipoCliente.OURO);

		// Ao trabalhar com BigDecimal, evite comparar com equals() -- método que o
		// assertEquals usa,
		// pois ela leva em conta escala (ex: 10.0 != 10.00).
		// Use o método compareTo().
		BigDecimal esperado = new BigDecimal("0.00");
		assertEquals(0, custoTotal.compareTo(esperado), "Valor calculado incorreto: " + custoTotal);

		// Uma alternativa mais elegante, é usar a lib AssertJ
		// O método isEqualByComparingTo não leva em conta escala
		// e não precisa instanciar um BigDecimal para fazer a comparação
		assertThat(custoTotal).as("Custo Total da Compra").isEqualByComparingTo("0.0");
	}
	*/
	@Test
    @DisplayName("Deve retornar zero se o carrinho estiver vazio")
    void calcularCustoTotal_CarrinhoVazio() {
		carrinhoPadrao.setItens(Collections.emptyList());

		BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.SUL, TipoCliente.OURO);

		assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
    @DisplayName("Desconto 10% (> 500 e < 1000) e Frete Faixa B (> 5kg e <= 10kg)")
    void calcularCustoTotal_Desconto10_FreteFaixaB() {
        // Subtotal 600 - 10% (60) = 540
        // Frete (8kg * 2.00) = 16
        // Total esperado = 556.00
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("600.00"), new BigDecimal("8.0"), false, 1L)
        );

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.NORDESTE, TipoCliente.PRATA);

        assertThat(total).isEqualByComparingTo("556.00");
    }
}
