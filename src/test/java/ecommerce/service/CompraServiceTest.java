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

class CompraServiceTest extends CompraServiceBaseTest {

    // --------------------------------------------------------------------------
    // PARTE 1: TESTES DO MÉTODO calcularCustoTotal
    // --------------------------------------------------------------------------

    @Test
    @DisplayName("Deve retornar zero se o carrinho estiver vazio")
    void calcularCustoTotal_CarrinhoVazio() {
        carrinhoPadrao.setItens(Collections.emptyList());

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.SUL, TipoCliente.OURO);

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Sem desconto (< 500) e Frete Isento (<= 5kg)")
    void calcularCustoTotal_SemDesconto_FreteIsento() {
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("100.00"), new BigDecimal("2.0"), false, 1L)
        );

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.SUL, TipoCliente.BRONZE);

        assertThat(total).isEqualByComparingTo("100.00");
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

    @Test
    @DisplayName("Teste Limite 500: Exatamente R$ 500,00 não deve ter desconto")
    void calcularCustoTotal_Limite500_SemDesconto() {
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("100.00"), new BigDecimal("0.2"), false, 5L)
        );

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.NORDESTE, TipoCliente.PRATA);

        assertThat(total).isEqualByComparingTo("450.00");
    }

    @Test
    @DisplayName("Desconto 20% (>= 1000) e Frete Faixa C (> 10kg e <= 50kg)")
    void calcularCustoTotal_Desconto20_FreteFaixaC() {
        // Subtotal 1000 - 20% (200) = 800
        // Frete (20kg * 4.00) = 80
        // Total esperado = 880.00
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("1000.00"), new BigDecimal("20.0"), false, 1L)
        );

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.SUDESTE, TipoCliente.OURO);

        assertThat(total).isEqualByComparingTo("880.00");
    }

    @Test
    @DisplayName("Frete Faixa D (> 50kg) com Taxa de Fragilidade")
    void calcularCustoTotal_FreteFaixaD_ComFragil() {
        // Subtotal 100
        // Frete Base (60kg * 7.00) = 420
        // Taxa Frágil (1 * 5.00) = 5
        // Total esperado = 525.00
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("100.00"), new BigDecimal("60.0"), true, 1L)
        );

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.NORTE, TipoCliente.BRONZE);

        assertThat(total).isEqualByComparingTo("525.00");
    }

    // --------------------------------------------------------------------------
    // PARTE 2: TESTES DO MÉTODO finalizarCompra
    // --------------------------------------------------------------------------

    @Test
    @DisplayName("Deve finalizar compra com sucesso verificado argumentos exatos")
    void finalizarCompra_Sucesso() {
        // Cenário: Item ID 10, Qtd 2
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("100.00"), new BigDecimal("1.0"), false, 2L)
        );

        when(clienteService.buscarPorId(1L)).thenReturn(clientePadrao);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(1L, clientePadrao)).thenReturn(carrinhoPadrao);

        // Mocks retornando sucesso para QUALQUER lista (para o setup)
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
            .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));
        when(pagamentoExternal.autorizarPagamento(eq(1L), anyDouble()))
            .thenReturn(new PagamentoDTO(true, 12345L));
        when(estoqueExternal.darBaixa(anyList(), anyList()))
            .thenReturn(new EstoqueBaixaDTO(true));

        CompraDTO resultado = compraService.finalizarCompra(1L, 1L);

        assertThat(resultado.sucesso()).isTrue();
        
        // VERIFICAÇÃO RIGOROSA (Mata mutantes das linhas 49 e 51)
        // Garante que o serviço extraiu o ID 10 e Qtd 2, e não 0L ou null
        verify(estoqueExternal).verificarDisponibilidade(eq(List.of(10L)), eq(List.of(2L)));
        verify(estoqueExternal).darBaixa(eq(List.of(10L)), eq(List.of(2L)));
    }

    @Test
    @DisplayName("Deve lançar exceção se produto indisponível")
    void finalizarCompra_EstoqueIndisponivel() {
        configurarItensNoCarrinho(criarItem(BigDecimal.TEN, BigDecimal.ONE, false, 1L));

        when(clienteService.buscarPorId(1L)).thenReturn(clientePadrao);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(1L, clientePadrao)).thenReturn(carrinhoPadrao);

        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
            .thenReturn(new DisponibilidadeDTO(false, List.of(99L)));

        assertThrows(IllegalStateException.class, () -> compraService.finalizarCompra(1L, 1L));

        verify(pagamentoExternal, never()).autorizarPagamento(any(), any());
    }

    @Test
    @DisplayName("Deve fazer rollback se baixa de estoque falhar após pagamento")
    void finalizarCompra_RollbackPagamento() {
        configurarItensNoCarrinho(criarItem(BigDecimal.TEN, BigDecimal.ONE, false, 1L));

        when(clienteService.buscarPorId(1L)).thenReturn(clientePadrao);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(1L, clientePadrao)).thenReturn(carrinhoPadrao);
        
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
            .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));

        when(pagamentoExternal.autorizarPagamento(eq(1L), anyDouble()))
            .thenReturn(new PagamentoDTO(true, 999L)); 

        when(estoqueExternal.darBaixa(anyList(), anyList()))
            .thenReturn(new EstoqueBaixaDTO(false)); 

        assertThrows(IllegalStateException.class, () -> compraService.finalizarCompra(1L, 1L));

        verify(pagamentoExternal).cancelarPagamento(1L, 999L);
    }

	@Test
    @DisplayName("Deve lançar exceção quando o pagamento não for autorizado")
    void finalizarCompra_ErroPagamentoNaoAutorizado() {
        // 1. Arrange: Configura dados básicos (Carrinho e Cliente)
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("100.00"), BigDecimal.ONE, false, 1L)
        );

        // Mocks de dados básicos
        when(clienteService.buscarPorId(1L)).thenReturn(clientePadrao);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(1L, clientePadrao)).thenReturn(carrinhoPadrao);

        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
            .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));

        // 2. Arrange: Configura o Mock para RECUSAR o pagamento (autorizado = false)
        when(pagamentoExternal.autorizarPagamento(eq(1L), anyDouble()))
            .thenReturn(new PagamentoDTO(false, null)); // <--- Isso aciona o 'if (!pagamento.autorizado())'

        // 3. Act & Assert: Verifica se a exceção é lançada
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            compraService.finalizarCompra(1L, 1L);
        });

        // Verifica a mensagem exata da exceção
        assertThat(exception.getMessage()).isEqualTo("Pagamento não autorizado.");

        // 4. Assert: Garante que, como falhou no pagamento, NÃO tentou dar baixa no estoque
        verify(estoqueExternal, never()).darBaixa(anyList(), anyList());
    }

	@Test
    @DisplayName("Borda 5kg: Exatamente 5kg deve ser isento (Faixa A)")
    void calcularCustoTotal_Borda5kg_Isento() {
        // Cenário: 1 produto de R$ 100,00 e peso EXATO 5.0kg
        // Regra: peso > 5 paga. Peso 5 é isento.
        // Se o mutante mudar para 'peso >= 5', este teste falha (matando o mutante).
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("100.00"), new BigDecimal("5.0"), false, 1L)
        );

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.SUL, TipoCliente.BRONZE);

        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Borda 10kg: Exatamente 10kg deve pagar R$ 2,00/kg (Faixa B)")
    void calcularCustoTotal_Borda10kg_FaixaB() {
        // Cenário: 1 produto de R$ 100,00 e peso EXATO 10.0kg
        // Regra: peso > 10 paga R$ 4,00. Peso 10 paga R$ 2,00.
        // Cálculo: 10kg * 2.00 = R$ 20.00 de frete. Total = 120.00.
        // Se o mutante mudar para 'peso >= 10' (entrando na faixa C de R$ 4,00), o total seria 140.00 e o teste falha.
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("100.00"), new BigDecimal("10.0"), false, 1L)
        );

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.NORDESTE, TipoCliente.OURO);

        assertThat(total).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("Borda 50kg: Exatamente 50kg deve pagar R$ 4,00/kg (Faixa C)")
    void calcularCustoTotal_Borda50kg_FaixaC() {
        // Cenário: 1 produto de R$ 100,00 e peso EXATO 50.0kg
        // Regra: peso > 50 paga R$ 7,00. Peso 50 paga R$ 4,00.
        // Cálculo: 50kg * 4.00 = R$ 200.00 de frete. Total = 300.00.
        // Se o mutante mudar para 'peso >= 50' (entrando na faixa D de R$ 7,00), o total seria 450.00 e o teste falha.
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("100.00"), new BigDecimal("50.0"), false, 1L)
        );

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.NORTE, TipoCliente.PRATA);

        assertThat(total).isEqualByComparingTo("300.00");
    }

	// --------------------------------------------------------------------------
    //  MATANDO MUTANTES QUE SOBRARAM
    // --------------------------------------------------------------------------

	@Test
    @DisplayName("Matar Mutante Linha 99: Peso deve ser multiplicado pela quantidade (não dividido)")
    void calcularCustoTotal_QuantidadeMaiorQueUm() {
        // Cenário: Peso 10kg, Quantidade 2.
        // Correto: Total 20kg (Faixa C -> Frete R$ 80,00).
        // Mutante (Divisão): 10 / 2 = 5kg (Faixa A -> Isento).
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("10.00"), new BigDecimal("10.0"), false, 2L)
        );

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.SUL, TipoCliente.BRONZE);

        // Subtotal: 20.00
        // Frete: 20kg * 4.00 = 80.00
        // Total esperado: 100.00
        assertThat(total).isEqualByComparingTo("100.00");
    }

	@Test
    @DisplayName("Deve retornar zero se a lista de itens for nula")
    void calcularCustoTotal_ListaItensNula() {
        carrinhoPadrao.setItens(null); // Força o null explicitamente

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.SUL, TipoCliente.OURO);

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

	


}