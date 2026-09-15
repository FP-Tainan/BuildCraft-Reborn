# BuildCraft Reborn — mapa e sessões de trabalho

Port do **BuildCraft 8.0.0 (Minecraft 1.12.2, Forge)** para **Fabric no Minecraft 26.2**, usando o
**Craft Energy** (CW, MV, RA, CWh) no lugar do MJ. Base de referência extraída em
`C:\Users\Tainan\Downloads\BuildCraft BASE`.

Mesmo jeito do IC2 Reborn: cada sessão é implementada, testada no jogo e só depois vira commit.

---

## 1. O que tem no BuildCraft 8.0.0

| Módulo | Classes | Conteúdo principal |
|---|---|---|
| **lib** | 1.178 | Infraestrutura: base de tile, GUIs, fluidos, inventários, marcadores, portas lógicas, rede, modelos com expressões, lasers, livro guia |
| **api** | 355 | Interfaces: MJ, tubos, portas lógicas, receitas, esquemas |
| **core** | 113 | Chave inglesa, engrenagens (madeira → diamante), pincel (16 cores), lista (filtro), marcadores de área e de caminho, conector de marcadores, caixa de volume, fonte (água/petróleo), motor de redstone e criativo, blocos decorativos |
| **energy** | 68 | Motor Stirling e motor a combustão, 10 fluidos de petróleo × 3 temperaturas (30), combustíveis, refrigerantes, geração de petróleo no mundo (biomas, poços, lagos) |
| **factory** | 63 | Tanque, bomba, poço de mineração, tubo de mineração, comporta, bancada automática, destilador, trocador de calor (multibloco), gel de água, folha de plástico |
| **transport** | 174 | 46 tubos (estrutura, itens, fluidos, cinéticos, RF), fio de tubo (16 cores), tampão, adaptador de energia, buffer filtrado, selante |
| **silicon** | 99 | Laser, mesa de montagem, mesa de trabalho avançada, chipsets, portas lógicas (AND/OR, 4 materiais, 3 modificadores), lente/filtro, pulsar, sensor de luz, temporizador, fachadas, copiador de portas |
| **builders** | 176 | Pedreira (quarry) + armação, preenchedor (22 padrões), construtor, mesa do arquiteto, biblioteca eletrônica, substituidor, moldes e plantas, planejador de preenchimento |
| **robotics** | 28 | **Sem robôs** no 8.0. Só o planejador de zonas (sem receita) |
| **compat** | 61 | JEI, TheOneProbe, HWYLA, CraftTweaker, Forestry, IC2 |

Números gerais: ~40 blocos, ~80 itens, 30 fluidos, ~830 textos de tradução (um único `.lang` para todos
os módulos, 31 idiomas), 111 páginas do livro guia (só inglês).

**Fica de fora** (não existe ou não faz sentido com Craft Energy): motor RF, dínamo MJ, tubos RF,
mesas de carga, de programação e de integração (sem receitas), robôs, módulo Forestry.

## 2. Licença

O BuildCraft usa a **Minecraft Mod Public License (MMPL) 1.0.1**, bem mais aberta que a do IC2:
- pode modificar e redistribuir, inclusive texturas;
- **o código-fonte precisa ficar público e gratuito** (o GitHub resolve);
- arquivos modificados ou copiados continuam sob MMPL — o repositório do BuildCraft Reborn deve ter o
  `LICENSE` da MMPL;
- sem cláusula de atribuição, mas vamos creditar "SpaceToad e BuildCraft Team" com link para o original
  (o CurseForge pede isso, como aconteceu com o IC2).

## 3. Decisões de projeto (confirmadas em 14/09/2026)

1. **Um mod só** (`buildcraftreborn`), com os módulos do BuildCraft como pacotes, igual ao IC2 Reborn.
2. **1 MJ = 1 CWh** (e **1 MJ/t = 1.000 CW**). Como 1 CWh = 1.000 CW·tick, a conta fecha exata:
   - motor Stirling: 1.000 CW; motor a combustão: 1.000–8.000 CW conforme o combustível;
   - pedreira: bateria de 24.000 CWh, quebrar pedra (dureza 1,5 × 16) = 24 CWh;
   - chipset de redstone: 10.000 CWh na mesa de montagem.
