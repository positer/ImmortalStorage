package com.cultivation.cultivation.client.guide;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JadeGuideBook {
    private static final String LOCK_KEY = "guide.cultivation.jade.lock.stage";
    private static final JadeGuideBook DEFAULT = createDefault();

    private final List<JadeGuideCategory> categories;
    private final List<JadeGuideChapter> chapters;
    private final Map<String, JadeGuideChapter> chaptersById;

    public JadeGuideBook(List<JadeGuideCategory> categories, List<JadeGuideChapter> chapters) {
        LinkedHashMap<String, JadeGuideCategory> categoryIndex = new LinkedHashMap<>();
        for (JadeGuideCategory category : categories) {
            if (categoryIndex.putIfAbsent(category.id(), category) != null) {
                throw new IllegalArgumentException("Duplicate guide category: " + category.id());
            }
        }
        LinkedHashMap<String, JadeGuideChapter> chapterIndex = new LinkedHashMap<>();
        for (JadeGuideChapter chapter : chapters) {
            if (!categoryIndex.containsKey(chapter.categoryId())) {
                throw new IllegalArgumentException("Unknown category for chapter " + chapter.id());
            }
            if (chapterIndex.putIfAbsent(chapter.id(), chapter) != null) {
                throw new IllegalArgumentException("Duplicate guide chapter: " + chapter.id());
            }
        }
        this.categories = List.copyOf(categoryIndex.values());
        this.chapters = chapterIndex.values().stream()
                .sorted(Comparator.comparingInt(JadeGuideChapter::order).thenComparing(JadeGuideChapter::id))
                .toList();
        this.chaptersById = Map.copyOf(chapterIndex);
    }

    public static JadeGuideBook defaultBook() {
        return DEFAULT;
    }

    public List<JadeGuideCategory> categories() {
        return categories;
    }

    public List<JadeGuideChapter> chapters() {
        return chapters;
    }

    public Map<String, JadeGuideChapter> chaptersById() {
        return chaptersById;
    }

    public List<JadeGuideChapter> chaptersIn(String categoryId) {
        return chapters.stream().filter(chapter -> chapter.categoryId().equals(categoryId)).toList();
    }

    private static JadeGuideBook createDefault() {
        List<JadeGuideCategory> categories = List.of(
                category("progression", "cultivation:jade_guide"),
                category("storage", "minecraft:chest"),
                category("resources", "cultivation:immortal_furnace"),
                category("tools", "cultivation:spirit_staff"),
                category("realm_compat", "minecraft:ender_eye"));

        List<JadeGuideChapter> chapters = new ArrayList<>();
        chapters.add(chapter("progression.overview", "progression", 10, 1, "cultivation:jade_guide"));
        chapters.add(chapter("progression.tasks", "progression", 15, 0, "cultivation:jade_guide"));
        chapters.add(chapter("progression.yuan", "progression", 20, 1, "cultivation:true_yuan"));
        chapters.add(chapter("progression.ascension", "progression", 30, 5, "cultivation:immortal_yuan"));
        chapters.add(chapter("progression.recipes", "progression", 40, 0, "minecraft:crafting_table"));
        chapters.add(chapter("storage.kongqiao", "storage", 110, 1, "minecraft:chest"));
        chapters.add(chapter("storage.terminal", "storage", 120, 6, "cultivation:xianqiao_manager"));
        chapters.add(chapter("storage.magnet", "storage", 130, 6, "minecraft:compass"));
        chapters.add(chapter("resources.furnace", "resources", 210, 5, "cultivation:immortal_furnace"));
        chapters.add(chapter("resources.sources", "resources", 220, 6, "cultivation:water_vein"));
        chapters.add(chapter("resources.infrastructure", "resources", 230, 6, "cultivation:source_vein_manager"));
        chapters.add(chapter("resources.catalog", "resources", 240, 0, "cultivation:spirit_core"));
        chapters.add(chapter("tools.staff", "tools", 310, 1, "cultivation:spirit_staff"));
        chapters.add(chapter("tools.sword", "tools", 320, 1, "cultivation:spirit_sword"));
        chapters.add(chapter("realm_compat.realm", "realm_compat", 410, 6, "minecraft:ender_eye"));
        chapters.add(chapter("realm_compat.tribulation", "realm_compat", 420, 6, "cultivation:white_day_thunder"));
        chapters.add(chapter("realm_compat.world", "realm_compat", 430, 6, "cultivation:world_shard_miner"));
        chapters.add(chapter("realm_compat.integration", "realm_compat", 440, 6, "cultivation:spirit_drive"));
        return new JadeGuideBook(categories, chapters);
    }

    private static JadeGuideCategory category(String id, String icon) {
        return new JadeGuideCategory(id, "guide.cultivation.jade.category." + id,
                ResourceLocation.parse(icon));
    }

    private static JadeGuideChapter chapter(String id, String category, int order, int stage, String icon) {
        String prefix = "guide.cultivation.jade.chapter." + id;
        return new JadeGuideChapter(id, category, order, stage,
                prefix + ".title", prefix + ".summary", prefix + ".body", prefix + ".keywords",
                LOCK_KEY, ResourceLocation.parse(icon));
    }
}
