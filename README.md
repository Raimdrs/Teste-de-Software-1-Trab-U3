## Pré-requisitos

Para compilar o projeto e executar os testes, você precisará ter instalados em seu sistema:

* Java JDK (versão 17 ou superior)
* Apache Maven (versão 3.8 ou superior)

## Como Executar a Aplicação

Este projeto é uma aplicação Spring Boot. Para executá-la (iniciar o servidor):

1.  Abra um terminal (Prompt de Comando, PowerShell, Terminal, etc.).

2.  Navegue até o diretório raiz do projeto (a pasta que contém o arquivo pom.xml).

3.  Execute o seguinte comando:

    ```bash
    mvn spring-boot:run
    ```

4.  Este comando irá baixar as dependências, compilar o código e iniciar o servidor web. Por padrão, a aplicação estará disponível em `http://localhost:8080`

## 🧪 Como Rodar os Testes

Para executar todos os testes automatizados (unitários e de integração):

```bash
mvn test
```

📊 Relatórios de Cobertura (JaCoCo)
O projeto utiliza o JaCoCo para verificar a cobertura estrutural do código (Branch Coverage).

Gere o relatório executando:

Bash

./mvnw clean verify
Após a execução, abra o relatório no navegador:

Caminho: target/site/jacoco/index.html

O objetivo alcançado foi de 100% de cobertura de arestas (branches) no método calcularCustoTotal.

## Relatório de Mutação (PITEST)
O PITEST foi utilizado para garantir a qualidade dos testes, introduzindo defeitos (mutantes) no código para verificar se os testes são capazes de detectá-los.

1. Linha de comando usada
Para rodar os testes de mutação especificamente na classe de serviço:

```bash
mvn pitest:mutationCoverage
```
2. Como gerar e interpretar o relatório
Após a execução do comando acima, o relatório é gerado em:

Caminho: target/pit-reports/index.html (abra este arquivo no navegador).

## Interpretação:

Linhas Verdes: Mutantes mortos (Killed). O teste falhou quando o código foi alterado, o que é bom.

Linhas Vermelhas: Mutantes sobreviventes (Survived). O teste passou mesmo com o código alterado, o que é ruim.

Objetivo: Atingimos 100% de Mutantes Mortos na classe CompraService.

## Estratégias usadas para matar mutantes sobreviventes
Durante o desenvolvimento, diversos mutantes sobreviveram inicialmente. Abaixo detalhamos as estratégias aplicadas para eliminar cada tipo:

### A. Mutantes de Condição de Borda (Conditional Boundary)
O PITEST frequentemente altera condições como > para >=.

Estratégia: Criamos testes com valores exatos nos limites das faixas de peso e preço.

Exemplo: Testamos exatamente 5.0kg (Isento), 10.0kg (Faixa B) e 50.0kg (Faixa C). Se a lógica mudasse para >= 50, o cálculo de frete mudaria de R$ 4,00 para R$ 7,00, quebrando o teste.

### B. Mutantes de Verificação de Nulo (False Returns)
O PITEST substituiu a verificação if (carrinho.getItens() == null) por false.

Estratégia: Adicionamos um teste passando explicitamente uma lista null via carrinho.setItens(null) e outro passando o objeto carrinho como null.

Refinamento: Utilizamos assertThat(total).isEqualTo(BigDecimal.ZERO) em vez de comparadores flexíveis, para garantir que o retorno fosse exatamente ZERO (escala 0) e não 0.00 (escala 2), o que ocorria quando o código "pulava" a verificação de nulo e fazia o cálculo completo.

### C. Mutantes de Parâmetros em Mocks
O PITEST alterou os valores dentro de lambdas (ex: IDs dos produtos) para 0L ou null.

Estratégia: No teste finalizarCompra, deixamos de usar anyList() e passamos a usar verify(mock).metodo(eq(List.of(10L))). Isso garantiu que, se o código de produção passasse zeros ou nulos para o serviço externo, o teste falharia.

### D. Mutantes Equivalentes (Refatoração)
Havia um mutante sobrevivente na lógica de itens frágeis: if (quantidadeFrageis > 0). O PITEST mudava para >= 0.

Estratégia: Como multiplicar 0 por 5,00 resulta matematicamente em 0 (o mesmo que não somar nada), o mutante era equivalente. A solução foi remover o if do código fonte e deixar o cálculo ser executado sempre, eliminando a bifurcação desnecessária.

### E. Mutantes Matemáticos
O PITEST alterou multiplicação por divisão no cálculo do peso total.

Estratégia: Adicionamos um teste onde a quantidade do item fosse 2. Assim, peso * 2 é diferente de peso / 2, matando o mutante (antes usávamos quantidade 1, onde multiplicar e dividir dá o mesmo resultado).