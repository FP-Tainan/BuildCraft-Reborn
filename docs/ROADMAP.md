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

### Sessão 6 — Tubos de fluidos ✅
> Feita: madeira (puxa com energia, 1 CWh a cada 1.000 CL), pedregulho, pedra, arenito, quartzo, ouro,
> ferro (saída pela chave), diamante (filtros com recipientes), argila, vazio. Vazão 40/80/160/320 CL/t,
> fluido desenhado dentro do tubo conforme a vazão, vedação (corante verde ou slime).

- Fluxo de fluido por seções e renderização; versões de fluido dos tubos (madeira, pedregulho, pedra,
  arenito, quartzo, ferro, ouro, diamante, argila, vazio).

### Sessão 7 — Petróleo e combustão ✅ (aguardando teste)
> Feita: 10 fluidos × 3 temperaturas com balde e textura gerada por cor, pegajosos e inflamáveis; registro de
> combustíveis/refrigerantes; motor a combustão (1.000 MV, tanques de combustível, refrigerante e resíduo, calor
> ideal 100 CCº, superaquece sem água, gelo vira água). Glóbulo de petróleo ficou de fora (era só ícone).

- 10 fluidos × 3 temperaturas, baldes, texturas geradas por cor, fluidos pegajosos e inflamáveis.
- Registro de combustíveis e refrigerantes; motor a combustão (3 tanques: combustível, refrigerante,
  resíduo); glóbulo de petróleo.

### Sessão 8 — Petróleo no mundo ✅ (aguardando teste)
> Feita: poços grandes e médios (esfera, jorro, tubo até a fonte de petróleo) e lagos com tentáculos; desertos,
> badlands e oceanos fazem o papel dos biomas de petróleo (×10, configurável), porque o 26.2 não tem API de
> biomas do overworld; conquistas "Ouro Negro" e "Mergulho Pegajoso".

- Biomas "Campo de Petróleo" (deserto e oceano), poços com jorro, esferas subterrâneas, lagos,
  fonte de petróleo e conquista "ouro negro".

### Sessão 9 — Refino ✅ (aguardando teste)
> Feita: destilador (10 receitas por temperatura, gás sai por cima e líquido por baixo, fluido desenhado nos
> tanques), trocador de calor multibloco (início, 1 a 3 meios, fim; 5/10/20 CL/t depois de 6 s esquentando),
> gelificador de água, gel que se espalha e água gelificada. Folha de plástico ficou de fora (sem receita no BC 8).

- Destilador (10 receitas por temperatura), trocador de calor multibloco (início, meio, fim),
  gel de água, folha de plástico.

### Sessão 10 — Silício: laser e mesas ✅ (aguardando teste)
> Feita: laser (1.000 MV, bateria de 1.024 CWh, até 4.000 CW, cone de 6 blocos, feixe com cor pela potência),
> mesa de montagem (receitas marcadas em rodízio, chipsets de redstone/ferro/ouro/quartzo/diamante com a energia
> do BuildCraft), mesa de trabalho avançada (molde 3×3, 500 CWh por item). Lentes, pulsar e portas lógicas ficam
> para as Sessões 11 e 14.

- Laser alimentado por Craft Energy com feixe renderizado, mesa de montagem (chipsets, lentes, pulsar,
  fios), mesa de trabalho avançada.

### Sessão 11 — Portas lógicas e fios ✅ (aguardando teste)
> Feita: porta lógica encaixável na face do tubo (tapa a ligação; agachado tira), 4 materiais × AND/OR × 4
> modificadores num item só, tela original com gatilhos/ações/parâmetros/grupos, saída de redstone pelo tubo.
> Gatilhos: sempre, redstone, inventário e tanque (vazio/contém/espaço/cheio/abaixo de 25-50-75%), estágio do
> motor, conteúdo do tubo, sinal no fio, luz, temporizador; ações: redstone, sinal no fio, pulsar constante/único.
> Fios de 16 cores nos cantos do tubo (rede calculada por tick, sem arquivo no mundo), pulsar (1 CWh por segundo
> em tubos de madeira/obsidiana, clique liga o manual), sensor de luz, temporizador e copiador de portas. Receitas
> na mesa de montagem (que agora mostra as receitas possíveis primeiro). Também: energia guardada alta/baixa no
> vizinho e a ação "desligar máquina" (pedreira, preenchedor, bomba, destilador, mesas...).

- Sistema de gatilhos e ações (core, tubos, silício), porta lógica encaixável no tubo com GUI,
  materiais e modificadores, AND/OR, fio de tubo (rede salva no mundo), pulsar, sensor de luz,
  temporizador, copiador de portas.

