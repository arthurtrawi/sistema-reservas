# Documentação Técnica — Sistema de Reservas

Este documento descreve a arquitetura, a modelagem e as decisões técnicas do
sistema, servindo também como material de estudo dos conceitos de POO I.
O projeto foi desenvolvido com apoio de IA (Claude), de forma incremental e
com aprovação de cada etapa.

- **Parte I — Back-end** (seções 1 a 9): entidades, repositórios, serviços,
  regras de negócio. Histórico na seção 9.
- **Parte II — Front-end** (seções 10 a 18): interface gráfica Swing e sua
  integração com os serviços. Histórico na seção 17.

---

# Parte I — Back-end

---

## 1. Arquitetura

O sistema segue uma **arquitetura em camadas**. Cada camada tem uma única
responsabilidade e só conhece a camada imediatamente abaixo.

```
        Main  (monta o sistema e abre a interface — ver Parte II)
          │        ← um futuro Front-end se conecta aqui
          ▼
   ┌───────────── SERVICE ─────────────┐
   │ ClienteService   RecursoService   │  regras de negócio
   │            ReservaService         │
   └────────────────┬──────────────────┘
                    ▼
   ┌──────────── REPOSITORY ───────────┐
   │ ClienteRepo  RecursoRepo  ReservaRepo │  armazenamento (List)
   └────────────────┬──────────────────┘
                    ▼
                  MODEL                    entidades + consistência própria

   exception/ e util/ são usados por todas as camadas.
```

| Camada | Responsabilidade | Não faz |
|---|---|---|
| `model` | Representar entidades, proteger o próprio estado | Não conhece outras instâncias nem o armazenamento |
| `repository` | Guardar, buscar, atualizar e remover objetos | Não aplica regras de negócio |
| `service` | Regras de negócio, validações que dependem de vários objetos, coordenação | Não manipula coleções diretamente |
| `exception` | Comunicar erros de forma clara e tipada | — |
| `util` | Validações reutilizáveis | — |
| `Main` | Montar as dependências e iniciar a interface | Não contém regras de negócio |

**Por que em camadas?** Se amanhã os dados forem para um banco de dados, só os
repositórios mudam. Se surgir uma interface gráfica, ela chama os serviços já
existentes. As regras de negócio ficam isoladas e não precisam ser reescritas.

### Dependências entre os serviços

```
ReservaService ──► ClienteService ──► ClienteRepository
       │                 └──────────► ReservaRepository
       ├─────────► RecursoService ──► RecursoRepository
       │                 └──────────► ReservaRepository
       └─────────► ReservaRepository
```

- `ReservaService` usa os **serviços** de cliente e recurso (e não os
  repositórios deles) para que a regra "registro inexistente gera exceção"
  exista em um único lugar.
- `ClienteService` e `RecursoService` consultam o `ReservaRepository` apenas
  para saber se há reservas ativas antes de uma exclusão.
- As dependências são passadas **pelo construtor** (injeção de dependência
  manual) no `Main` (e em `teste.CenariosBackend.montarSistema()`).

---

## 2. Descrição das classes

### 2.1 `model`

**`Cliente`** — pessoa que realiza reservas.

| Atributo | Tipo | Regras |
|---|---|---|
| `cpf` | `String`, `final` | Identificador único; normalizado para 11 dígitos; sem setter |
| `nome` | `String` | Obrigatório |
| `email` | `String` | Obrigatório, formato `algo@algo.algo` |
| `telefone` | `String` | Obrigatório |

`equals`/`hashCode` comparam pelo CPF.

**`Recurso`** — item reservável (sala, equipamento...).

| Atributo | Tipo | Regras |
|---|---|---|
| `id` | `int`, `final` | Positivo; gerado pelo repositório |
| `nome` | `String` | Obrigatório |
| `tipo` | `String` | Obrigatório |
| `descricao` | `String` | Opcional (nulo vira `""`) |

`equals`/`hashCode` comparam pelo id.

**`Reserva`** — uso de um recurso por um cliente em um período.

| Atributo | Tipo | Regras |
|---|---|---|
| `id` | `int`, `final` | Gerado pelo repositório |
| `cliente` | `Cliente`, `final` | Obrigatório; não muda |
| `recurso` | `Recurso`, `final` | Obrigatório; não muda |
| `inicio`, `fim` | `LocalDateTime` | `fim` estritamente após `inicio` |
| `status` | `StatusReserva` | Nasce `ATIVA`; só muda por métodos de transição |

Métodos: `sobrepoe(inicio, fim)`, `alterarPeriodo(...)`, `cancelar()`,
`concluir()`, `isAtiva()`. Não existem `setStatus`, `setInicio` ou `setFim`.

**`StatusReserva`** (enum) — `ATIVA`, `CANCELADA`, `CONCLUIDA`. Cada constante
guarda uma descrição legível e se **bloqueia o recurso** (`bloqueiaRecurso()`),
que é `true` apenas para `ATIVA`.

### 2.2 `repository`

Os três repositórios guardam os objetos em um `ArrayList` privado e final.

