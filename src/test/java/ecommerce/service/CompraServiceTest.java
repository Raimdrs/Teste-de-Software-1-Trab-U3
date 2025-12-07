package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;

class CompraServiceTest extends CompraServiceBaseTest {

    // --------------------------------------------------------------------------
    // TESTES DO MÉTODO calcularCustoTotal
    // --------------------------------------------------------------------------

    @Test
    @DisplayName("Deve retornar zero (escala 0) se o carrinho estiver vazio")
    void calcularCustoTotal_CarrinhoVazio() {
        carrinhoPadrao.setItens(Collections.emptyList());

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.SUL, TipoCliente.OURO);

        // Usamos isEqualTo para obrigar que seja BigDecimal.ZERO estrito (escala 0)
        // Se o código cair no cálculo final, virá 0.00 (escala 2) e o teste falhará, matando o mutante.
        assertThat(total).isEqualTo(BigDecimal.ZERO);
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
    @DisplayName("Desconto 10% (>= 500 e < 1000) e Frete Faixa B (> 5kg e <= 10kg)")
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
    @DisplayName("Teste Limite 500: Exatamente R$ 500,00 deve ter 10% de desconto")
    void calcularCustoTotal_Limite500_ComDesconto() {
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
    // TESTES DO MÉTODO finalizarCompra
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

        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
            .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));
        when(pagamentoExternal.autorizarPagamento(eq(1L), anyDouble()))
            .thenReturn(new PagamentoDTO(true, 12345L));
        when(estoqueExternal.darBaixa(anyList(), anyList()))
            .thenReturn(new EstoqueBaixaDTO(true));

        CompraDTO resultado = compraService.finalizarCompra(1L, 1L);

        assertThat(resultado.sucesso()).isTrue();
        
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
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("100.00"), BigDecimal.ONE, false, 1L)
        );

        when(clienteService.buscarPorId(1L)).thenReturn(clientePadrao);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(1L, clientePadrao)).thenReturn(carrinhoPadrao);
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
            .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));

        when(pagamentoExternal.autorizarPagamento(eq(1L), anyDouble()))
            .thenReturn(new PagamentoDTO(false, null)); 

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            compraService.finalizarCompra(1L, 1L);
        });

        assertThat(exception.getMessage()).isEqualTo("Pagamento não autorizado.");
        verify(estoqueExternal, never()).darBaixa(anyList(), anyList());
    }

    // --------------------------------------------------------------------------
    // TESTES DE BORDA E MUTAÇÃO (VALORES EXATOS)
    // --------------------------------------------------------------------------

    @Test
    @DisplayName("Borda 5kg: Exatamente 5kg deve ser isento (Faixa A)")
    void calcularCustoTotal_Borda5kg_Isento() {
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("100.00"), new BigDecimal("5.0"), false, 1L)
        );

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.SUL, TipoCliente.BRONZE);

        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Borda 10kg: Exatamente 10kg deve pagar R$ 2,00/kg (Faixa B)")
    void calcularCustoTotal_Borda10kg_FaixaB() {
        // 10kg * 2.00 = 20.00 frete. Total 120.00.
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("100.00"), new BigDecimal("10.0"), false, 1L)
        );

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.NORDESTE, TipoCliente.OURO);

        assertThat(total).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("Borda 50kg: Exatamente 50kg deve pagar R$ 4,00/kg (Faixa C)")
    void calcularCustoTotal_Borda50kg_FaixaC() {
        // 50kg * 4.00 = 200.00 frete. Total 300.00.
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("100.00"), new BigDecimal("50.0"), false, 1L)
        );

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.NORTE, TipoCliente.PRATA);

        assertThat(total).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("Matar Mutante: Peso deve ser multiplicado pela quantidade")
    void calcularCustoTotal_QuantidadeMaiorQueUm() {
        // Peso 10kg, Qtd 2 = 20kg Total (Faixa C).
        // Se fosse divisão, daria 5kg (Faixa A).
        configurarItensNoCarrinho(
            criarItem(new BigDecimal("10.00"), new BigDecimal("10.0"), false, 2L)
        );

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.SUL, TipoCliente.BRONZE);

        // Subtotal: 20.00 + Frete (20 * 4.00 = 80.00) = 100.00
        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Deve retornar zero (escala 0) se a lista de itens for nula")
    void calcularCustoTotal_ListaItensNula() {
        carrinhoPadrao.setItens(null);

        BigDecimal total = compraService.calcularCustoTotal(carrinhoPadrao, Regiao.SUL, TipoCliente.OURO);

        assertThat(total).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve retornar zero (escala 0) se o objeto Carrinho for nulo")
    void calcularCustoTotal_CarrinhoNulo() {
        BigDecimal total = compraService.calcularCustoTotal(null, Regiao.SUL, TipoCliente.OURO);

        assertThat(total).isEqualTo(BigDecimal.ZERO);
    }
}