3. **Tensões**: motores de redstone e Stirling em **220 MV**; motor a combustão, laser, pedreira,
   construtor e preenchedor em **1.000 MV**.
4. **Motores entregam CW contínuo** (não em pulsos) pela face da frente, só com sinal de redstone; o pistão
   continua animando e o estágio de calor (azul → verde → amarelo → vermelho → superaquecido) continua.
5. **Tubos cinéticos e RF saem**: quem leva energia são os cabos do Craft Energy. Os tubos de madeira,
   obsidiana e stripes recebem um pouco de CW (ou pulsar/motor de redstone).
6. **Unidades iguais ao IC2 Reborn**: fluidos em **CL**, calor em **CCº**.
7. **Modelos com expressões, lasers, travelling items**: tudo refeito com renderizadores próprios do 26.2
   (não dá para portar o sistema antigo).
8. **Receitas que não existem** (preenchedor, construtor, biblioteca, substituidor, planejador de zonas)
   serão inventadas no espírito do BuildCraft.
9. **Livro guia**: o mesmo esquema do IC2 Reborn (tela própria), convertendo as páginas do BuildCraft e
   traduzindo para PT-BR.
10. **Multímetro**: mover o multímetro do IC2 Reborn para o Craft Energy, para servir aos dois mods
    (a decidir).

---

## 4. Sessões de trabalho

Cada sessão termina com build verde, testes de jogo (gametests) e teste do usuário.

### Sessão 0 — Fundação do projeto
- Projeto Gradle/Fabric 26.2 (mesma base do IC2 Reborn), Craft Energy como pasta irmã.
- `fabric.mod.json`, ícone, créditos, `LICENSE` MMPL, README.
- Conversão das traduções `.lang` → JSON (`en_us`, `pt_br`), aba criativa, config simples.
- Base comum: bloco com orientação, block entity com sincronização, tela/menu base, slots fantasmas,
  widget de tanque, ledgers simples.
- Gametests rodando no `./gradlew build`.

### Sessão 1 — Core: ferramentas e marcadores ✅
> Feita. A caixa de volume e a localização no mapa ficaram para as sessões do preenchedor e do
> planejador de zonas, onde são usadas. A fonte de água também entrou nesta sessão.

- Chave inglesa, 5 engrenagens, pincel (16 cores + limpar), lista (filtro com GUI), blocos decorativos.
- Marcador de área (Land Mark) e de caminho (Path Mark), conector de marcadores, caixa de volume
  (dados salvos no mundo), localização no mapa, fragmento de fluido.
- **Renderizador de laser** (base para marcadores, pedreira e silício).

### Sessão 2 — Motores ✅
> Feita: redstone (50 CW), Stirling (1.000 CW, buffer de 1.000 CWh) e criativo (1 a 256 kCW, tensão
> escolhida). Saída só pela frente, com redstone; o motor criativo muda a tensão com clique direito
> e a potência com a chave agachado.

- Base de motor: frente giratória com chave, redstone liga, estágios de calor, superaquecimento,
  pistão animado, saída de CW no Craft Energy.
- Motor de redstone (baixíssima potência), motor Stirling (combustível sólido, GUI), motor criativo.

### Sessão 3 — Tanque, bomba e poço de mineração ✅
> Feita: tanque (16.000 CL, empilhável), bomba (10 CWh por balde), poço de mineração (dureza × 16 CWh
> por bloco), comporta e bancada automática (40 CWh por item, só com energia), tudo a 220 MV. A calha saiu: o funil do Minecraft faz o mesmo.

- Tanque (empilha na vertical, renderiza o fluido, comparador).
- Tubo de mineração, poço de mineração, bomba (busca de fluido até 64 blocos, água infinita).
- Comporta (flood gate), bancada automática.

### Sessão 4 — Pedreira (Quarry) ✅
> Feita: área pelos Marcadores de Área (ou 11×11 atrás), armação de 24 CWh por peça, mineração até a
> rocha-mãe com dureza × 16 CWh + 1 CWh por bloco, pórtico e broca com lasers, chunks carregados.
> Nominal 1.000 MV, mas aceita a partir de 200 MV enquanto não existe o motor a combustão.

- Armação, pedreira com fila de tarefas (armação → mover broca → quebrar), área pelos marcadores ou
  11×11 padrão, broca e pórtico renderizados, carregamento de chunk, entrega em inventário ou tubo.

