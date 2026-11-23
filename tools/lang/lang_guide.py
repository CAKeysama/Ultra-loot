# Ultra Loot Guide (in-game book): chapters, pages and their translations, in one place.
#
# gen_lang.py merges L into the lang files and calls write_extra(), which writes the book structure to
# assets/ultraloot/guide/guide.json. Page text keys: ultraloot.guide.<chapter>.<n> (+ ".title" for a heading).
#
# Markup inside page text: **bold**, [[key term]] (blue), \n = line break, \n\n = paragraph.
# Item specs (icons row): "<item id>", "<item id>@<entity id>" for a Mob Card, "...#<wear>" to show a worn card.
# Numbers must match config/Balance defaults and docs/guides; GuideContentTest checks keys, items and recipes.
import json
import os
import re

U = "ultraloot:"
GUIDE = "ultraloot:guide_book"


def fmt(text):
    text = re.sub(r"\*\*(.+?)\*\*", "§l\\1§r", text)
    return re.sub(r"\[\[(.+?)\]\]", "§1\\1§r", text)


def P(en, pt, title=None, recipe=None, items=None):
    return {"en": en, "pt": pt, "title": title, "recipe": recipe, "items": items or []}


CHAPTERS = [
    ("welcome", GUIDE, ("Welcome", "Bem-vindo"), [
        P("**Ultra Loot** turns energy (FE) into loot. Two machine families do the work:\n\n"
          "[[Loot Generator]]: rolls chest, fishing and archaeology loot tables straight into your chests.\n\n"
          "[[Mob Loot Generator]]: simulates mob kills from a Mob Card, paid with FE and experience. No mob is ever spawned.",
          "**Ultra Loot** transforma energia (FE) em loot. Duas famílias de máquinas fazem o trabalho:\n\n"
          "[[Gerador de Loot]]: sorteia tabelas de loot de baús, pesca e arqueologia direto nos seus baús.\n\n"
          "[[Gerador de Loot de Mobs]]: simula abates a partir de um Cartão de Mob, pagos com FE e experiência. Nenhum mob é gerado.",
          title=("What is Ultra Loot?", "O que é o Ultra Loot?"),
          items=[U + "loot_generator_t1", U + "mob_loot_generator_t1", U + "card_restorer"]),
        P("Both families come in **four tiers**. Higher tiers are faster, do more per operation, hold more energy and "
          "never cost more FE per result.\n\nEvery machine has a status line. Its colour and symbol always tell you what "
          "to do: cyan square = working, gray square = idle, amber ! = waiting for something, red x = needs your action.",
          "As duas famílias têm **quatro tiers**. Tiers maiores são mais rápidos, fazem mais por operação, guardam mais "
          "energia e nunca custam mais FE por resultado.\n\nToda máquina tem uma linha de status. A cor e o símbolo sempre "
          "dizem o que fazer: quadrado ciano = trabalhando, quadrado cinza = parada, ! âmbar = esperando algo, "
          "x vermelho = precisa da sua ação.",
          title=("Tiers and status", "Tiers e status")),
        P("Use the chapter list on the left, or:\n\n- arrow keys / mouse wheel: turn pages\n- click a chapter in the list "
          "to jump to it\n- hover an item to see its name and tooltip\n\nCraft another copy of this guide from a "
          "Book and a Redstone Dust.",
          "Use a lista de capítulos à esquerda, ou:\n\n- setas / roda do mouse: virar páginas\n- clique num capítulo da "
          "lista para pular\n- passe o mouse sobre um item para ver nome e descrição\n\nCrie outra cópia deste guia com "
          "um Livro e um Pó de Redstone.",
          title=("Using this guide", "Usando este guia"), recipe=U + "guide_book"),
    ]),
    ("components", U + "machine_frame", ("First steps", "Primeiros passos"), [
        P("Every machine starts from a **Machine Frame** and a **Logic Circuit**. Make these first; they only need iron, "
          "glass, copper and redstone.",
          "Toda máquina começa com uma **Estrutura de Máquina** e um **Circuito Lógico**. Faça-os primeiro; só precisam "
          "de ferro, vidro, cobre e redstone.",
          title=("Machine Frame", "Estrutura de Máquina"), recipe=U + "machine_frame"),
        P("Logic Circuits come in pairs. Better circuits gate the higher tiers: Enhanced (Tier II), Refined (Tier III) "
          "and Stellar (Tier IV).",
          "Circuitos Lógicos saem em pares. Circuitos melhores liberam os tiers maiores: Aprimorado (Tier II), Refinado "
          "(Tier III) e Estelar (Tier IV).",
          title=("Logic Circuit", "Circuito Lógico"), recipe=U + "logic_circuit",
          items=[U + "logic_circuit", U + "enhanced_circuit", U + "refined_circuit", U + "stellar_circuit"]),
        P("The **Loot Core** is the heart of a Loot Generator.",
          "O **Núcleo de Loot** é o coração do Gerador de Loot.",
          title=("Loot Core", "Núcleo de Loot"), recipe=U + "loot_core"),
        P("The **Soul Core** is the heart of a Mob Loot Generator.",
          "O **Núcleo de Almas** é o coração do Gerador de Loot de Mobs.",
          title=("Soul Core", "Núcleo de Almas"), recipe=U + "soul_core"),
        P("Machines need **Forge Energy (FE)**. Ultra Loot does not make power: use any generator from another tech mod "
          "and connect its cable to an **Energy Input** face (back and bottom by default).",
          "As máquinas precisam de **Forge Energy (FE)**. O Ultra Loot não gera energia: use o gerador de qualquer mod "
          "de tecnologia e ligue o cabo numa face de **Entrada de Energia** (trás e baixo por padrão).",
          title=("Power", "Energia")),
    ]),
    ("loot_generator", U + "loot_generator_t1", ("Loot Generator", "Gerador de Loot"), [
        P("Place it, put chests or barrels within range and give it FE. Each operation rolls the selected **loot profile** "
          "and puts the result straight into those inventories. It has no inventory of its own and never drops items.",
          "Coloque a máquina, ponha baús ou barris no alcance e forneça FE. Cada operação sorteia o **perfil de loot** "
          "escolhido e coloca o resultado direto nesses inventários. Ela não tem inventário próprio e nunca derruba itens.",
          title=("How it works", "Como funciona"), recipe=U + "loot_generator_t1"),
        P("Tier I to IV:\n- rolls per operation: 1 / 2 / 3 / 4\n- scan range: 5 / 8 / 12 / 16 blocks\n"
          "- FE per roll: 800 / 720 / 640 / 560\n- speed: 1x / 1.5x / 2x / 3x\n\nA base cycle takes 10 seconds at 1x.",
          "Tier I a IV:\n- sorteios por operação: 1 / 2 / 3 / 4\n- alcance: 5 / 8 / 12 / 16 blocos\n"
          "- FE por sorteio: 800 / 720 / 640 / 560\n- velocidade: 1x / 1,5x / 2x / 3x\n\nUm ciclo base leva 10 segundos em 1x.",
          title=("Tiers", "Tiers"),
          items=[U + "loot_generator_t1", U + "loot_generator_t2", U + "loot_generator_t3", U + "loot_generator_t4"]),
        P("Every 5 seconds the machine scans the loaded block entities in range. The **Inventory Scan** tab lists every "
          "inventory it found, its distance and free slots.\n\nAn inventory that **touches** the machine is only used "
          "if that face is set to **Item Target**, so loot never goes into a battery behind its energy face.",
          "A cada 5 segundos a máquina varre os blocos com inventário carregados no alcance. A aba **Varredura de "
          "Inventários** mostra cada inventário encontrado, a distância e os slots livres.\n\nUm inventário **encostado** "
          "na máquina só é usado se aquela face estiver como **Alvo de Itens**, então o loot nunca vai para uma bateria "
          "atrás da face de energia.",
          title=("Finding inventories", "Encontrando inventários")),
        P("By default loot goes to **every** inventory in range. To choose, open the **Inventory Scan** tab and switch "
          "the button from **All in range** to **Picked only**, then click the inventories you want: a tick marks "
          "them. Hover a row to see its position.\n\nNew chests placed later are not used until you pick them.",
          "Por padrão o loot vai para **todos** os inventários no alcance. Para escolher, abra a aba **Varredura de "
          "Inventários**, troque o botão de **Todos no alcance** para **Só os escolhidos** e clique nos inventários "
          "que quiser: eles ficam marcados. Passe o mouse numa linha para ver a posição.\n\nBaús colocados depois só "
          "são usados quando você os escolher.",
          title=("Choosing the chests", "Escolhendo os baús")),
        P("Before paying, the machine **simulates** the whole delivery. Items are spread round robin across targets. "
          "Only when everything fits is the loot inserted and the FE paid. If not, nothing is generated, nothing is "
          "consumed and the status says **Inventories are full**.\n\nNothing is ever lost or duplicated.",
          "Antes de pagar, a máquina **simula** a entrega inteira. Os itens são distribuídos em rodízio entre os alvos. "
          "Só quando tudo cabe o loot é inserido e o FE pago. Se não couber, nada é gerado, nada é consumido e o status "
          "mostra **Inventários cheios**.\n\nNada é perdido ou duplicado.",
          title=("Safe delivery", "Entrega segura")),
        P("Upgrade a machine by crafting it again with the previous tier in the centre: Tier II needs gold, diamonds and "
          "an Enhanced Circuit; Tier III emeralds, netherite and a Refined Circuit; Tier IV shulker shells, diamond "
          "blocks, a nether star and a Stellar Circuit.",
          "Melhore a máquina fabricando-a de novo com o tier anterior no centro: o Tier II pede ouro, diamantes e um "
          "Circuito Aprimorado; o Tier III esmeraldas, netherita e um Circuito Refinado; o Tier IV cascas de shulker, "
          "blocos de diamante, uma estrela do Nether e um Circuito Estelar.",
          title=("Upgrading the tier", "Subindo de tier"), recipe=U + "loot_generator_t2"),
    ]),
    ("profiles", "minecraft:chest", ("Loot profiles", "Perfis de loot"), [
        P("A **loot profile** is a loot table plus the context it needs. Choose one in the **Loot Configuration** tab. "
          "Ultra Loot ships 66 profiles: every vanilla chest, fishing, archaeology, piglin bartering and a few of its own.",
          "Um **perfil de loot** é uma tabela de loot mais o contexto de que ela precisa. Escolha um na aba "
          "**Configuração de Loot**. O Ultra Loot traz 66 perfis: todos os baús vanilla, pesca, arqueologia, escambo com "
          "piglins e alguns próprios.",
          title=("Choosing a profile", "Escolhendo um perfil"),
          items=["minecraft:chest", "minecraft:fishing_rod", "minecraft:brush", "minecraft:gold_ingot"]),
        P("Each profile has a **minimum tier** and an **energy multiplier**: valuable tables (End City, Ancient City...) "
          "need a better machine and cost more FE per roll. A locked profile shows **Profile needs a higher tier**.",
          "Cada perfil tem um **tier mínimo** e um **multiplicador de energia**: tabelas valiosas (Cidade do End, Cidade "
          "Ancestral...) pedem uma máquina melhor e custam mais FE por sorteio. Um perfil bloqueado mostra "
          "**Perfil exige tier maior**.",
          title=("Tiers and cost", "Tiers e custo")),
        P("Chest loot tables added by **other mods** are discovered automatically. They need a Tier II machine and cost "
          "1.67x energy. Tables that create exploration maps are skipped, because they would search the world for "
          "structures and lag the server.",
          "Tabelas de baús de **outros mods** são descobertas automaticamente. Elas pedem uma máquina Tier II e custam "
          "1,67x de energia. Tabelas que criam mapas de exploração são ignoradas, porque procurariam estruturas no mundo "
          "e travariam o servidor.",
          title=("Modded loot", "Loot de outros mods")),
        P("With any **Loot Card** installed, an output **filter** unlocks: All, Resources, Food or Equipment. Filtered "
          "items are destroyed before they exist and counted in the Statistics tab. Filters cannot make a table drop "
          "something it doesn't.",
          "Com qualquer **Cartão de Loot** instalado, um **filtro** de saída é liberado: Tudo, Recursos, Comida ou "
          "Equipamento. Itens filtrados são destruídos antes de existirem e contados na aba Estatísticas. Filtros não "
          "fazem uma tabela dropar algo que ela não dropa.",
          title=("Filters", "Filtros"), items=[U + "loot_card_1", U + "loot_card_2", U + "loot_card_3"]),
    ]),
    ("mob_generator", U + "mob_loot_generator_t1", ("Mob Loot Generator", "Gerador de Loot de Mobs"), [
        P("Insert a **Mob Card**, feed it FE and experience, and it simulates kills of that mob using the mob's own loot "
          "table. The drops go to a 27-slot output. No entity is ever spawned, so there is no lag and no mob farm.",
          "Insira um **Cartão de Mob**, forneça FE e experiência, e ela simula abates daquele mob usando a própria tabela "
          "de loot dele. Os drops vão para uma saída de 27 slots. Nenhuma entidade é gerada: sem lag e sem farm de mobs.",
          title=("How it works", "Como funciona"), recipe=U + "mob_loot_generator_t1"),
        P("Tier I to IV:\n- kills per operation: 1 / 1 / 2 / 2\n- FE per kill: 1,200 / 1,080 / 960 / 840\n"
          "- XP tank: 1,000 / 4,000 / 12,000 / 40,000 points\n- speed: 1x / 1.5x / 2x / 3x\n\nA base cycle takes 15 "
          "seconds at 1x. Each kill also costs the mob's XP price.",
          "Tier I a IV:\n- abates por operação: 1 / 1 / 2 / 2\n- FE por abate: 1.200 / 1.080 / 960 / 840\n"
          "- tanque de XP: 1.000 / 4.000 / 12.000 / 40.000 pontos\n- velocidade: 1x / 1,5x / 2x / 3x\n\nUm ciclo base "
          "leva 15 segundos em 1x. Cada abate também custa o preço em XP do mob.",
          title=("Tiers", "Tiers"),
          items=[U + "mob_loot_generator_t1", U + "mob_loot_generator_t2", U + "mob_loot_generator_t3",
                 U + "mob_loot_generator_t4"]),
        P("Every operation follows the same safe order: check the card, the XP and the FE, simulate the kills, check that "
          "all drops fit in the output, and only then commit everything. If anything fails, **nothing is consumed** and "
          "the card does not wear.",
          "Toda operação segue a mesma ordem segura: confere o cartão, o XP e o FE, simula os abates, confere se todos os "
          "drops cabem na saída e só então efetiva tudo. Se algo falhar, **nada é consumido** e o cartão não se desgasta.",
          title=("Nothing is wasted", "Nada é desperdiçado")),
        P("Take drops by hand in the **Inventory** tab, let an **Item Output** face push them into a chest every half "
          "second, or pull them with pipes. All of these work at the same time.",
          "Retire os drops à mão na aba **Inventário**, deixe uma face de **Saída de Itens** empurrá-los para um baú a "
          "cada meio segundo, ou puxe com tubos. Tudo isso funciona ao mesmo tempo.",
          title=("Getting the drops out", "Retirando os drops"), items=["minecraft:hopper", "minecraft:chest"]),
    ]),
    ("mob_cards", U + "mob_card", ("Mob Cards", "Cartões de Mob"), [
        P("Craft a **Blank Mob Card** and use it on a living mob. The mob is not hurt; the card now carries that mob. "
          "Bosses, blacklisted mobs and mobs whose loot cannot be found cannot be imprinted.",
          "Fabrique um **Cartão de Mob Vazio** e use-o num mob vivo. O mob não sofre dano; o cartão passa a carregar "
          "aquele mob. Chefes, mobs bloqueados e mobs cujo loot não pode ser encontrado não podem ser gravados.",
          title=("Imprinting a mob", "Gravando um mob"), recipe=U + "blank_mob_card"),
        P("Mobs have **reward tiers**. Tier I (chicken, zombie, skeleton...) is cheap. Tier II (cow, blaze, enderman...) "
          "costs more. Tier III (witch, evoker, shulker...) costs the most FE and XP per kill. The card tooltip shows "
          "the tier, XP per kill and FE cost.",
          "Os mobs têm **tiers de recompensa**. O Tier I (galinha, zumbi, esqueleto...) é barato. O Tier II (vaca, "
          "blaze, enderman...) custa mais. O Tier III (bruxa, evocador, shulker...) custa mais FE e XP por abate. A "
          "descrição do cartão mostra o tier, o XP por abate e o custo de FE.",
          title=("Mob tiers", "Tiers de mobs"),
          items=[U + "mob_card@minecraft:zombie", U + "mob_card@minecraft:blaze", U + "mob_card@minecraft:witch"]),
        P("Card data **wears out** with use: every simulated kill adds 1 wear. A card stays intact for 2,048 kills. Over "
          "the next 1,024 kills it decays: each kill has a growing chance to give **Rotten Flesh** instead of loot, and "
          "the FE cost rises up to double.",
          "Os dados do cartão **se desgastam** com o uso: cada abate simulado soma 1 de desgaste. O cartão fica intacto "
          "por 2.048 abates. Nos 1.024 abates seguintes ele se deteriora: cada abate tem chance crescente de dar "
          "**Carne Podre** em vez do loot, e o custo de FE sobe até o dobro.",
          title=("Card wear", "Desgaste do cartão"),
          items=[U + "mob_card@minecraft:zombie", U + "mob_card@minecraft:zombie#2400",
                 U + "mob_card@minecraft:zombie#2800", U + "mob_card@minecraft:zombie#3072"]),
        P("A fully **corrupted** card still runs, but only makes Rotten Flesh at double FE. It never breaks or "
          "disappears. Watch the durability bar (green, amber, red) or the **integrity** row in the Mob Configuration "
          "tab, and repair the card in a **Card Restorer**.",
          "Um cartão totalmente **corrompido** continua funcionando, mas só produz Carne Podre com o dobro de FE. Ele "
          "nunca quebra nem some. Acompanhe a barra de durabilidade (verde, âmbar, vermelha) ou a linha de "
          "**integridade** na aba Configuração do Mob, e conserte o cartão num **Restaurador de Cartões**.",
          title=("Corrupted cards", "Cartões corrompidos"), items=["minecraft:rotten_flesh", U + "card_restorer"]),
        P("Insert the card by hand in the **Mob Configuration** tab, or let automation insert it through an **XP "
          "Input** face while the card slot is empty. Automation can never take the card out. Swapping the card resets "
          "the progress bar.",
          "Insira o cartão à mão na aba **Configuração do Mob**, ou deixe a automação inseri-lo por uma face de "
          "**Entrada de XP** enquanto o slot do cartão estiver vazio. A automação nunca retira o cartão. Trocar o cartão "
          "zera a barra de progresso.",
          title=("Inserting a card", "Inserindo o cartão")),
    ]),
    ("restorer", U + "card_restorer", ("Card Restorer", "Restaurador de Cartões"), [
        P("The **Card Restorer** repairs worn Mob Cards with FE. Put a worn card in the input slot and power the block. "
          "When the card reaches 0 wear it moves to the output slot and works like new.",
          "O **Restaurador de Cartões** conserta Cartões de Mob desgastados com FE. Ponha um cartão desgastado no slot "
          "de entrada e energize o bloco. Quando o cartão chega a 0 de desgaste, ele vai para o slot de saída e funciona "
          "como novo.",
          title=("Repairing cards", "Consertando cartões"), recipe=U + "card_restorer"),
        P("It repairs up to 8 wear points per tick. Each point costs 40 FE times the mob's energy multiplier, and FE is "
          "only spent on points actually repaired. A fully corrupted zombie card costs 73,728 FE to restore.\n\n"
          "Buffer: 50,000 FE. Input: up to 1,000 FE per transfer.",
          "Ele conserta até 8 pontos de desgaste por tick. Cada ponto custa 40 FE vezes o multiplicador de energia do mob, "
          "e o FE só é gasto em pontos realmente consertados. Restaurar um cartão de zumbi totalmente corrompido custa "
          "73.728 FE.\n\nBuffer: 50.000 FE. Entrada: até 1.000 FE por transferência.",
          title=("Cost", "Custo")),
        P("Every face accepts FE. Hoppers and pipes can insert bound Mob Cards into the input and extract repaired cards "
          "from the output, so a restorer can sit in an automatic card loop.",
          "Todas as faces aceitam FE. Funis e tubos podem inserir Cartões de Mob gravados na entrada e extrair os "
          "cartões consertados da saída, então o restaurador pode fazer parte de um ciclo automático de cartões.",
          title=("Automation", "Automação"), items=["minecraft:hopper"]),
    ]),
    ("experience", U + "experience_capsule", ("Experience", "Experiência"), [
        P("The Mob Loot Generator stores experience as **points** in a tank. The tank can only be filled, never drained "
          "by automation, and the machine never takes XP from players on its own.",
          "O Gerador de Loot de Mobs guarda experiência como **pontos** num tanque. O tanque só pode ser enchido, nunca "
          "esvaziado por automação, e a máquina nunca tira XP de jogadores por conta própria.",
          title=("The XP tank", "O tanque de XP")),
        P("Ways to fill it:\n- **Bottles o' Enchanting** in the XP slot (7 points each)\n- an **Experience Capsule**\n"
          "- XP fluids from other mods (tag c:experience, 20 mB = 1 point) through an XP Input face\n"
          "- a **Range Card**, which absorbs XP orbs around the machine",
          "Formas de enchê-lo:\n- **Frascos de Experiência** no slot de XP (7 pontos cada)\n- uma **Cápsula de "
          "Experiência**\n- fluidos de XP de outros mods (tag c:experience, 20 mB = 1 ponto) por uma face de Entrada de "
          "XP\n- um **Cartão de Alcance**, que absorve orbes de XP em volta da máquina",
          title=("Filling the tank", "Enchendo o tanque"),
          items=["minecraft:experience_bottle", U + "experience_capsule", U + "range_card_1"]),
        P("Sneak and use the **Experience Capsule** to store your own XP in it, up to 30 levels (1,395 points). Put the "
          "full capsule in the machine's XP slot; the empty capsule comes back and can be pulled out by a hopper.",
          "Agache e use a **Cápsula de Experiência** para guardar o seu próprio XP nela, até 30 níveis (1.395 pontos). "
          "Coloque a cápsula cheia no slot de XP da máquina; a cápsula vazia volta e pode ser retirada por um funil.",
          title=("Experience Capsule", "Cápsula de Experiência"), recipe=U + "experience_capsule"),
    ]),
    ("upgrades", U + "speed_card_1", ("Upgrade cards", "Cartões de upgrade"), [
        P("Each machine has **five upgrade slots**, one per category: Speed, Quantity, Range, Efficiency and Loot. Each "
          "card has levels I to III. Shift-clicking a higher level swaps it in and gives back the old card.",
          "Cada máquina tem **cinco slots de upgrade**, um por categoria: Velocidade, Quantidade, Alcance, Eficiência e "
          "Loot. Cada cartão tem níveis I a III. Shift + clique num nível maior troca o cartão e devolve o antigo.",
          title=("Five slots", "Cinco slots"), recipe=U + "blank_card",
          items=[U + "speed_card_1", U + "quantity_card_1", U + "range_card_1", U + "efficiency_card_1",
                 U + "loot_card_1"]),
        P("**Speed**: 1.25x / 1.5x / 2x faster.\n**Quantity**: 2x / 3x / 4x rolls or kills, each one independent.\n"
          "**Range**: +8 / +16 / +24 scan blocks (Loot), or collects XP orbs within 2 / 4 / 6 blocks (Mob).\n"
          "**Efficiency**: -10% / -20% / -35% energy.\n**Loot**: +1 / +2 / +3 luck (Loot) or Looting I-III (Mob), and "
          "unlocks filters.",
          "**Velocidade**: 1,25x / 1,5x / 2x mais rápido.\n**Quantidade**: 2x / 3x / 4x sorteios ou abates, cada um "
          "independente.\n**Alcance**: +8 / +16 / +24 blocos de varredura (Loot) ou coleta orbes de XP a 2 / 4 / 6 blocos "
          "(Mobs).\n**Eficiência**: -10% / -20% / -35% de energia.\n**Loot**: +1 / +2 / +3 de sorte (Loot) ou Pilhagem "
          "I-III (Mobs), e libera filtros.",
          title=("What they do", "O que fazem")),
        P("Every card except Efficiency raises the energy cost. The final cost per operation is shown in the Overview "
          "tab. It is never below 25% of the tier's base cost, so operations are never free.\n\nLevels II and III are "
          "crafted from the previous level with better materials and an Enhanced or Refined Circuit.",
          "Todo cartão, exceto Eficiência, aumenta o custo de energia. O custo final por operação aparece na aba Visão "
          "Geral. Ele nunca fica abaixo de 25% do custo base do tier, então nenhuma operação é de graça.\n\nOs níveis II e "
          "III são feitos a partir do nível anterior com materiais melhores e um Circuito Aprimorado ou Refinado.",
          title=("Energy cost", "Custo de energia"), recipe=U + "speed_card_1"),
    ]),
    ("sides", "minecraft:hopper", ("Side configuration", "Configuração das faces"), [
        P("Each of the six faces has a role, set in the **Side Configuration** tab. Faces are relative to the machine's "
          "front, so the setup keeps working however you place it. Left-click a face for the next role, right-click "
          "for the previous one.",
          "Cada uma das seis faces tem uma função, definida na aba **Configuração das Faces**. As faces são relativas à "
          "frente da máquina, então a configuração continua valendo do jeito que você a colocar. Clique esquerdo numa "
          "face passa para a próxima função; clique direito volta.",
          title=("Faces", "Faces")),
        P("Roles:\n- **Energy Input**: accepts FE\n- **Item Target** (Loot): an inventory touching this face may receive "
          "loot\n- **XP Input** (Mob): XP, XP fluids, bottles, capsules and Mob Cards\n- **Item Output** (Mob): pushes "
          "drops out; pipes may extract\n- **None**: nothing connects",
          "Funções:\n- **Entrada de Energia**: aceita FE\n- **Alvo de Itens** (Loot): um inventário encostado nesta face "
          "pode receber loot\n- **Entrada de XP** (Mobs): XP, fluidos de XP, frascos, cápsulas e Cartões de Mob\n- "
          "**Saída de Itens** (Mobs): empurra os drops para fora; tubos podem extrair\n- **Nenhum**: nada se conecta",
          title=("Roles", "Funções")),
        P("Defaults. Loot Generator: back and bottom take energy, every other face is an Item Target. Mob Loot "
          "Generator: back takes energy, left and top take XP, bottom outputs items.\n\nUpgrade slots are never "
          "reachable by automation, and the output can only be extracted, never filled.",
          "Padrões. Gerador de Loot: trás e baixo recebem energia, as outras faces são Alvo de Itens. Gerador de Loot de "
          "Mobs: trás recebe energia, esquerda e cima recebem XP, baixo é saída de itens.\n\nOs slots de upgrade nunca "
          "ficam acessíveis à automação, e a saída só pode ser esvaziada, nunca abastecida.",
          title=("Defaults and safety", "Padrões e segurança")),
    ]),
    ("energy", "minecraft:redstone", ("Energy and tiers", "Energia e tiers"), [
        P("FE buffer by tier: 10,000 / 25,000 / 50,000 / 100,000. Max input per transfer: 250 / 625 / 1,250 / 2,500.\n\n"
          "An operation is paid in one piece. If a card stack costs more than the tier's buffer, the buffer **grows** "
          "to hold one operation, so you are never blocked by buffer size. Stored FE is never thrown away when it "
          "shrinks back.",
          "Buffer de FE por tier: 10.000 / 25.000 / 50.000 / 100.000. Entrada máxima por transferência: 250 / 625 / "
          "1.250 / 2.500.\n\nUma operação é paga de uma vez. Se uma combinação de cartões custar mais que o buffer do "
          "tier, o buffer **cresce** para caber uma operação, então o tamanho do buffer nunca trava a máquina. O FE "
          "guardado nunca é descartado quando ele volta a diminuir.",
          title=("FE buffer", "Buffer de FE")),
        P("Higher tiers are **never less efficient**: FE per roll or kill only goes down as the tier goes up, while "
          "speed and work per operation go up. One Tier IV machine always beats several Tier I machines on energy.",
          "Tiers maiores **nunca são menos eficientes**: o FE por sorteio ou abate só diminui conforme o tier sobe, "
          "enquanto a velocidade e o trabalho por operação aumentam. Uma máquina Tier IV sempre ganha de várias Tier I "
          "em energia.",
          title=("Efficiency by tier", "Eficiência por tier")),
        P("The XP tank does **not** grow. An expensive mob with a Quantity Card can need more XP per operation than a "
          "Tier I tank holds; the machine then shows **Cost exceeds XP tank** in red. Remove the Quantity Card or use a "
          "higher tier.",
          "O tanque de XP **não** cresce. Um mob caro com Cartão de Quantidade pode precisar de mais XP por operação do "
          "que cabe num tanque Tier I; a máquina então mostra **Custo excede o tanque de XP** em vermelho. Remova o Cartão "
          "de Quantidade ou use um tier maior.",
          title=("The XP limit", "O limite de XP")),
    ]),
    ("automation", "minecraft:comparator", ("Automation and other mods", "Automação e outros mods"), [
        P("Ultra Loot uses the standard NeoForge capabilities, so cables and pipes from other tech mods connect directly: "
          "FE cables to Energy Input faces, item pipes to Item Output faces, XP fluid pipes to XP Input faces.",
          "O Ultra Loot usa as capabilities padrão do NeoForge, então cabos e tubos de outros mods de tecnologia se "
          "conectam direto: cabos de FE nas faces de Entrada de Energia, tubos de itens nas faces de Saída de Itens, "
          "tubos de fluido de XP nas faces de Entrada de XP.",
          title=("Cables and pipes", "Cabos e tubos")),
        P("Machines work while their chunk is loaded and ticking. They never load chunks by themselves. When the chunk "
          "unloads they pause; progress, energy, XP, items and statistics are saved and resume later.",
          "As máquinas funcionam enquanto o chunk estiver carregado e ativo. Elas nunca carregam chunks sozinhas. Quando o "
          "chunk descarrega, elas pausam; progresso, energia, XP, itens e estatísticas são salvos e continuam depois.",
          title=("Chunks", "Chunks")),
        P("Breaking a machine drops its cards and items, and releases stored XP as orbs. Stored FE is lost, so empty "
          "the buffer first if it matters.",
          "Quebrar uma máquina derruba os cartões e itens, e libera o XP guardado como orbes. O FE guardado é perdido, "
          "então gaste o buffer antes se isso importar.",
          title=("Moving a machine", "Mudando a máquina de lugar")),
    ]),
    ("troubleshooting", "minecraft:barrier", ("Troubleshooting", "Solução de problemas"), [
        P("Amber (waiting):\n- **Not enough energy**: add FE or an Efficiency Card\n- **Not enough experience**: fill the "
          "XP tank\n- **Inventories are full** / **Output is full**: empty the chests or the output\n\nThe machine "
          "resumes by itself once the cause is gone.",
          "Âmbar (esperando):\n- **Energia insuficiente**: forneça FE ou use um Cartão de Eficiência\n- **Experiência "
          "insuficiente**: encha o tanque de XP\n- **Inventários cheios** / **Saída cheia**: esvazie os baús ou a saída\n\n"
          "A máquina volta sozinha quando a causa some.",
          title=("Amber statuses", "Status âmbar")),
        P("Red (your action):\n- **No inventories in range**: place a chest in range or set a face to Item Target\n"
          "- **No inventory picked**: tick one in the Inventory Scan tab\n"
          "- **Profile needs a higher tier**: upgrade the machine\n- **Cost exceeds XP tank**: remove the Quantity Card\n"
          "- **No loot table for this mob**: the tooltip says why (usually a datapack names a missing table)\n"
          "- **This mob cannot be simulated**: use another mob",
          "Vermelho (sua ação):\n- **Nenhum inventário no alcance**: ponha um baú no alcance ou defina uma face como Alvo "
          "de Itens\n- **Nenhum inventário escolhido**: marque um na aba Varredura\n- **Perfil exige tier maior**: suba o tier da máquina\n- **Custo excede o tanque de XP**: remova o "
          "Cartão de Quantidade\n- **Sem tabela de loot para este mob**: a descrição diz o motivo (normalmente um datapack "
          "aponta uma tabela que não existe)\n- **Este mob não pode ser simulado**: use outro mob",
          title=("Red statuses", "Status vermelhos")),
        P("When the status text is too long for the panel, hover it to read it in full. The **Statistics** tab counts "
          "operations, items made, items filtered, energy and XP used, which helps you compare setups.",
          "Quando o texto do status não cabe no painel, passe o mouse sobre ele para lê-lo inteiro. A aba "
          "**Estatísticas** conta operações, itens gerados, itens filtrados, energia e XP gastos, o que ajuda a "
          "comparar montagens.",
          title=("More help", "Mais ajuda")),
    ]),
]

