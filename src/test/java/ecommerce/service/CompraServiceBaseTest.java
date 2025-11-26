package ecommerce.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @Mock
    protected CarrinhoDeComprasService carrinhoService;

    @Mock
    protected ClienteService clienteService;

    @Mock
    protected IEstoqueExternal estoqueExternal;

    @Mock
    protected IPagamentoExternal pagamentoExternal;

    @InjectMocks
    protected CompraService compraService;

    // Variáveis protegidas para serem acessadas pela classe de teste
    protected Cliente clientePadrao;
    protected CarrinhoDeCompras carrinhoPadrao;

    @BeforeEach
    void setup() {
        // Inicializa um Cliente válido antes de CADA teste
        clientePadrao = new Cliente(1L, "Cliente Teste", Regiao.SUL, TipoCliente.OURO);

        // Inicializa um Carrinho básico (sem itens) antes de CADA teste
        carrinhoPadrao = new CarrinhoDeCompras();
        carrinhoPadrao.setId(1L);
        carrinhoPadrao.setCliente(clientePadrao);
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