| Classe | Métodos |
|---|---|
| `ClienteRepository` | `adicionar`, `buscarPorCpf`, `existePorCpf`, `listarTodos`, `atualizar`, `remover` |
| `RecursoRepository` | `gerarProximoId`, `adicionar`, `buscarPorId`, `existePorId`, `listarTodos`, `atualizar`, `remover` |
| `ReservaRepository` | `gerarProximoId`, `adicionar`, `buscarPorId`, `listarTodas`, `buscarPorCliente`, `buscarPorRecurso`, `buscarPorStatus`, `atualizar` |

- Buscas individuais retornam `Optional` (sem `null`).
- Listas retornadas são cópias **somente leitura** (`List.copyOf`).
- Ids são sequenciais e nunca reaproveitados.
- `ReservaRepository` não tem `remover`: reservas são canceladas, não apagadas.

### 2.3 `service`

**`ClienteService`** — `cadastrar`, `buscarPorCpf`, `listar`, `atualizar`, `excluir`.

**`RecursoService`** — `cadastrar`, `buscarPorId`, `listar`, `atualizar`, `excluir`.

**`ReservaService`**

| Método | Finalidade |
|---|---|
| `criar(cpf, idRecurso, inicio, fim)` | Cria reserva ATIVA após todas as validações |
| `buscarPorId(id)` / `listar()` | Consultas básicas |
| `alterarPeriodo(id, inicio, fim)` | Muda o período de uma reserva ativa, revalidando conflitos |
| `cancelar(id)` / `concluir(id)` | Transições de status |
| `listarPorCliente(cpf)` / `listarPorRecurso(id)` | Reservas (todos os status) de um cliente/recurso existente |
| `listarPorStatus(status)` | Filtro por status |
| `listarPorPeriodo(inicio, fim)` | Reservas que se sobrepõem ao intervalo |
| `verificarDisponibilidade(idRecurso, inicio, fim)` | `true` se não houver conflito |
| `buscarConflitos(idRecurso, inicio, fim)` | Lista as reservas ativas conflitantes |

### 2.4 `exception`

```
RuntimeException
└── SistemaReservasException                 (base de todos os erros de negócio)
    ├── ClienteNaoEncontradoException
    ├── RecursoNaoEncontradoException
    ├── ReservaNaoEncontradaException
    ├── CadastroDuplicadoException
    ├── OperacaoNaoPermitidaException
    └── ReservaInvalidaException
        └── ConflitoReservaException
```

Convenção:
- **Dado de campo inválido** (vazio, nulo, formato errado) → `IllegalArgumentException`
  (exceção nativa, idiomática para argumentos inválidos).
- **Violação de regra de negócio** → `SistemaReservasException` ou subclasse.

### 2.5 `util`

**`Validador`** — classe `final` com construtor privado e métodos estáticos:
`exigirTexto`, `exigirNaoNulo`, `validarCpf`, `validarEmail`, `validarPeriodo`.
Evita repetir as mesmas validações nas entidades e nos serviços.

**`FormatoDataHora`** — formato padrão `dd/MM/yyyy HH:mm`, usado nas
mensagens de erro com datas (adicionado após a construção do front-end;
ver seção 18).

---

## 3. Relacionamentos

```
Cliente 1 ─────────── 0..* Reserva 0..* ─────────── 1 Recurso
```

- Associação **unidirecional**: `Reserva` conhece `Cliente` e `Recurso`;
  o contrário não acontece.
- Para obter "as reservas de um cliente", consulta-se o `ReservaRepository`.

**Justificativa.** Uma associação bidirecional (por exemplo, `Cliente` com uma
`List<Reserva>`) exigiria manter duas estruturas sincronizadas a cada criação
ou cancelamento — uma fonte clássica de inconsistência. Com uma única fonte de
verdade, o modelo é mais simples e seguro.

As referências `cliente` e `recurso` em `Reserva` são `final`: uma reserva
nunca troca de dono nem de recurso. Para isso, cancela-se e cria-se outra.

---

## 4. Decisões de modelagem

| Decisão | Justificativa |
|---|---|
| CPF como identificador do cliente | Chave natural, já única no mundo real; normalizado para evitar duplicidade por formatação |
| Id sequencial para recurso e reserva | Não há chave natural; o repositório gera, como um auto-incremento |
| `LocalDateTime` para o período | Atende reservas por hora (salas, equipamentos) |
| Intervalo semiaberto `[início, fim)` | Reservas que "encostam" (10h–12h e 12h–14h) não conflitam |
| Sem `setStatus` / `setInicio` / `setFim` | O estado só muda por métodos que verificam a transição |
| Status `CONCLUIDA` além de `ATIVA` e `CANCELADA` | Distingue reserva utilizada de reserva desistida |
| Exceções *unchecked* com base comum | Código sem `throws` em cascata; o Front-end captura tudo com um `catch` |
| `ConflitoReservaException extends ReservaInvalidaException` | Um conflito **é uma** reserva inválida |
| Excluir cliente com reservas apenas canceladas/concluídas é permitido | A regra exige bloquear apenas com reservas **ativas**; o histórico continua consistente |
| Reservas não são apagadas | Preserva o histórico; usa-se cancelamento |
| Validar tudo antes de atualizar | Evita objetos parcialmente alterados quando um campo é inválido |
| Repositórios separados, sem classe base genérica | Clareza didática; uma `RepositorioEmMemoria<T>` é uma evolução possível |
| Validação de CPF só por formato | Dígitos verificadores não fazem parte dos requisitos |
| Reservas no passado não são bloqueadas | Não é requisito e dificultaria os testes com datas fixas |