UI = {
    "item.ultraloot.guide_book": ("Ultra Loot Guide", "Guia do Ultra Loot"),
    "ultraloot.tooltip.guide_book": ("Use to read how every machine works", "Use para ler como cada máquina funciona"),
    "ultraloot.guide.title": ("Ultra Loot Guide", "Guia do Ultra Loot"),
    "ultraloot.guide.chapters": ("Chapters", "Capítulos"),
    "ultraloot.guide.page": ("%s / %s", "%s / %s"),
    "ultraloot.guide.recipe_missing": ("Recipe not available on this server", "Receita indisponível neste servidor"),
    "ultraloot.guide.received": ("You received the Ultra Loot Guide. Use it to learn the mod.",
                                 "Você recebeu o Guia do Ultra Loot. Use-o para aprender o mod."),
}

L = dict(UI)
_book = {"chapters": []}
for cid, icon, (ten, tpt), pages in CHAPTERS:
    L[f"ultraloot.guide.chapter.{cid}"] = (ten, tpt)
    chapter = {"id": cid, "icon": icon, "title": f"ultraloot.guide.chapter.{cid}", "pages": []}
    for n, p in enumerate(pages, 1):
        key = f"ultraloot.guide.{cid}.{n}"
        L[key] = (fmt(p["en"]), fmt(p["pt"]))
        entry = {"text": key}
        if p["title"]:
            L[key + ".title"] = p["title"]
            entry["title"] = key + ".title"
        if p["recipe"]:
            entry["recipe"] = p["recipe"]
        if p["items"]:
            entry["items"] = p["items"]
        chapter["pages"].append(entry)
    _book["chapters"].append(chapter)


def write_extra(out, lang_dir):
    """Writes assets/ultraloot/guide/guide.json next to the lang folder (called by gen_lang.py)."""
    path = os.path.join(os.path.dirname(lang_dir), "guide", "guide.json")
    out.json(path, _book)
