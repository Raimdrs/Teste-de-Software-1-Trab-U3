package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;

@ExtendWith(MockitoExtension.class)
class CompraServiceCenario1Test {

    // Dependências internas mockadas com Mockito
    @Mock
    private CarrinhoDeComprasService carrinhoService;

    @Mock
    private ClienteService clienteService;

    // Dependências externas implementadas com Fakes (classes internas abaixo)
    private FakeEstoque estoqueFake;
    private FakePagamento pagamentoFake;

    private CompraService compraService;

    private Cliente clientePadrao;
    private CarrinhoDeCompras carrinhoPadrao;

    @BeforeEach
    void setup() {
        estoqueFake = new FakeEstoque();
        pagamentoFake = new FakePagamento();

        // Injeta os Mocks e os Fakes manualmente no construtor
        compraService = new CompraService(carrinhoService, clienteService, estoqueFake, pagamentoFake);

        // Configuração de dados comuns
        clientePadrao = new Cliente(1L, "João Fake", Regiao.SUDESTE, TipoCliente.OURO);
        
        Produto produto = new Produto(1L, "Notebook", "Dell", new BigDecimal("3000.00"), 
                new BigDecimal("2.0"), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, false, null);
        
        ItemCompra item = new ItemCompra(1L, produto, 1L);
        
        carrinhoPadrao = new CarrinhoDeCompras(1L, clientePadrao, List.of(item), null);
    }

    @Test
    @DisplayName("Cenário 1: Sucesso total usando Fakes externos e Mocks internos")
    void finalizarCompra_Sucesso() {
        // Arrange (Configurar Mocks)
        when(clienteService.buscarPorId(1L)).thenReturn(clientePadrao);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(1L, clientePadrao)).thenReturn(carrinhoPadrao);

        // Arrange (Configurar Fakes - estado inicial)
        estoqueFake.adicionarDisponivel(1L); // Produto 1 está disponível
        pagamentoFake.setAutorizarSempre(true);

        // Act
        CompraDTO resultado = compraService.finalizarCompra(1L, 1L);

        // Assert
        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.mensagem()).contains("sucesso");
        // Verifica se o fake de estoque realmente registrou a baixa
        assertThat(estoqueFake.getItensBaixados()).contains(1L);
    }

    // -------------------------------------------------------------------------
    // Implementações FAKE (Classes Internas para o Cenário 1)
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