---

## 5. Regras de negócio implementadas

| # | Regra | Onde |
|---|---|---|
| R1 | Campos obrigatórios não podem ser nulos ou vazios | Construtores/setters do `model` via `Validador` |
| R2 | CPF não pode se repetir | `ClienteService.cadastrar` |
| R3 | Operações sobre cliente/recurso/reserva inexistente lançam exceção | `buscarPor...` dos serviços |
| R4 | Não excluir cliente ou recurso com reserva ATIVA | `ClienteService.excluir`, `RecursoService.excluir` |
| R5 | Término estritamente posterior ao início | `Validador.validarPeriodo` (usado em `Reserva` e `ReservaService`) |
| R6 | Conflito = mesmo recurso + reserva ATIVA + períodos sobrepostos | `ReservaService.buscarConflitos` (privado) |
| R7 | Reservas canceladas/concluídas não bloqueiam o recurso | `StatusReserva.bloqueiaRecurso()` |
| R8 | Múltiplas reservas no mesmo recurso, se não houver conflito | Consequência de R6 |
| R9 | Disponibilidade verificada antes de criar ou alterar | `ReservaService.criar` / `alterarPeriodo` |
| R10 | Alteração de período ignora a própria reserva na checagem | Parâmetro `idIgnorado` |
| R11 | Só reservas ATIVAS podem ser alteradas, canceladas ou concluídas | `Reserva.exigirAtiva` e `ReservaService.alterarPeriodo` |

---

## 6. Conceitos de POO aplicados

| Conceito | Onde aparece |
|---|---|
| **Classes e objetos** | `Cliente`, `Recurso`, `Reserva` representam o domínio; o `Main` e os testes criam e manipulam instâncias |
| **Encapsulamento** | Todos os atributos `private`; setters validam; `Reserva` sem setters de estado; listas internas nunca expostas (`List.copyOf`) |
| **Construtores** | Garantem que nenhum objeto nasça inválido; `Reserva` nasce sempre `ATIVA` |
| **`final`** | Identificadores, associações da reserva, dependências dos serviços, classe `Validador` |
| **Associação** | `Reserva` → `Cliente` e `Reserva` → `Recurso` |
| **Herança** | Hierarquia de exceções a partir de `SistemaReservasException` |
| **Polimorfismo** | `catch (ReservaInvalidaException e)` também captura `ConflitoReservaException`; sobrescrita de `equals`, `hashCode` e `toString` |
| **Enumeração** | `StatusReserva`, com atributos e método próprios |
| **Collections** | `List`/`ArrayList` nos repositórios; `Optional` nas buscas |
| **Tratamento de exceções** | Exceções específicas para cada falha; `try/catch` nos cenários de teste e nas telas |
| **Abstração / interface** | Interface funcional `Acao` em `CenariosBackend`, usada com lambdas (ver também a Parte II) |
| **Separação de responsabilidades** | Camadas `model` / `repository` / `service` |

---

## 7. Métodos mais importantes

### `Reserva.sobrepoe(outroInicio, outroFim)`

```java
return inicio.isBefore(outroFim) && outroInicio.isBefore(fim);
```

Dois intervalos `[A_ini, A_fim)` e `[B_ini, B_fim)` se sobrepõem exatamente
quando `A_ini < B_fim` **e** `B_ini < A_fim`. A comparação estrita faz com que
intervalos adjacentes (12h–14h após 10h–12h) não sejam considerados conflito.
Cobre todos os casos: sobreposição parcial à esquerda/direita, um contido no
outro e períodos idênticos.

### `ReservaService.buscarConflitos(recurso, inicio, fim, idIgnorado)` (privado)

Núcleo da regra de disponibilidade. Percorre as reservas do recurso e retorna
as que (1) não são a reserva ignorada, (2) estão ativas e (3) se sobrepõem ao
período. É reutilizado por `criar`, `alterarPeriodo`, `verificarDisponibilidade`
e pela versão pública `buscarConflitos`, de modo que a regra existe em um só lugar.

### `ReservaService.criar(...)`

Ordem das validações: cliente existe → recurso existe → período válido →
sem conflito. Só então o id é gerado e a reserva é gravada. Em caso de
conflito, a exceção informa os ids das reservas que impedem a operação.

### `ReservaService.alterarPeriodo(...)`

Exige reserva ativa, valida o novo período e verifica conflitos **ignorando a
própria reserva** — sem isso, estender uma reserva de 14h–16h para 14h–17h
falharia por "conflitar" consigo mesma.

### `Reserva.cancelar()` / `concluir()`

Implementam as transições `ATIVA → CANCELADA` e `ATIVA → CONCLUIDA`. Ambas
chamam `exigirAtiva`, que impede, por exemplo, cancelar duas vezes ou cancelar
uma reserva já concluída.

### `ClienteService.excluir(cpf)` / `RecursoService.excluir(id)`

Verificam se existe alguma reserva ativa associada antes de remover,
garantindo integridade referencial.

---

## 8. Testes

Os cenários estão em `teste/CenariosBackend.java` (originalmente em `Main.java`;
movidos quando o `Main` passou a abrir a interface), sem bibliotecas externas.
Execução: `java -cp out teste.CenariosBackend`. Três métodos
auxiliares estruturam os testes:

