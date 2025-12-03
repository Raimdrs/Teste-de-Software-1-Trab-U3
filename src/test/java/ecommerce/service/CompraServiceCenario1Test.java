package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;

class CompraServiceCenario1Test extends CompraServiceBaseTest {

    // Dependências externas implementadas com Fakes locais
    private FakeEstoque estoqueFake;
    private FakePagamento pagamentoFake;

    @BeforeEach
    @Override
    public void setup() {
        // 1. Chama o setup da base para criar mocks e dados padrões
        super.setup(); 

        // 2. Inicializa os Fakes específicos deste cenário
        estoqueFake = new FakeEstoque();
        pagamentoFake = new FakePagamento();

        // 3. Sobrescreve o compraService injetando Mocks Internos (da Base) e Fakes Externos (Locais)
        // carrinhoService e clienteService são herdados da Base
        compraService = new CompraService(carrinhoService, clienteService, estoqueFake, pagamentoFake);
    }

    @Test
    @DisplayName("Cenário 1: Sucesso total usando Fakes externos e Mocks internos")
    void finalizarCompra_Sucesso() {
        // Arrange (Configurar Mocks internos herdados)
        when(clienteService.buscarPorId(1L)).thenReturn(clientePadrao);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(1L, clientePadrao)).thenReturn(carrinhoPadrao);

        // Arrange (Configurar Fake de Estoque)
        configurarItensNoCarrinho(criarItem(java.math.BigDecimal.TEN, java.math.BigDecimal.ONE, false, 1L));

        estoqueFake.adicionarDisponivel(10L); // Produto 10 está disponível
        pagamentoFake.setAutorizarSempre(true);

        // Act
        CompraDTO resultado = compraService.finalizarCompra(1L, 1L);

        // Assert
        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.mensagem()).contains("sucesso");
        // Verifica se o fake de estoque realmente registrou a baixa
        assertThat(estoqueFake.getItensBaixados()).contains(10L);
    }

    // -------------------------------------------------------------------------
    // Implementações FAKE (Classes Internas)
    // -------------------------------------------------------------------------

    static class FakeEstoque implements IEstoqueExternal {
        private Set<Long> idsDisponiveis = new HashSet<>();
        private Set<Long> itensBaixados = new HashSet<>();

        public void adicionarDisponivel(Long id) {
            idsDisponiveis.add(id);
        }

        public Set<Long> getItensBaixados() {
            return itensBaixados;
        }

        @Override
        public DisponibilidadeDTO verificarDisponibilidade(List<Long> produtosIds, List<Long> produtosQuantidades) {
            List<Long> indisponiveis = produtosIds.stream()
                    .filter(id -> !idsDisponiveis.contains(id))
                    .toList();
            
            return new DisponibilidadeDTO(indisponiveis.isEmpty(), indisponiveis);
        }

        @Override
        public EstoqueBaixaDTO darBaixa(List<Long> produtosIds, List<Long> produtosQuantidades) {
            itensBaixados.addAll(produtosIds);
            return new EstoqueBaixaDTO(true);
        }
    }

    static class FakePagamento implements IPagamentoExternal {
        private boolean autorizarSempre = true;

        public void setAutorizarSempre(boolean autorizar) {
            this.autorizarSempre = autorizar;
        }

        @Override
        public PagamentoDTO autorizarPagamento(Long clienteId, Double custoTotal) {
            if (autorizarSempre) {
                return new PagamentoDTO(true, 12345L);
            }
            return new PagamentoDTO(false, null);
        }

        @Override
        public void cancelarPagamento(Long clienteId, Long pagamentoTransacaoId) {
            // Lógica de cancelamento fake
        }
    }
}