### Sessão 5 — Tubos de itens ✅
> Feita: madeira (puxa com energia, 1 CWh por item), pedregulho, pedra, arenito, quartzo, ouro, ferro
> (saída pela chave), diamante (filtros), argila, vazio, obsidiana (suga itens) e estrutura. Itens
> andam desenhados; pedreira, poço, funis e baús entregam direto nos tubos.

- Bloco de tubo único com regras de conexão, modelo dinâmico (centro + braços + cor do vidro).
- Itens viajando (agendados, sincronizados em lote, renderizados).
- Tubos: estrutura, madeira, pedregulho, pedra, arenito, quartzo, ouro, ferro, diamante (GUI de filtro),
  argila, vazio, obsidiana. Selante.

### Sessão 6 — Tubos de fluidos
- Fluxo de fluido por seções e renderização; versões de fluido dos tubos (madeira, pedregulho, pedra,
  arenito, quartzo, ferro, ouro, diamante, argila, vazio).

### Sessão 7 — Petróleo e combustão
- 10 fluidos × 3 temperaturas, baldes, texturas geradas por cor, fluidos pegajosos e inflamáveis.
- Registro de combustíveis e refrigerantes; motor a combustão (3 tanques: combustível, refrigerante,
  resíduo); glóbulo de petróleo.

### Sessão 8 — Petróleo no mundo
- Biomas "Campo de Petróleo" (deserto e oceano), poços com jorro, esferas subterrâneas, lagos,
  fonte de petróleo e conquista "ouro negro".

### Sessão 9 — Refino
- Destilador (10 receitas por temperatura), trocador de calor multibloco (início, meio, fim),
  gel de água, folha de plástico.

### Sessão 10 — Silício: laser e mesas
- Laser alimentado por Craft Energy com feixe renderizado, mesa de montagem (chipsets, lentes, pulsar,
  fios), mesa de trabalho avançada.

### Sessão 11 — Portas lógicas e fios
- Sistema de gatilhos e ações (core, tubos, silício), porta lógica encaixável no tubo com GUI,
  materiais e modificadores, AND/OR, fio de tubo (rede salva no mundo), pulsar, sensor de luz,
  temporizador, copiador de portas.

### Sessão 12 — Preenchedor (Filler)
- 22 padrões (limpar, preencher, caixa, armação, pirâmide, escada, formas 2D, esferas),
  GUI de padrão com parâmetros, planejador de preenchimento na caixa de volume.

### Sessão 13 — Construção: arquiteto, construtor, biblioteca
- Moldes (templates) e plantas (blueprints), mesa do arquiteto, construtor (com fluidos e caminho),
  biblioteca eletrônica, substituidor, esquema de bloco único, regras de blocos refeitas para 26.2.

### Sessão 14 — Tubos especiais, stripes e fachadas
- Tubos lápis, daizuli, emzuli, madeira-diamante; buffer filtrado; lente/filtro e adaptador de energia.
- Tubo stripes (usa itens no mundo, estende o tubo).
- **Fachadas** (cobrir o tubo com o visual de qualquer bloco) — a parte mais difícil de renderização.

### Sessão 15 — Polimento e publicação
- Livro guia (111 páginas convertidas + PT-BR), conquistas, receitas no EMI/REI, informações no
  Jade/WTHIT, planejador de zonas (opcional), revisão de receitas, página do CurseForge.

---

## 5. Riscos principais

| Risco | Onde | Plano |
|---|---|---|
| Modelos com expressões e lasers | motores, tubos, pedreira, silício | renderizadores próprios (feito uma vez na Sessão 1/2) |
| Itens e fluidos viajando nos tubos | Sessões 5–6 | agendamento por tick de chegada + sync em lote |
| Fachadas com qualquer bloco | Sessão 14 | deixar por último |
| Biomas e estruturas de petróleo | Sessão 8 | dados de bioma + features do 26.2 |
| 30 fluidos com temperatura | Sessão 7 | texturas geradas por cor na build |
| Pré-visualização de planta (mundo falso) | Sessão 13 | versão simplificada |
| Regras de blocos com ids do 1.12 | Sessão 13 | reescrever as 32 regras |
