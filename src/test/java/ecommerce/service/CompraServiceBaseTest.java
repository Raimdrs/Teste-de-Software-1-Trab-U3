package ecommerce.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;

@ExtendWith(MockitoExtension.class)
public class CompraServiceBaseTest {

    // Dependências Mockadas (Padrão para a maioria dos testes)
    @Mock
    protected CarrinhoDeComprasService carrinhoService;

    @Mock
    protected ClienteService clienteService;

    @Mock
    protected IEstoqueExternal estoqueExternal;

    @Mock
    protected IPagamentoExternal pagamentoExternal;

    // O serviço a ser testado
    protected CompraService compraService;

    // Dados comuns
    protected Cliente clientePadrao;
    protected CarrinhoDeCompras carrinhoPadrao;

    @BeforeEach
    public void setup() {
        // 1. Configuração de Dados Comuns
        clientePadrao = new Cliente(1L, "Cliente Teste", Regiao.SUL, TipoCliente.OURO);

        carrinhoPadrao = new CarrinhoDeCompras();
        carrinhoPadrao.setId(1L);
        carrinhoPadrao.setCliente(clientePadrao);

        // 2. Inicialização Padrão do Serviço (com Mocks)
        // As subclasses que usam Fakes irão sobrescrever esta variável após chamar super.setup()
        compraService = new CompraService(carrinhoService, clienteService, estoqueExternal, pagamentoExternal);
    }

    // --------------------------------------------------------------------------
    // MÉTODOS AUXILIARES
    // --------------------------------------------------------------------------

    protected void configurarItensNoCarrinho(ItemCompra... itens) {
        carrinhoPadrao.setItens(List.of(itens));
    }

    protected ItemCompra criarItem(BigDecimal preco, BigDecimal peso, boolean fragil, long quantidade) {
        Produto produto = new Produto();
        produto.setPreco(preco);
        produto.setPesoFisico(peso);
        produto.setFragil(fragil);
        produto.setId(10L); // ID genérico para o produto

        ItemCompra item = new ItemCompra();
        item.setProduto(produto);
        item.setQuantidade(quantidade);
        return item;
    }
}