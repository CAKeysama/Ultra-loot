package io.github.cakeysama.ultraloot.guide;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cakeysama.ultraloot.guide.GuideBookData.Chapter;
import io.github.cakeysama.ultraloot.guide.GuideBookData.ItemSpec;
import io.github.cakeysama.ultraloot.guide.GuideBookData.Page;
import io.github.cakeysama.ultraloot.mob.CardWear;
import io.github.cakeysama.ultraloot.mob.MobCardItem;
import io.github.cakeysama.ultraloot.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shipped Ultra Loot Guide: every key translated in both languages, every item, mob and recipe real. */
class GuideContentTest {
    private static GuideBookData book;
    private static JsonObject english;
    private static JsonObject portuguese;

    @BeforeAll
    static void load() throws Exception {
        book = GuideBookData.parse(json("/assets/ultraloot/guide/guide.json"));
        english = json("/assets/ultraloot/lang/en_us.json");
        portuguese = json("/assets/ultraloot/lang/pt_br.json");
    }

    private static JsonObject json(String path) throws Exception {
        try (InputStream in = GuideContentTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void bookHasChaptersAndPages() {
        assertTrue(book.chapters().size() >= 10, "chapters: " + book.chapters().size());
        Set<String> ids = new HashSet<>();
        for (Chapter chapter : book.chapters()) {
            assertTrue(ids.add(chapter.id()), "duplicate chapter " + chapter.id());
            assertFalse(chapter.pages().isEmpty(), "empty chapter " + chapter.id());
        }
    }

    @Test
    void everyKeyIsTranslatedInBothLanguages() {
        for (Chapter chapter : book.chapters()) {
            translated(chapter.titleKey());
            for (Page page : chapter.pages()) {
                translated(page.textKey());
                if (page.titleKey() != null) translated(page.titleKey());
            }
        }
        for (String key : List.of("item.ultraloot.guide_book", "ultraloot.tooltip.guide_book", "ultraloot.guide.title",
                "ultraloot.guide.page", "ultraloot.guide.recipe_missing", "ultraloot.guide.received")) {
            translated(key);
        }
    }

    private static void translated(String key) {
        assertTrue(english.has(key) && !english.get(key).getAsString().isBlank(), "en_us missing " + key);
        assertTrue(portuguese.has(key) && !portuguese.get(key).getAsString().isBlank(), "pt_br missing " + key);
    }

    @Test
    void everyIconAndItemExists() {
        for (Chapter chapter : book.chapters()) {
            assertTrue(BuiltInRegistries.ITEM.containsKey(chapter.icon()), "unknown icon " + chapter.icon());
            for (Page page : chapter.pages()) {
                for (ItemSpec spec : page.items()) {
                    assertTrue(BuiltInRegistries.ITEM.containsKey(spec.item()), "unknown item " + spec.item());
                    if (spec.entity() != null) {
                        assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(spec.entity()), "unknown mob " + spec.entity());
                    }
                }
            }
        }
    }

    @Test
    void everyRecipeIsShipped() {
        for (Chapter chapter : book.chapters()) {
            for (Page page : chapter.pages()) {
                if (page.recipe() == null) continue;
                String path = "/data/" + page.recipe().getNamespace() + "/recipe/" + page.recipe().getPath() + ".json";
                assertNotNull(GuideContentTest.class.getResource(path), "recipe not shipped: " + page.recipe());
            }
        }
    }

    @Test
    void itemSpecsParse() {
        ItemSpec plain = ItemSpec.parse("ultraloot:guide_book");
        assertEquals("guide_book", plain.item().getPath());
        assertNull(plain.entity());
        assertEquals(0, plain.wear());

        ItemSpec worn = ItemSpec.parse("ultraloot:mob_card@minecraft:zombie#2800");
        assertEquals("mob_card", worn.item().getPath());
        assertEquals("zombie", worn.entity().getPath());
        assertEquals(2800, worn.wear());

        ItemStack card = worn.toStack();
        assertEquals(EntityType.ZOMBIE, MobCardItem.getEntityType(card).orElseThrow());
        assertEquals(2800, CardWear.wear(card));
        assertEquals(ModItems.GUIDE_BOOK.get(), plain.toStack().getItem());
    }

    @Test
    void unknownItemsFallBackToABarrier() {
        assertEquals("minecraft:barrier",
                BuiltInRegistries.ITEM.getKey(ItemSpec.parse("ultraloot:does_not_exist").toStack().getItem()).toString());
    }
}
