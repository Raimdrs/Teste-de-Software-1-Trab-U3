package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

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
class CompraServiceCenario2Test {

    // Dependências internas implementadas com Fakes (Classes internas abaixo)
    private FakeCarrinhoService carrinhoServiceFake;
    private FakeClienteService clienteServiceFake;

    // Dependências externas mockadas com Mockito
    @Mock
    private IEstoqueExternal estoqueMock;

    @Mock
    private IPagamentoExternal pagamentoMock;

    private CompraService compraService;

    @BeforeEach
    void setup() {
        // Inicializa os Fakes internos
        carrinhoServiceFake = new FakeCarrinhoService(null); // Repositório nulo, pois é fake
        clienteServiceFake = new FakeClienteService(null);   // Repositório nulo, pois é fake

        // Injeta Fakes internos e Mocks externos
        compraService = new CompraService(carrinhoServiceFake, clienteServiceFake, estoqueMock, pagamentoMock);
    }

    @Test
    @DisplayName("Cenário 2: Sucesso total usando Mocks externos e Fakes de repositório")
    void finalizarCompra_Sucesso() {
        // Arrange (Configurar Fake interno de Banco de Dados)
        Cliente cliente = new Cliente(1L, "Maria Fake", Regiao.SUL, TipoCliente.PRATA);
        Produto produto = new Produto(10L, "Celular", "Samsung", new BigDecimal("1500.00"), 
                new BigDecimal("0.5"), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, false, null);
        ItemCompra item = new ItemCompra(1L, produto, 2L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras(1L, cliente, List.of(item), null);

        // Popula os "bancos de dados" dos fakes
        clienteServiceFake.setClienteFake(cliente);
        carrinhoServiceFake.setCarrinhoFake(carrinho);

        // Arrange (Configurar Mocks externos)
        when(estoqueMock.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));
        
        when(pagamentoMock.autorizarPagamento(eq(1L), anyDouble()))
                .thenReturn(new PagamentoDTO(true, 999L));
        
        when(estoqueMock.darBaixa(anyList(), anyList()))
                .thenReturn(new EstoqueBaixaDTO(true));

        // Act
        CompraDTO resultado = compraService.finalizarCompra(1L, 1L);

        // Assert
        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.mensagem()).contains("sucesso");
    }

    // -------------------------------------------------------------------------
    // Implementações FAKE (Classes Internas para o Cenário 2)
    // -------------------------------------------------------------------------

    // Fake estendendo a classe real para simular comportamento sem banco de dados
    static class FakeClienteService extends ClienteService {
        private Cliente clienteFake;

        public FakeClienteService(ecommerce.repository.ClienteRepository repo) {
            super(repo);
        }

        public void setClienteFake(Cliente cliente) {
            this.clienteFake = cliente;
        }

        @Override
        public Cliente buscarPorId(Long clienteId) {
            if (clienteFake != null && clienteFake.getId().equals(clienteId)) {
                return clienteFake;
            }
            throw new IllegalArgumentException("Cliente não encontrado (Fake)");
        }
    }

    // Fake estendendo a classe real para simular comportamento sem banco de dados
    static class FakeCarrinhoService extends CarrinhoDeComprasService {
        private CarrinhoDeCompras carrinhoFake;

        public FakeCarrinhoService(ecommerce.repository.CarrinhoDeComprasRepository repo) {
            super(repo);
        }

        public void setCarrinhoFake(CarrinhoDeCompras carrinho) {
            this.carrinhoFake = carrinho;
        }

        @Override
        public CarrinhoDeCompras buscarPorCarrinhoIdEClienteId(Long carrinhoId, Cliente cliente) {
            if (carrinhoFake != null && carrinhoFake.getId().equals(carrinhoId)) {
                return carrinhoFake;
            }
            throw new IllegalArgumentException("Carrinho não encontrado (Fake)");
        }
    }
}