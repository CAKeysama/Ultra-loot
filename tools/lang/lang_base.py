# Base translation table for Ultra Loot: key -> (en_us, pt_br). Merged into the lang files by gen_lang.py.
ROMAN = ["I", "II", "III", "IV"]
L = {}

def put(key, en, pt):
    L[key] = (en, pt)

put("itemGroup.ultraloot", "Ultra Loot", "Ultra Loot")
for t in range(4):
    put(f"block.ultraloot.loot_generator_t{t+1}", f"Loot Generator (Tier {ROMAN[t]})", f"Gerador de Loot (Tier {ROMAN[t]})")
    put(f"block.ultraloot.mob_loot_generator_t{t+1}", f"Mob Loot Generator (Tier {ROMAN[t]})", f"Gerador de Loot de Mobs (Tier {ROMAN[t]})")

items = {
    "machine_frame": ("Machine Frame", "Estrutura de Máquina"),
    "loot_core": ("Loot Core", "Núcleo de Loot"),
    "soul_core": ("Soul Core", "Núcleo de Almas"),
    "logic_circuit": ("Logic Circuit", "Circuito Lógico"),
    "enhanced_circuit": ("Enhanced Circuit", "Circuito Aprimorado"),
    "refined_circuit": ("Refined Circuit", "Circuito Refinado"),
    "stellar_circuit": ("Stellar Circuit", "Circuito Estelar"),
    "blank_card": ("Blank Upgrade Card", "Cartão de Upgrade Vazio"),
    "blank_mob_card": ("Blank Mob Card", "Cartão de Mob Vazio"),
    "mob_card": ("Mob Card", "Cartão de Mob"),
    "experience_capsule": ("Experience Capsule", "Cápsula de Experiência"),
}
for k, (en, pt) in items.items():
    put(f"item.ultraloot.{k}", en, pt)
put("item.ultraloot.mob_card.named", "Mob Card (%s)", "Cartão de Mob (%s)")

cards = {
    "speed": ("Speed Card", "Cartão de Velocidade"),
    "quantity": ("Quantity Card", "Cartão de Quantidade"),
    "range": ("Range Card", "Cartão de Alcance"),
    "efficiency": ("Efficiency Card", "Cartão de Eficiência"),
    "loot": ("Loot Card", "Cartão de Loot"),
}
for k, (en, pt) in cards.items():
    for lvl in range(1, 4):
        put(f"item.ultraloot.{k}_card_{lvl}", f"{en} {ROMAN[lvl-1]}", f"{pt} {ROMAN[lvl-1]}")

put("ultraloot.upgrade.speed", "Speed", "Velocidade")
put("ultraloot.upgrade.quantity", "Quantity", "Quantidade")
put("ultraloot.upgrade.range", "Range", "Alcance")
put("ultraloot.upgrade.efficiency", "Efficiency", "Eficiência")
put("ultraloot.upgrade.loot", "Loot", "Loot")

put("ultraloot.card.speed.effect", "Operation speed: %s", "Velocidade de operação: %s")
put("ultraloot.card.quantity.effect", "Independent rolls: x%s", "Tentativas independentes: x%s")
put("ultraloot.card.range.effect", "Loot Generator: +%s blocks scan range", "Gerador de Loot: +%s blocos de alcance")
put("ultraloot.card.range.effect_mob", "Mob Loot Generator: collects XP orbs within %s blocks", "Gerador de Mobs: coleta orbes de XP em até %s blocos")
put("ultraloot.card.efficiency.effect", "Energy use: -%s%%", "Consumo de energia: -%s%%")
put("ultraloot.card.loot.effect", "Loot Generator: +%s luck", "Gerador de Loot: +%s de sorte")
put("ultraloot.card.loot.effect_mob", "Mob Loot Generator: Looting %s", "Gerador de Mobs: Pilhagem %s")
put("ultraloot.card.loot.filters", "Unlocks output filters", "Desbloqueia filtros de saída")
put("ultraloot.card.energy", "Energy per operation: %s", "Energia por operação: %s")
put("ultraloot.card.slot_rule", "One card per category; a higher level replaces a lower one", "Um cartão por categoria; nível maior substitui o menor")

modes = {
    "none": ("None", "Nenhum"),
    "energy_input": ("Energy Input", "Entrada de Energia"),
    "item_target": ("Item Target", "Alvo de Itens"),
    "xp_input": ("XP Input", "Entrada de XP"),
    "item_output": ("Item Output", "Saída de Itens"),
}
for k, (en, pt) in modes.items():
    put(f"ultraloot.side_mode.{k}", en, pt)

sides = {"front": ("Front", "Frente"), "back": ("Back", "Trás"), "left": ("Left", "Esquerda"),
         "right": ("Right", "Direita"), "top": ("Top", "Topo"), "bottom": ("Bottom", "Base")}
for k, (en, pt) in sides.items():
    put(f"ultraloot.side.{k}", en, pt)