- `esperarSucesso(descricao, acao)` — a ação não pode lançar exceção.
- `esperarErro(descricao, TipoDeExcecao.class, acao)` — a ação deve lançar
  exatamente aquele tipo (ou subclasse).
- `verificar(descricao, condicao)` — confere o estado do sistema.

Cobertura: os 12 cenários obrigatórios (numerados 01–12 na saída) e 38
verificações extras (validação de campos, períodos inválidos, transições de
status proibidas, consultas por status/período/conflito, alteração de período,
integridade nas exclusões e preservação do histórico).

**Resultado da última execução: 50 aprovados, 0 reprovados.**

---

## 9. Histórico de decisões e alterações

| Etapa | O que foi feito / decidido |
|---|---|
| 1 — Análise | Proposta de arquitetura, entidades e regras. Aprovadas: exceções extras com base *unchecked*; CPF como identificador; status `CONCLUIDA`; exclusão permitida com reservas apenas inativas; `LocalDateTime`; projeto em `sistema-reservas/` |
| 2 — Model | Entidades, enum, exceções e `Validador`. Escolhida a convenção `IllegalArgumentException` (campos) × `SistemaReservasException` (negócio). `ConflitoReservaException` passou a herdar de `ReservaInvalidaException` |
| 2 — Correção | A primeira compilação com `-Xlint:all` gerou 8 avisos de `serialVersionUID` nas exceções; foi adicionado `serialVersionUID = 1L` em cada uma |
| 3 — Repository | Repositórios com `ArrayList`, `Optional` e listas somente leitura. Decidido não criar remoção de reservas (histórico) e manter repositórios separados (sem classe genérica) |
| 4 — Service | Regras de negócio. Decisões: validar todos os campos antes de atualizar; `ReservaService` depende dos serviços (não dos repositórios) de cliente e recurso; `idIgnorado` na checagem de conflitos; reservas no passado não bloqueadas |
| 5 — Testes | `Main` com 50 cenários; todos aprovados na primeira execução. Removidos dois `import` não usados |
| 6 — Documentação | Criação deste documento e do `README.md` |
| Pós front-end (autorizado) | Datas das mensagens de erro formatadas como `dd/MM/yyyy HH:mm` (`util.FormatoDataHora`); `Main` passou a abrir a interface e os testes foram movidos para `teste.CenariosBackend` |

### Possíveis evoluções (não implementadas)

- Classe base genérica `RepositorioEmMemoria<T>` para eliminar a repetição entre repositórios.
- Validação dos dígitos verificadores do CPF.
- Impedir reservas com início no passado.
- Persistência em banco de dados (alterando apenas a camada `repository`).
- ~~Camada de Front-end consumindo os serviços existentes~~ — implementada (Parte II).

---

# Parte II — Front-end (Interface Gráfica Swing)

## 10. Arquitetura do Front-end

A interface fica no pacote `view` e é uma **nova camada sobre os serviços**.
Ela não acessa repositórios e não implementa regras de negócio.

```
   Usuário ──► View (Swing) ──► Service ──► Repository
                  ▲                │
                  └── resultado ou exceção (mensagem do back-end)
```

Fluxo de qualquer operação:

1. O usuário age na tela (clique, Enter, seleção).
2. O painel coleta os dados dos campos.
3. O painel chama o método do Service correspondente.
4. O Service aplica as regras e usa o Repository.
5. O painel recebe o resultado (ou captura `SistemaReservasException` /
   `IllegalArgumentException`) e mostra uma mensagem.
6. O painel recarrega tabela, combos e indicadores a partir do Service.

### Estrutura

```
Main ── monta repositórios e serviços ── SwingUtilities.invokeLater(...) ──► JanelaPrincipal
                                                                              │
 JanelaPrincipal (BorderLayout)                                               │
 ├── WEST   MenuLateral ─────── ActionListener ──► navegarPara(Modulo)  ◄─────┘
 └── CENTER ├── NORTH  PainelCabecalho
            ├── CENTER CardLayout ── INICIO   → InicioPainel
            │                     ├─ CLIENTES → ClientePainel ─┐
            │                     ├─ RECURSOS → RecursoPainel ─┴─ extends PainelCadastro
            │                     └─ RESERVAS → ReservaPainel
            └── SOUTH  PainelRodape
```

- **Injeção de dependência:** a `JanelaPrincipal` recebe os três serviços
  pelo construtor e os repassa aos painéis. Nenhum painel cria serviços.
- **Sincronização entre módulos:** todo painel implementa `Atualizavel`.
  A cada navegação, `JanelaPrincipal.navegarPara` chama `atualizar()` do
  painel exibido, que recarrega seus dados dos serviços. Assim, um cliente
  cadastrado em Clientes aparece no combo de Reservas, e uma reserva criada
  muda o indicador do Início — sem que os painéis conheçam uns aos outros.
- **Event Dispatch Thread (EDT):** a interface é criada em
  `SwingUtilities.invokeLater`. Como as operações são em memória e
  instantâneas, os serviços são chamados diretamente na EDT, sem travar a
  tela (por isso não foi necessário `SwingWorker`).
- **Encerramento:** `DO_NOTHING_ON_CLOSE` + `WindowListener`; o "X" e o
  botão Sair pedem confirmação e então chamam `dispose()` e `System.exit(0)`.

