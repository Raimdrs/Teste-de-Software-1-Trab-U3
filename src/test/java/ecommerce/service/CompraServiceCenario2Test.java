package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;

class CompraServiceCenario2Test extends CompraServiceBaseTest {

    // Dependências internas implementadas com Fakes
    private FakeCarrinhoService carrinhoServiceFake;
    private FakeClienteService clienteServiceFake;

    @BeforeEach
    @Override
    public void setup() {
        // 1. Setup da base (Mocks externos e dados)
        super.setup();

        // 2. Fakes internos
        carrinhoServiceFake = new FakeCarrinhoService(null);
        clienteServiceFake = new FakeClienteService(null);

        // 3. Sobrescreve o serviço com Fakes Internos e Mocks Externos (da Base)
        compraService = new CompraService(carrinhoServiceFake, clienteServiceFake, estoqueExternal, pagamentoExternal);
    }

    @Test
    @DisplayName("Cenário 2: Sucesso total usando Mocks externos e Fakes de repositório")
    void finalizarCompra_Sucesso() {
        // Arrange: Popula os "bancos de dados" dos fakes
        // clientePadrao e carrinhoPadrao vêm da Base
        clienteServiceFake.setClienteFake(clientePadrao);
        carrinhoServiceFake.setCarrinhoFake(carrinhoPadrao);

        // Configura o carrinho com um item (usando helper da base)
        configurarItensNoCarrinho(criarItem(java.math.BigDecimal.TEN, java.math.BigDecimal.ONE, false, 1L));

        // Arrange (Configurar Mocks externos herdados da Base)
        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, Collections.emptyList()));
        
        when(pagamentoExternal.autorizarPagamento(eq(1L), anyDouble()))
                .thenReturn(new PagamentoDTO(true, 999L));
        
        when(estoqueExternal.darBaixa(anyList(), anyList()))
                .thenReturn(new EstoqueBaixaDTO(true));

        // Act
        CompraDTO resultado = compraService.finalizarCompra(1L, 1L);

        // Assert
        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.mensagem()).contains("sucesso");
    }

    // -------------------------------------------------------------------------
    // Implementações FAKE
    // -------------------------------------------------------------------------

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