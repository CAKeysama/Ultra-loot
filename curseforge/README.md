# Publicar no CurseForge

Tudo o que o CurseForge pede está nesta pasta. Criar o projeto e enviar o arquivo precisa da sua conta
(https://www.curseforge.com → **Create a Project**); isso não dá para fazer daqui.

## 1. Criar o projeto (uma vez só)

| Campo | Valor |
|---|---|
| Game / Class | Minecraft → **Mods** |
| Name | Ultra Loot |
| Summary | Tech machines that turn energy into loot: loot tables, mob drops on cards, no mobs spawned. |
| Description | colar `DESCRIPTION.md` (o editor aceita Markdown) |
| Avatar / logo | `logo.png` (400×400) |
| Main category | Technology (sugestão; subcategorias como Automation se aparecerem) |
| License | **All Rights Reserved** (é o que está no `gradle.properties` hoje; troque lá se escolher outra, ex. MIT) |
| Source / Issues | https://github.com/CAKeysama/Ultra-loot · https://github.com/CAKeysama/Ultra-loot/issues |

Projetos novos passam por uma revisão manual do CurseForge antes de aparecer para o público.

## 2. Enviar a versão 0.2.0

| Campo | Valor |
|---|---|
| Arquivo | `build/libs/ultraloot-0.2.0.jar` (o mesmo anexado na release v0.2.0 do GitHub) |
| Display name | Ultra Loot 0.2.0 |
| Release type | **Beta** |
| Game version | 1.21.1 |
| Mod loader | NeoForge |
| Java | Java 21 |
| Environment | Client **e** Server |
| Changelog | colar `CHANGELOG-0.2.0.md` |
| Dependencies | nenhuma obrigatória (precisa de algum mod que gere FE; não dá para marcar um específico) |

## 3. Galeria
Imagens em `screenshots/` (tiradas no jogo, 854×480). Sugestão de ordem: 01 máquinas, 02 gerador de mobs,
03 gerador de loot, 04 restaurador, 05 status, 06/07 livro-guia.

## Arquivos
- `logo.png` é gerado por `python tools/generate_logo.py` (também gera `src/main/resources/ultraloot_logo.png`,
  o ícone na lista de mods do jogo).
- O nome, versão, autor, licença, links e logo do mod dentro do JAR vêm de `gradle.properties` e
  `src/main/templates/META-INF/neoforge.mods.toml`.