### Organização dos pacotes

| Pacote / classe | Responsabilidade |
|---|---|
| `view.JanelaPrincipal` | Janela, layout geral, registro dos painéis e navegação |
| `view.Modulo` | Enum dos módulos, com título e descrição do cabeçalho |
| `view.Atualizavel` | Interface `atualizar()` dos painéis que exibem dados |
| `view.PainelCadastro` | Classe abstrata com o fluxo comum de Clientes e Recursos |
| `view.componentes.*` | Componentes visuais reutilizáveis (ver seção 12) |
| `view.inicio.*` | Painel Início e carga de dados de exemplo |
| `view.cliente.ClientePainel` | Módulo de Clientes |
| `view.recurso.RecursoPainel` | Módulo de Recursos |
| `view.reserva.*` | Módulo de Reservas, renderizador de status e valor `Periodo` |
| `teste.CenariosInterface` | Testes automatizados da interface |

---

## 11. Telas

### 11.1 Início (`InicioPainel`)
- Três `CartaoIndicador`: **Clientes**, **Recursos** e **Reservas ativas**,
  com valores reais dos serviços, recalculados a cada abertura.
- **Acesso rápido**: Nova reserva, Gerenciar clientes, Gerenciar recursos.
  O painel recebe um `Consumer<Modulo>` (`this::navegarPara`) e "pede" a
  navegação sem conhecer a janela.
- **Dados de exemplo**: com o sistema vazio, cadastra 5 clientes, 6 recursos
  e 8 reservas (6 ativas, 1 cancelada, 1 concluída) **pelos serviços**, com
  datas relativas ao dia atual. Pede confirmação e fica inativo depois.

### 11.2 Clientes (`ClientePainel`) e Recursos (`RecursoPainel`)
- Formulário à esquerda (campos com `*` obrigatórios e dicas de formato) e
  tabela à direita com pesquisa e contador ("1 de 4 clientes").
- **Modo cadastro**: botão Cadastrar ativo.
- **Modo edição**: ao clicar numa linha, o registro é consultado no serviço
  (`buscarPorCpf` / `buscarPorId`) e o formulário é preenchido; o botão
  Atualizar fica ativo. Em Clientes, o CPF fica bloqueado (é o identificador).
- Excluir pede confirmação ("Não" é o padrão).
- Enter em qualquer campo executa a ação do modo atual.
- Recursos: o ID é gerado pelo back-end e exibido na tabela e na mensagem de
  sucesso; a ordenação por ID é numérica.

### 11.3 Reservas (`ReservaPainel`)
- **Nova reserva**: combos de cliente e recurso (carregados dos serviços),
  início e término com `CampoDataHora` (data + hora em `JSpinner`).
- **Verificar disponibilidade**: mostra "disponível" (verde) ou
  "Indisponível: conflito com a(s) reserva(s) [ids]" (vermelho). Qualquer
  alteração no formulário apaga o resultado, para não exibir informação velha.
- **Confirmar reserva**: chama `criar(...)`; conflitos e períodos inválidos
  são recusados pelo back-end e exibidos ("Conflito de horário", "Reserva inválida").
- **Tabela**: ID, Cliente, Recurso, Período e Status (selo colorido).
- **Filtros combináveis** por cliente, recurso e status.
- **Cancelar** e **Concluir**: com confirmação; ativos apenas para reservas
  com status Ativa.

---

## 12. Componentes reutilizáveis (`view.componentes`)

| Componente | Descrição |
|---|---|
| `Tema` | Cores, fontes e espaçamentos centralizados; configura o Look and Feel Metal |
| `ComponentesUI` | Fábrica de rótulos, campos, combos, cartões, parágrafos e linhas de botões; destaque de erro em campos |
| `Botao` | `JButton` arredondado nos estilos PRIMARIO, SECUNDARIO e PERIGO, com efeitos de mouse e foco |
| `MenuLateral` | Menu vertical genérico (itens com `ActionListener`, item ativo destacado) |
| `PainelCabecalho` / `PainelRodape` | Título, descrição e selo do módulo / nome do sistema e versão |
| `FormularioPainel` | Formulário em coluna com `GridBagLayout` (rótulo, campo, dica) |
| `PainelTabela` | `JTable` somente leitura com estilo, seleção única, ordenação, pesquisa e mensagem de lista vazia (`CardLayout`) |
| `RenderizadorCelula` | Desenho padrão das células: espaçamento, zebra, datas formatadas, dica com texto completo |
| `RenderizadorCombo<T>` | Texto exibido para cada objeto de um combo (genérico) |
| `CampoDataHora` | Data e hora com `JSpinner`, convertidas para `LocalDateTime` |
| `CartaoIndicador` | Cartão numérico do Início |
| `Formatos` | Exibição de CPF (`000.000.000-00`) e data/hora (`dd/MM/yyyy HH:mm`) |
| `Notificador` / `NotificadorDialogo` | Interface de mensagens e sua implementação com `JOptionPane` |

### Identidade visual

| Uso | Cor |
|---|---|
| Menu lateral | `#1E293B`, item ativo com barra `#2563EB` |
| Fundo / cartões / bordas | `#F1F5F9` / branco / `#E2E8F0` |
| Texto principal / secundário | `#0F172A` / `#64748B` |
| Ação principal / perigo | `#2563EB` / `#DC2626` |
| Status Ativa / Cancelada / Concluída | verde `#15803D` / vermelho `#B91C1C` / cinza `#475569` (sobre fundos claros) |