### Sessão 12 — Preenchedor (Filler) ✅ (aguardando teste)
> Feita: preenchedor com caixa dos marcadores, 19 padrões (nenhum, limpar, preencher, caixa, armação, pirâmide,
> escada, esfera e partes, 8 formas 2D) com parâmetros na tela original, escavar/inverter, 27 slots de material,
> bateria de 16.000 CWh. Receita do BuildCraft 7 (no 8 ele não tinha receita). Planejador de preenchimento e travar
> padrão por porta lógica ficam para depois.

- 22 padrões (limpar, preencher, caixa, armação, pirâmide, escada, formas 2D, esferas),
  GUI de padrão com parâmetros, planejador de preenchimento na caixa de volume.

### Sessão 13 — Construção: arquiteto, construtor, biblioteca ✅ (aguardando teste)
> Parte B: biblioteca eletrônica (pasta `buildcraftreborn/library` comum a todos os mundos; enviar/baixar em
> 2,5 s, lista com rolagem, apagar só no criativo), substituidor (troca um bloco por outro na planta com dois
> esquemas de bloco único, mantendo direção e outras propriedades), esquema de bloco único (clique num bloco
> guarda; agachado no ar limpa) e caminho de marcadores atrás do construtor (constrói em cada ponto da linha).
> Biblioteca, substituidor e esquema não tinham receita no BC 8 e ganharam receitas novas.
> Parte A: molde e planta (arquivo por hash dentro do mundo, o item só guarda nome/autor/hash; nome dado
> renomeando o item em branco na bigorna), mesa do arquiteto (caixa dos marcadores, 900/300 blocos por tick, sem
> energia), construtor (constrói atrás de si girado pela direção, quebra de cima para baixo e coloca de baixo para
> cima, 27 slots + 4 tanques de 8.000 CL para água/lava, lista do que falta, 16.000 CWh), regras de blocos refeitas
> em Java para os estados do 26.2. Receitas de molde, planta e arquiteto do BuildCraft; o construtor não tinha
> receita no BC 8, então ganhou uma nova. Parte B: biblioteca, substituidor, esquema de bloco único, caminho.

- Moldes (templates) e plantas (blueprints), mesa do arquiteto, construtor (com fluidos e caminho),
  biblioteca eletrônica, substituidor, esquema de bloco único, regras de blocos refeitas para 26.2.

### Sessão 14 — Tubos especiais, stripes e fachadas ✅ (aguardando teste)
> Parte A: itens pintados (caixinha colorida em volta do item), tubo lápis (pinta; chave troca a cor, agachado
> volta), daizuli (itens da cor só saem pela face especial; chave no centro troca a cor, num braço move a face),
> madeira-diamante (1 item por vez, lista branca/negra/rodízio, tela original), emzuli (4 presets com filtro e
> pintura, ligados por porta lógica), lentes e filtros coloridos (mesa de montagem, 500 CWh) e ações de porta
> "pintar itens" e "predefinição de extração".
> Parte B: tubo stripes (256 CWh; quebra o bloco da ponta e manda os drops de volta, usa itens com um jogador
> falso — coloca blocos, planta, ara —, tubo de itens na ponta estende a linha e tubo vazio recolhe), buffer
> filtrado (9 filtros + 9 slots, tela original) e tubo de fluidos de madeira-diamante (lista branca/negra de
> recipientes).
> Parte C: fachadas (qualquer bloco inteiro sem block entity, e vidros; a sólida tapa a ligação, a vazada deixa
> ligar; desenhadas com o próprio modelo do bloco). Receita especial na bancada: 3 tubos de estrutura + o bloco =
> 6 fachadas; uma fachada sozinha alterna vazada. Fachadas de fase (por cor de fio) ficaram de fora.

- Tubos lápis, daizuli, emzuli, madeira-diamante; buffer filtrado; lente/filtro e adaptador de energia.
- Tubo stripes (usa itens no mundo, estende o tubo).
- **Fachadas** (cobrir o tubo com o visual de qualquer bloco) — a parte mais difícil de renderização.

### Sessão 15 — Polimento e publicação 🔶 (parte feita, aguardando teste)
> Feito: livro guia (12 capítulos escritos para este port, em inglês e português; receita: livro +
> engrenagem de madeira; todo jogador ganha um ao entrar pela primeira vez), 26 conquistas com os
> nomes do BuildCraft (gatilho de inventário) e revisão de receitas (todo item tem receita de bancada, de mesa de
> montagem ou vem de fluido/criativo). Pendente, depende de decisão: EMI/REI e Jade/WTHIT (dependências novas no
> Gradle) e a página do CurseForge. O planejador de zonas era da robótica, que ficou fora do port.

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
