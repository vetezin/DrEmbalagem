# Doutor Embalagem — apresentação integrada

Protótipo local em HTML, CSS e JavaScript, sem instalação de pacotes. Dados fictícios e cálculos demonstrativos; sem backend, autenticação ou publicação.

## Abrir

Na pasta do projeto, com Node.js no PATH:

```cmd
node servir-demo.cjs
```

- Dashboard: http://127.0.0.1:8081/prototipo-dashboard/index.html
- Mobile: http://127.0.0.1:8081/prototipo-mobile/index.html

Mantenha o terminal aberto. Ctrl+C encerra o servidor. Use sempre o mesmo endereço, porta, navegador e perfil para os dois protótipos: `localhost` e `127.0.0.1` são origens diferentes. Não abra os HTMLs diretamente por `file://`, pois o compartilhamento entre páginas depende da mesma origem HTTP.

O servidor atende somente em 127.0.0.1 e disponibiliza apenas os três diretórios públicos do protótipo. A planilha e os documentos de referência não são publicados.

## Roteiro para o cliente

1. No dashboard, clique em **Cadastrar empresa** e **Confirmar cadastro**. Todos os campos vêm preenchidos.
2. Abra o mobile, clique em **Adicionar nova consultoria**, selecione a empresa e confirme **Entrar e iniciar consultoria**.
3. Clique em **Adicionar ambiente**, escolha **Recepção**, depois **Cadastrar equipamento** e selecione um modelo. Ajuste a quantidade com + e −, se desejar.
4. Para demonstrar cadastros novos, o botão + da lista de ambientes abre o exemplo **Depósito**. **Cadastrar novo equipamento** abre um formulário preenchido.
5. Volte aos ambientes, clique em **Conferir e enviar** e **Enviar ao dashboard**. Cada ambiente precisa ter um equipamento.
6. No dashboard, abra **Consultoria** para conferir os ambientes, equipamentos e as quatro categorias de cálculo. Abra cada categoria para ver os produtos, quantidades, preços e contas.
7. Clique em **Ver resultados** para apresentar custos, economia anual e redução física de consumo.
8. Use **Reiniciar demonstração** no dashboard para repetir o roteiro. A confirmação remove somente os dados deste protótipo, inclusive nas outras abas abertas.

O cenário permite uma empresa e uma consultoria por rodada. Confirmar novamente uma visita não cria duplicatas. Depois do envio, o mobile exibe o resumo sem edição. A coleta persiste ao recarregar e as abas abertas atualizam quando o armazenamento compartilhado muda. Não há sincronização entre dispositivos ou navegadores.

## Cálculos e limites

`compartilhado/mock.js` é a fonte única do catálogo, cadastro de exemplo, parâmetros anuais e funções de cálculo. O estado da empresa e da coleta fica no `localStorage`, na chave `doutor-embalagem.demo.v1`.

Os parâmetros das abas **Sacos de Lixo**, **Sabonete Líquido**, **Papel Toalha** e **Papel Higiênico** da planilha são reproduzidos no cenário financeiro. Custo = quantidade anual × preço unitário; consumo físico = quantidade × conteúdo/peso; economia = atual − proposto. Não há conversão automática entre litros, folhas, metros e kg.

Referência antes do arredondamento: custo atual R$ 22.208,852; proposto R$ 12.184,295; economia R$ 10.024,557, exibida como **R$ 10.024,56**. Preços com três casas decimais e resultados intermediários são preservados. Por isso a soma das parcelas exibidas pode diferir em R$ 0,01 do total arredondado.

**Os equipamentos coletados não determinam automaticamente o consumo anual nem os preços.** Esses valores são um cenário preenchido, vinculado à consultoria fictícia para apresentação. As regras finais dependem de validação com o cliente. As dimensões de modelos do catálogo são exemplos; cadastros novos não recebem dimensões inventadas.

Não foram incluídos estoque, compras, login, exportação Excel ou alterações no arquivo da planilha.

## Verificação

Com Node.js:

```powershell
node --test tests/mock.test.cjs
```

Os testes cobrem compartilhamento do cadastro, vínculo com a empresa, restrição e idempotência do envio, quantidades persistidas, cálculos conferidos com a planilha, reset e falhas no armazenamento. O roteiro também foi percorrido no Playwright com duas abas, recarga e formulários preenchidos.

A pasta não é um repositório Git; nenhum commit ou push foi feito.