Tipografia `SansSerif` (disponível em qualquer sistema): títulos 22 negrito,
seções 15 negrito, textos 13. Sem emojis (o Java no Linux nem sempre os exibe).

---

## 13. Integração View → Service

A interface usa **apenas** métodos públicos já existentes no back-end, com
as assinaturas originais. Nenhuma alteração no back-end foi necessária para
a integração; a única mudança posterior (autorizada) foi o formato das datas
nas mensagens de erro (seção 18).

| Tela / ação | Método do back-end |
|---|---|
| Listar clientes; combos de cliente | `ClienteService.listar()` |
| Cadastrar / atualizar / excluir cliente | `ClienteService.cadastrar(cpf, nome, email, telefone)` / `atualizar(cpf, nome, email, telefone)` / `excluir(cpf)` |
| Selecionar cliente na tabela | `ClienteService.buscarPorCpf(cpf)` |
| Listar recursos; combos de recurso | `RecursoService.listar()` |
| Cadastrar / atualizar / excluir recurso | `RecursoService.cadastrar(nome, tipo, descricao)` / `atualizar(id, nome, tipo, descricao)` / `excluir(id)` |
| Selecionar recurso na tabela | `RecursoService.buscarPorId(id)` |
| Confirmar reserva | `ReservaService.criar(cpf, idRecurso, inicio, fim)` |
| Verificar disponibilidade | `ReservaService.verificarDisponibilidade(idRecurso, inicio, fim)` e `buscarConflitos(...)` |
| Listar reservas | `ReservaService.listar()` |
| Filtros | `listarPorCliente(cpf)`, `listarPorRecurso(id)`, `listarPorStatus(status)` |
| Cancelar / concluir | `ReservaService.cancelar(id)` / `concluir(id)` |
| Estado da reserva selecionada | `ReservaService.buscarPorId(id)` e `Reserva.isAtiva()` |
| Indicadores do Início | `listar().size()` dos três serviços e `listarPorStatus(ATIVA).size()` |
| Exibição de dados | getters das entidades (`getNome()`, `getCpf()`, `getInicio()`, `getStatus()`...) |

### Tratamento de erros

Cada operação captura apenas `SistemaReservasException` (regras de negócio)
e `IllegalArgumentException` (dados inválidos) e chama `Notificador.erro(e)`,
que escolhe um **título** conforme o tipo da exceção e exibe a **mensagem do
próprio back-end**:

| Exceção | Título exibido |
|---|---|
| `ConflitoReservaException` | Conflito de horário |
| `ClienteNaoEncontradoException`, `RecursoNaoEncontradoException`, `ReservaNaoEncontradaException` | Registro não encontrado |
| `CadastroDuplicadoException` | Cadastro duplicado |
| `OperacaoNaoPermitidaException` | Operação não permitida |
| `ReservaInvalidaException` | Reserva inválida |
| `IllegalArgumentException` | Dados inválidos |

Erros inesperados (bugs) não são capturados, para não serem escondidos atrás
de uma mensagem genérica.

### O que a interface valida (e o que não valida)

| A interface faz (usabilidade) | Continua no back-end (regras) |
|---|---|
| Destacar campos obrigatórios vazios antes de enviar | Formato de CPF e e-mail, CPF duplicado |
| Exigir seleção de cliente e recurso na reserva | Existência do cliente e do recurso |
| Interpretar a data/hora digitada (`dd/MM/yyyy`, `HH:mm`) | Término posterior ao início |
| Desativar Cancelar/Concluir para reservas não ativas (via `isAtiva()`) | Transições de status permitidas |
| — | Conflitos de horário e disponibilidade |
| — | Exclusão bloqueada com reservas ativas |

---

## 14. Decisões de design

| Decisão | Justificativa |
|---|---|
| Swing + Look and Feel Metal com tema próprio | Nativo do JDK, igual em todos os sistemas operacionais e aceita cores personalizadas; FlatLaf seria externo |
| Menu lateral + `CardLayout` | Navegação simples; cada módulo é um painel independente |
| Formulário à esquerda, tabela à direita | Cadastro e consulta visíveis ao mesmo tempo |
| `PainelCadastro` abstrato (Template Method) | Clientes e Recursos tinham o mesmo fluxo; evita duplicar ~200 linhas |
| Interface `Notificador` | Telas independentes de `JOptionPane`; permite testes automatizados sem diálogos |
| Interface `Atualizavel` | Sincroniza os módulos sem dependência entre painéis |
| Tabelas somente leitura | Toda alteração passa pelo formulário e pelo Service |
| Pesquisa como filtro visual (`TableRowSorter`) | O back-end só tem busca exata por CPF/ID; filtrar texto é apresentação, não regra |
| Filtros de reserva combinados por interseção | Cada filtro usa sua consulta do Service; a tela só cruza os resultados |
| Combos guardam objetos do model | A tela obtém o `Cliente`/`Recurso` escolhido diretamente; o texto vem do `RenderizadorCombo` |
| `JSpinner` para data/hora | Sem texto livre a interpretar; conversão direta para `LocalDateTime` |
| Coluna "Período" com objeto `Periodo` comparável | Seis colunas não cabiam; a ordenação continua cronológica |
| Botões "Verificar disponibilidade" e "Concluir" | Usam métodos já existentes no back-end; sem "Concluir" o status Concluída nunca apareceria |
| Confirmações com "Não" como padrão | Operações destrutivas exigem escolha explícita |
| Dados de exemplo apenas com sistema vazio | Uma segunda carga violaria a regra de CPF único |
| Componentes nomeados com `setName` | Os testes localizam componentes pelo nome, não pela posição |