status = {
    "running": ("Running", "Operando"),
    "no_energy": ("Not enough energy", "Energia insuficiente"),
    "no_profile": ("No loot profile selected", "Nenhum perfil de loot selecionado"),
    "invalid_profile": ("Loot profile unavailable", "Perfil de loot indisponível"),
    "profile_locked": ("Profile needs a higher tier", "Perfil exige tier maior"),
    "no_targets": ("No inventories in range", "Nenhum inventário no alcance"),
    "targets_full": ("Inventories are full", "Inventários cheios"),
    "no_mob_card": ("Insert a Mob Card", "Insira um Cartão de Mob"),
    "invalid_mob": ("This mob cannot be simulated", "Este mob não pode ser simulado"),
    "no_xp": ("Not enough experience", "Experiência insuficiente"),
    "output_full": ("Output is full", "Saída cheia"),
    "energy_buffer_too_small": ("Cost exceeds FE buffer: remove cards or upgrade tier",
                                "Custo excede o buffer de FE: remova cartões ou suba o tier"),
    "xp_tank_too_small": ("Cost exceeds XP tank: remove Quantity Card or upgrade tier",
                          "Custo excede o tanque de XP: remova o Cartão de Quantidade ou suba o tier"),
    "no_loot_source": ("No loot table for this mob (see log)", "Sem tabela de loot para este mob (veja o log)"),
    "no_picked_targets": ("No inventory picked: tick one in the Inventory Scan tab",
                          "Nenhum inventário escolhido: marque um na aba Varredura"),
}
for k, (en, pt) in status.items():
    put(f"ultraloot.status.{k}", en, pt)

filters = {"all": ("All items", "Todos os itens"), "resources": ("Resources", "Recursos"),
           "food": ("Food", "Comida"), "equipment": ("Equipment", "Equipamentos")}
for k, (en, pt) in filters.items():
    put(f"ultraloot.filter.{k}", en, pt)

scan = {
    "target": ("Target", "Alvo"),
    "full": ("Full", "Cheio"),
    "not_storage": ("Not a storage block (Storage only mode)", "Não é armazenamento (modo só armazenamento)"),
    "blacklisted": ("Never used by the generator", "Nunca usado pelo gerador"),
    "face_disabled": ("Touching face is not an Item Target", "Face em contato não é Alvo de Itens"),
    "not_picked": ("Not picked (Picked only mode)", "Não escolhido (modo só os escolhidos)"),
}
for k, (en, pt) in scan.items():
    put(f"ultraloot.scan_state.{k}", en, pt)

put("ultraloot.mob_card.empty", "No mob imprinted", "Nenhum mob gravado")
put("ultraloot.mob_card.xp_cost", "XP per kill: %s", "XP por abate: %s")
put("ultraloot.mob_card.energy", "FE cost: %s of base", "Custo de FE: %s do base")
put("ultraloot.mob_card.tier", "Mob tier %s", "Tier do mob %s")
put("ultraloot.mob_card.integrity", "Integrity: %s", "Integridade: %s")
put("ultraloot.mob_card.decaying", "Decaying: %s of kills drop Rotten Flesh",
    "Deteriorando: %s dos abates dão Carne Podre")
put("ultraloot.mob_card.corrupted", "Corrupted: only Rotten Flesh, double FE", "Corrompido: só Carne Podre, FE em dobro")
put("ultraloot.mob_card.repair_hint", "Repair it in a Card Restorer", "Conserte em um Restaurador de Cartões")
put("ultraloot.mob_card.unsupported", "This mob is not supported", "Este mob não é suportado")
put("ultraloot.mob_card.cannot_imprint", "This entity cannot be imprinted", "Esta entidade não pode ser gravada")
put("ultraloot.blank_mob_card.hint", "Use on a living mob to imprint it", "Use em um mob vivo para gravá-lo")
put("ultraloot.capsule.stored", "Stored XP: %s / %s", "XP armazenado: %s / %s")
put("ultraloot.capsule.hint", "Sneak + use: store your XP. Use: release it", "Agachar + usar: guarda seu XP. Usar: libera")

put("ultraloot.tooltip.tier", "Tier %s", "Tier %s")
put("ultraloot.tooltip.speed", "Speed: %s", "Velocidade: %s")
put("ultraloot.tooltip.energy_capacity", "Energy buffer: %s FE", "Buffer de energia: %s FE")
put("ultraloot.tooltip.attempts", "Rolls per operation: %s", "Tentativas por operação: %s")
put("ultraloot.tooltip.range", "Scan range: %s blocks", "Alcance de varredura: %s blocos")
put("ultraloot.tooltip.kills", "Simulated kills per operation: %s", "Abates simulados por operação: %s")
put("ultraloot.tooltip.xp_capacity", "XP tank: %s points", "Tanque de XP: %s pontos")
put("ultraloot.tooltip.loot_generator", "Fills nearby chests with loot table rolls", "Preenche baús próximos com loot de loot tables")
put("ultraloot.tooltip.mob_loot_generator", "Simulates mob drops using FE and XP", "Simula drops de mobs usando FE e XP")