---

## 15. Conceitos de POO aplicados no Front-end

| Conceito | Onde aparece |
|---|---|
| **Herança** | `JanelaPrincipal extends JFrame`; painéis `extends JPanel`; `Botao extends JButton`; `StatusReservaRenderer extends RenderizadorCelula extends DefaultTableCellRenderer` |
| **Classe abstrata / métodos abstratos** | `PainelCadastro` define o fluxo; `ClientePainel` e `RecursoPainel` implementam `executarCadastro`, `executarExclusao`, etc. |
| **Polimorfismo (sobrescrita)** | `paintComponent` em `Botao`, `MenuLateral.ItemMenu`, `StatusReservaRenderer`; `getTableCellRendererComponent`; `isCellEditable` / `getColumnClass` no modelo da tabela |
| **Polimorfismo (interfaces)** | `JanelaPrincipal` trata todos os painéis como `Atualizavel`; telas usam `Notificador` sem saber se é diálogo ou registro de teste |
| **Interfaces** | `Atualizavel`, `Notificador` (com métodos `default` e `static`), `Comparable<Periodo>` |
| **Encapsulamento** | Componentes `private final`; painéis expõem só `atualizar()`; `PainelTabela` esconde modelo, ordenador e cartões |
| **Composição** | A janela é composta de menu, cabeçalho, rodapé e painéis; `CampoDataHora` compõe dois `JSpinner` |
| **Classes aninhadas** | `ItemMenu`, `ModeloSomenteLeitura`, `RenderizadorCabecalho`, `ComboPlanoUI` |
| **Generics** | `RenderizadorCombo<T>`, `ComponentesUI.combo()`, `preencherCombo(JComboBox<T>, List<T>)` |
| **Enumerações** | `Modulo` (com atributos), `Botao.Estilo` (com cores) |
| **Objeto imutável** | `Periodo` (atributos `final`, `equals`/`hashCode`) |
| **Programação orientada a eventos** | `ActionListener` (botões, filtros, Enter), `ItemListener` (combos do formulário), `ListSelectionListener` (tabelas), `DocumentListener` (pesquisa e limpeza de erros), `ChangeListener` (spinners), `WindowListener` (fechamento) |
| **Injeção de dependência** | Serviços e `Notificador` passados por construtor |
| **Lambdas e referências a métodos** | `evento -> cadastrar()`, `this::navegarPara`, `Cliente::getNome` |

---

## 16. Testes do Front-end

`teste.CenariosInterface` abre a `JanelaPrincipal` real, com um
`Notificador` de teste que registra as mensagens em vez de abrir diálogos.
Os cenários preenchem campos, clicam (`doClick()`), selecionam linhas e
conferem o que a tela exibe e as mensagens emitidas. Requer ambiente gráfico.

**Resultado da última execução: 72 aprovados, 0 reprovados**
(e 50/50 em `teste.CenariosBackend`).

### Correspondência com os cenários exigidos

| # | Cenário exigido | Teste(s) correspondente(s) |
|---|---|---|
| 1 | Inicialização correta | "Aplicação inicia no módulo Início"; execução de `Main` sem erros |
| 2 | Navegação entre módulos | "Menu leva ao módulo ..." (4); atalhos do Início (3); Sair e "X" com confirmação |
| 3 | Listagem dos clientes | "Listagem mostra os 2 clientes cadastrados" |
| 4 | Cadastro de cliente | "Cadastro válido: mensagem de sucesso / cliente aparece com CPF formatado" |
| 5 | Atualização de cliente | "Selecionar linha preenche o formulário"; "Atualização: sucesso e tabela exibe o novo nome" |
| 6 | Exclusão de cliente | Recusa da confirmação; bloqueio com reserva ativa; "Exclusão confirmada" |
| 7 | Listagem dos recursos | "Ao abrir o módulo, a tabela é carregada do back-end" |
| 8 | Cadastro e atualização de recursos | "Cadastro válido ... ID gerado"; "Descrição é opcional"; "Atualização: ... novo tipo" |
| 9 | Clientes e recursos nos combos | "Combos carregam clientes e recursos do back-end" |
| 10 | Reserva válida | "Reserva válida: sucesso e nova linha na tabela" |
| 11 | Reserva com conflito | "Verificar disponibilidade (ocupado)"; "Confirmar com conflito: 'Conflito de horário'" |
| 12 | Cancelamento de reserva | Recusa da confirmação; "Cancelamento confirmado: status 'Cancelada'"; nova reserva no horário liberado |
| 13 | Atualização automática | Tabelas após cada operação; "combos incluem clientes cadastrados depois"; "indicador de ativas é atualizado (+1)" |
| 14 | Mensagens de erro | Cadastro duplicado, Dados inválidos, Operação não permitida, Conflito de horário, Reserva inválida, avisos de campos obrigatórios |
| 15 | Filtros de pesquisa | Pesquisa em Clientes e Recursos; filtros de Reservas por cliente, recurso, status e combinados |
| 16 | Tabelas sem registros | "Tabela vazia exibe mensagem"; "Pesquisa sem resultados"; "Filtro sem resultados exibe mensagem própria" |

Além dos testes automatizados, cada tela foi conferida visualmente por
capturas de tela durante o desenvolvimento (layout, cores, cortes de texto).

---

## 17. Histórico do Front-end

| Etapa | O que foi feito / decidido |
|---|---|
| 1 — Análise | Back-end analisado: nenhuma alteração necessária. Aprovados: `Main` passa a abrir a interface e os testes vão para `teste.CenariosBackend`; filtros combinados por interseção; pesquisa como filtro visual; botões "Verificar disponibilidade" e "Concluir"; Metal com tema próprio; botão "Carregar dados de exemplo" |
| 2 — Estrutura | `JanelaPrincipal`, `MenuLateral`, `PainelCabecalho`, `PainelRodape`, `Tema`, `Botao`, `ComponentesUI`, `Notificador`, `Atualizavel`, `Modulo` |
| 3 — Clientes | `ClientePainel`, `PainelTabela`, `FormularioPainel`, `RenderizadorCelula`, `Formatos`; início de `CenariosInterface` |
| 4 — Recursos | Criada a classe abstrata `PainelCadastro`; `ClientePainel` refatorado (testes sem alteração confirmaram o comportamento); `RecursoPainel` |
| 5 — Reservas | `ReservaPainel`, `CampoDataHora`, `RenderizadorCombo`, `StatusReservaRenderer`, `Periodo` |
| 6 — Início | `InicioPainel`, `CartaoIndicador`, `DadosExemplo` |
| 7 — Validação | Testes de Sair/"X"; correspondência com os 16 cenários; revisão (sem imports sem uso nem código provisório); documentação |
| 8 — Capturas | `teste.GeradorCapturas` gera 8 imagens em `docs/imagens/` (cenas montadas com os dados de exemplo) e o README passou a exibi-las |
| 9 — Ajuste no back-end (autorizado) | Datas das mensagens de erro no formato brasileiro via `util.FormatoDataHora`, reutilizado pela interface |

---

## 18. Problemas encontrados e soluções

| Problema | Causa | Solução |
|---|---|---|
| Item "Início" parecia selecionado sem estar ativo | O foco do teclado era desenhado igual ao destaque de mouse | Foco passou a ser só um contorno fino |
| `Botao` alterava a cor do texto durante o desenho | `setForeground` dentro de `paintComponent` (má prática) | Cor de texto desabilitado definida no `Tema` (`Button.disabledText`) |
| Avisos `serialVersionUID` no `-Xlint:all` | Componentes Swing são `Serializable` | `serialVersionUID = 1L` em cada subclasse |
| Clientes e Recursos quase idênticos | Duplicação de fluxo de tela | Classe abstrata `PainelCadastro` (Template Method) |
| Ordenação por ID colocava "10" antes de "2" | O modelo informava todas as colunas como `Object` | `getColumnClass` devolve o tipo real da coluna |
| Coluna ID com alinhamento e seleção diferentes | O `JTable` usa um renderizador próprio para `Number` | `RenderizadorCelula` registrado também para `Number` |
| Textos cortados nas colunas | Larguras inadequadas | Novas proporções e dica (tooltip) com o texto completo |
| `NullPointerException` ao abrir Reservas | `contains(null)` em lista imutável (`List.copyOf`) do back-end | Testar `null` antes de `contains` |
| Teste da interface travava em vez de falhar | Exceção escapava e a janela mantinha o processo vivo | `try/finally` com `System.exit` e contagem da falha |
| Datas e selos de status cortados na tabela de reservas | Seis colunas em ~530 px | Coluna única "Período" com objeto `Periodo` comparável |
| Filtros exibiam "Todos os clie..." | Texto longo para a largura | Texto "Todos" (o rótulo acima já identifica o filtro) |
| Setas dos combos com degradê do Metal | Chaves do `UIManager` não afetam esse botão | `ComboPlanoUI` (`BasicComboBoxUI` com seta plana) |
| HTML em `JLabel` para quebrar linhas | Contrariava a restrição de não usar HTML | `ComponentesUI.paragrafo` com `JTextArea` somente leitura |
| Descrição do indicador imprecisa | "não canceladas" incluiria concluídas | Texto "com status Ativa" |
| Datas como `2026-11-25T10:00` nas mensagens de erro | O back-end concatenava o `LocalDateTime` diretamente | `util.FormatoDataHora` (alteração no back-end autorizada) |

### Limitação corrigida: formato das datas nas mensagens do back-end

As mensagens de período inválido e de conflito de horário exibiam as datas
no formato técnico do `LocalDateTime` (ex.: `2026-11-25T10:00`). Com
autorização, o back-end foi ajustado: foi criado `util.FormatoDataHora`
(padrão `dd/MM/yyyy HH:mm`), usado em `Validador.validarPeriodo` e
`ReservaService.garantirSemConflito`. A interface (`Formatos.dataHora`)
passou a reutilizar o mesmo utilitário, eliminando a repetição do padrão.
Nenhuma regra de negócio mudou; os 122 testes continuam aprovados.
