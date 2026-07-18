package com.cultivation.cultivation.client.screen;

import com.cultivation.cultivation.client.guide.JadeGuideBook;
import com.cultivation.cultivation.client.guide.JadeGuideCategory;
import com.cultivation.cultivation.client.guide.JadeGuideChapter;
import com.cultivation.cultivation.client.guide.JadeGuideHistory;
import com.cultivation.cultivation.client.guide.JadeGuideLocation;
import com.cultivation.cultivation.client.guide.JadeGuidePage;
import com.cultivation.cultivation.client.guide.JadeGuidePaginator;
import com.cultivation.cultivation.client.guide.JadeGuideProgression;
import com.cultivation.cultivation.client.guide.JadeGuideSearch;
import com.cultivation.cultivation.client.guide.JadeGuideSession;
import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Centered two-page progression guide for the Ancient Jade. */
public final class JadeGuideScreen extends Screen {
    static final int BOOK_WIDTH = 356;
    static final int BOOK_HEIGHT = 224;
    private static final int PAGE_WIDTH = 150;
    private static final int LEFT_PAGE_X = 22;
    private static final int RIGHT_PAGE_X = 184;
    private static final int CONTENT_Y = 48;
    private static final int CONTENT_LINES = 15;
    private static final int LINE_HEIGHT = 9;
    private static final int BUTTON_HEIGHT = 14;

    private final JadeGuideBook book = JadeGuideBook.defaultBook();
    private final JadeGuideSession session = JadeGuideSession.INSTANCE;
    private final List<Button> dynamicButtons = new ArrayList<>();

    private EditBox searchBox;
    private Button historyBack;
    private Button historyForward;
    private PageButton pageBack;
    private PageButton pageForward;
    private Mode mode = Mode.HOME;
    private String selectedCategory;
    private JadeGuideChapter selectedChapter;
    private List<JadeGuidePage> chapterPages = List.of();
    private List<JadeGuideSearch.Result> searchResults = List.of();
    private int currentPage;
    private int stage;
    private boolean stageTenInfiniteImmortalYuan;
    private boolean applyingHistory;
    private boolean suppressSearchResponder;
    private final List<GuideStackHit> guideStackHits = new ArrayList<>();

    public JadeGuideScreen() {
        super(Component.translatable("guide.cultivation.jade.title"));
    }

    @Override
    protected void init() {
        clearWidgets();
        dynamicButtons.clear();
        CultivationPlayerData playerData = minecraft != null && minecraft.player != null
                ? CultivationPlayerData.get(minecraft.player) : null;
        stage = playerData != null ? playerData.getStage() : 0;
        stageTenInfiniteImmortalYuan = playerData != null
                && playerData.isStageTenInfiniteImmortalYuanConfigured();
        int left = bookLeft();
        int top = bookTop();

        searchBox = new EditBox(font, left + 105, top + 11, 146, 12,
                Component.translatable("guide.cultivation.jade.search"));
        searchBox.setHint(Component.translatable("guide.cultivation.jade.search_hint"));
        searchBox.setMaxLength(80);
        searchBox.setResponder(this::onSearchChanged);
        setSearchValue(session.searchQuery());
        addRenderableWidget(searchBox);

        historyBack = addRenderableWidget(Button.builder(Component.literal("<"), ignored -> navigateHistory(false))
                .bounds(left + 68, top + 11, 16, 12)
                .tooltip(Tooltip.create(Component.translatable("guide.cultivation.jade.history_back")))
                .build());
        historyForward = addRenderableWidget(Button.builder(Component.literal(">"), ignored -> navigateHistory(true))
                .bounds(left + 86, top + 11, 16, 12)
                .tooltip(Tooltip.create(Component.translatable("guide.cultivation.jade.history_forward")))
                .build());
        addRenderableWidget(Button.builder(Component.literal("H"), ignored -> showHome(true))
                .bounds(left + 254, top + 11, 16, 12)
                .tooltip(Tooltip.create(Component.translatable("guide.cultivation.jade.home")))
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, ignored -> onClose())
                .bounds(left + 273, top + 11, 34, 12).build());

        pageBack = addRenderableWidget(new PageButton(left + 16, top + 190, false, ignored -> previousPage(), true));
        pageForward = addRenderableWidget(new PageButton(left + 315, top + 190, true, ignored -> nextPage(), true));

        JadeGuideLocation location = session.history().current().orElse(null);
        if (location != null) {
            restoreLocation(location);
        } else if (!searchBox.getValue().isBlank()) {
            showSearch(searchBox.getValue(), 0, true);
        } else {
            showHome(false, true);
        }
        updateControlState();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        renderBookChrome(graphics, bookLeft(), bookTop());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        guideStackHits.clear();
        renderPageContent(graphics, mouseX, mouseY);
        renderWidgets(graphics, mouseX, mouseY, partialTick);
        guideStackHits.stream().filter(hit -> hit.contains(mouseX, mouseY)).findFirst()
                .ifPresent(hit -> graphics.renderTooltip(font, hit.stack(), mouseX, mouseY));
    }

    private void renderPageContent(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = bookLeft();
        int top = bookTop();
        drawCenteredHeading(graphics, screenHeading(), left + LEFT_PAGE_X, top + 29, PAGE_WIDTH);
        drawCenteredHeading(graphics, rightHeading(), left + RIGHT_PAGE_X, top + 29, PAGE_WIDTH);
        switch (mode) {
            case HOME -> renderHome(graphics, left, top);
            case CATEGORY -> renderCategory(graphics, left, top);
            case CHAPTER -> renderChapter(graphics, left, top);
            case SEARCH -> renderSearch(graphics, left, top);
        }
    }

    private void drawCenteredHeading(GuiGraphics graphics, Component heading, int x, int y, int width) {
        int drawX = x + Math.max(0, (width - font.width(heading)) / 2);
        graphics.drawString(font, heading, drawX, y, 0x303030, false);
        graphics.fill(x + 4, y + 12, x + width - 4, y + 13, 0xFF8B8372);
        graphics.fill(x + width / 2 - 2, y + 12, x + width / 2 + 2, y + 13, 0xFFE6D8B8);
    }

    private void renderBookChrome(GuiGraphics graphics, int left, int top) {
        graphics.fill(left, top + 4, left + BOOK_WIDTH, top + BOOK_HEIGHT - 4, 0xFF2A2117);
        graphics.fill(left + 5, top, left + BOOK_WIDTH - 5, top + BOOK_HEIGHT, 0xFF493A26);
        graphics.fill(left + 10, top + 7, left + BOOK_WIDTH - 10, top + BOOK_HEIGHT - 7, 0xFFF1E7C9);
        graphics.fill(left + 14, top + 11, left + BOOK_WIDTH / 2 - 4, top + BOOK_HEIGHT - 11, 0xFFFFF8E3);
        graphics.fill(left + BOOK_WIDTH / 2 + 4, top + 11, left + BOOK_WIDTH - 14, top + BOOK_HEIGHT - 11,
                0xFFFFF8E3);
        graphics.fill(left + BOOK_WIDTH / 2 - 4, top + 8, left + BOOK_WIDTH / 2 + 4, top + BOOK_HEIGHT - 8,
                0xFF8B7655);
        graphics.fill(left + BOOK_WIDTH / 2 - 1, top + 8, left + BOOK_WIDTH / 2 + 1, top + BOOK_HEIGHT - 8,
                0xFF3A2B1D);
        graphics.fill(left + 17, top + 14, left + 18, top + BOOK_HEIGHT - 14, 0xFFD0C29D);
        graphics.fill(left + BOOK_WIDTH - 18, top + 14, left + BOOK_WIDTH - 17, top + BOOK_HEIGHT - 14,
                0xFFD0C29D);
    }

    private Component rightHeading() {
        return switch (mode) {
            case HOME, CATEGORY -> Component.translatable("guide.cultivation.jade.chapters");
            case CHAPTER -> Component.translatable("guide.cultivation.jade.details");
            case SEARCH -> Component.translatable("guide.cultivation.jade.search_results", searchResults.size());
        };
    }

    private void renderHome(GuiGraphics graphics, int left, int top) {
        renderRuntimeIcon(graphics, ResourceLocation.parse("cultivation:jade_guide"),
                left + LEFT_PAGE_X + 4, top + CONTENT_Y - 18);
        int summaryLines = drawWrappedText(graphics, Component.translatable(JadeGuideProgression.summaryKey(stage)),
                left + LEFT_PAGE_X, top + CONTENT_Y, PAGE_WIDTH, 0x404040, 3);
        int nextGoalY = top + CONTENT_Y + Math.max(LINE_HEIGHT, summaryLines * LINE_HEIGHT) + 8;
        graphics.drawString(font, Component.translatable("guide.cultivation.jade.next_goal"),
                left + LEFT_PAGE_X, nextGoalY, 0x315531, false);
        drawWrappedText(graphics, Component.translatable(JadeGuideProgression.nextGoalKey(
                        stage, stageTenInfiniteImmortalYuan)),
                left + LEFT_PAGE_X, nextGoalY + LINE_HEIGHT + 2, PAGE_WIDTH, 0x404040, 7);
        renderUnlockProgress(graphics, left + LEFT_PAGE_X, top + 184);
    }

    private void renderUnlockProgress(GuiGraphics graphics, int x, int y) {
        int unlocked = (int) book.chapters().stream().filter(chapter -> chapter.isUnlocked(stage)).count();
        int total = Math.max(1, book.chapters().size());
        graphics.drawString(font, Component.translatable("guide.cultivation.jade.unlock_progress", unlocked, total),
                x, y - 11, 0x4A4337, false);
        graphics.fill(x, y, x + PAGE_WIDTH, y + 9, 0xFF3B3327);
        graphics.fill(x + 2, y + 2, x + PAGE_WIDTH - 2, y + 7, 0xFF6E6655);
        int filled = (PAGE_WIDTH - 4) * unlocked / total;
        graphics.fill(x + 2, y + 2, x + 2 + filled, y + 7, 0xFFB6C93E);
    }

    private void renderCategory(GuiGraphics graphics, int left, int top) {
        JadeGuideCategory category = book.categories().stream()
                .filter(value -> value.id().equals(selectedCategory)).findFirst().orElse(null);
        if (category == null) return;
        renderRuntimeIcon(graphics, category.iconId(), left + LEFT_PAGE_X + 4, top + CONTENT_Y - 18);
        drawWrappedText(graphics, Component.translatable("guide.cultivation.jade.category." + category.id() + ".summary"),
                left + LEFT_PAGE_X, top + CONTENT_Y, PAGE_WIDTH, 0x404040, 10);
        int unlocked = (int) book.chaptersIn(category.id()).stream().filter(chapter -> chapter.isUnlocked(stage)).count();
        graphics.drawString(font, Component.translatable("guide.cultivation.jade.category_progress",
                        unlocked, book.chaptersIn(category.id()).size()),
                left + LEFT_PAGE_X, top + 184, 0x315531, false);
    }

    private void renderChapter(GuiGraphics graphics, int left, int top) {
        if (selectedChapter == null) return;
        renderRuntimeIcon(graphics, selectedChapter.iconId(), left + LEFT_PAGE_X + 4, top + CONTENT_Y - 18);
        if (!selectedChapter.isUnlocked(stage)) {
            int lockLines = drawWrappedText(graphics,
                    Component.translatable(selectedChapter.lockSummaryKey(), selectedChapter.minimumStage()),
                    left + LEFT_PAGE_X, top + CONTENT_Y, PAGE_WIDTH, 0xAA3333, 5);
            drawWrappedText(graphics, Component.translatable(selectedChapter.summaryKey()),
                    left + LEFT_PAGE_X, top + CONTENT_Y + lockLines * LINE_HEIGHT + 8,
                    PAGE_WIDTH, 0x505050, 9);
            return;
        }
        renderChapterPage(graphics, left + LEFT_PAGE_X, top + CONTENT_Y, currentPage);
        renderChapterPage(graphics, left + RIGHT_PAGE_X, top + CONTENT_Y, currentPage + 1);
        Component indicator = Component.translatable("book.pageIndicator",
                Math.min(currentPage + 1, chapterPages.size()), chapterPages.size());
        graphics.drawString(font, indicator, left + BOOK_WIDTH / 2 - 8 - font.width(indicator),
                top + 202, 0x404040, false);
    }

    private void renderChapterPage(GuiGraphics graphics, int x, int y, int pageIndex) {
        if (pageIndex < 0 || pageIndex >= chapterPages.size()) return;
        if (selectedChapter != null && selectedChapter.id().equals("progression.recipes")) {
            renderRecipePage(graphics, x, y, pageIndex);
            return;
        }
        JadeGuidePage page = chapterPages.get(pageIndex);
        for (String line : page.lines()) {
            graphics.drawString(font, line, x, y, 0x303030, false);
            y += LINE_HEIGHT;
        }
    }

    private void renderSearch(GuiGraphics graphics, int left, int top) {
        if (searchResults.isEmpty()) {
            graphics.drawString(font, Component.translatable("guide.cultivation.jade.search_empty"),
                    left + RIGHT_PAGE_X, top + CONTENT_Y, 0x606060, false);
        }
    }

    private void renderWidgets(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }


    private int drawWrappedText(GuiGraphics graphics, Component text, int x, int y,
                                int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, width);
        int count = Math.min(maxLines, lines.size());
        for (int index = 0; index < count; index++) {
            graphics.drawString(font, lines.get(index), x, y + index * LINE_HEIGHT, color, false);
        }
        return count;
    }

    private void showHome(boolean clearSearch) {
        showHome(clearSearch, true);
    }

    private void showHome(boolean clearSearch, boolean pushHistory) {
        if (clearSearch) setSearchValue("");
        mode = Mode.HOME;
        selectedCategory = null;
        selectedChapter = null;
        chapterPages = List.of();
        currentPage = 0;
        if (pushHistory && !applyingHistory) session.history().push(JadeGuideLocation.home());
        rebuildDynamicButtons();
    }

    private void openCategory(String categoryId) {
        openCategory(categoryId, true);
    }

    private void openCategory(String categoryId, boolean pushHistory) {
        mode = Mode.CATEGORY;
        selectedCategory = categoryId;
        selectedChapter = null;
        chapterPages = List.of();
        currentPage = 0;
        if (pushHistory && !applyingHistory) {
            session.history().push(JadeGuideLocation.category(categoryId));
        }
        rebuildDynamicButtons();
    }

    private void openChapter(JadeGuideChapter chapter, int requestedPage, boolean pushHistory) {
        selectedChapter = chapter;
        selectedCategory = chapter.categoryId();
        mode = Mode.CHAPTER;
        chapterPages = chapter.isUnlocked(stage) ? paginate(chapter) : List.of();
        currentPage = chapterPages.isEmpty() ? 0
                : Mth.clamp(Math.max(0, requestedPage / 2 * 2), 0, chapterPages.size() - 1);
        session.recordChapter(chapter.id(), currentPage);
        if (pushHistory && !applyingHistory) {
            session.history().push(JadeGuideLocation.chapter(chapter.id(), currentPage, searchBox.getValue()));
        }
        rebuildDynamicButtons();
    }

    private List<JadeGuidePage> paginate(JadeGuideChapter chapter) {
        JadeGuidePaginator paginator = new JadeGuidePaginator(font::width, PAGE_WIDTH, CONTENT_LINES);
        if (chapter.id().equals("progression.recipes")) {
            List<RecipeHolder<?>> recipes = cultivationRecipes();
            if (recipes.isEmpty()) {
                return List.of(new JadeGuidePage(Component.translatable(chapter.titleKey()).getString(),
                        List.of(Component.translatable("guide.cultivation.jade.recipes.empty").getString())));
            }
            return recipes.stream().map(holder -> new JadeGuidePage(
                    holder.value().getResultItem(minecraft.level.registryAccess()).getHoverName().getString(),
                    List.<String>of())).toList();
        }
        String body = Component.translatable(chapter.bodyKey()).getString();
        return paginator.paginate(Component.translatable(chapter.titleKey()).getString(),
                java.util.Arrays.asList(body.split("\\n\\n")));
    }

    private List<String> recipeParagraphs() {
        if (minecraft == null || minecraft.level == null) {
            return List.of(Component.translatable("guide.cultivation.jade.recipes.unavailable").getString());
        }
        List<String> paragraphs = new ArrayList<>();
        minecraft.level.getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.id().getNamespace().equals("cultivation"))
                .sorted(java.util.Comparator.comparing(holder -> holder.id().toString()))
                .forEach(holder -> paragraphs.add(formatRecipe(holder)));
        return paragraphs.isEmpty()
                ? List.of(Component.translatable("guide.cultivation.jade.recipes.empty").getString())
                : paragraphs;
    }

    private List<RecipeHolder<?>> cultivationRecipes() {
        if (minecraft == null || minecraft.level == null) return List.of();
        List<RecipeHolder<?>> recipes = new ArrayList<>();
        minecraft.level.getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.id().getNamespace().equals("cultivation"))
                .sorted(java.util.Comparator.comparing(holder -> holder.id().toString()))
                .forEach(recipes::add);
        return List.copyOf(recipes);
    }

    private void renderRecipePage(GuiGraphics graphics, int x, int y, int pageIndex) {
        List<RecipeHolder<?>> recipes = cultivationRecipes();
        if (pageIndex < 0 || pageIndex >= recipes.size()) return;
        RecipeHolder<?> holder = recipes.get(pageIndex);
        Recipe<?> recipe = holder.value();
        ItemStack output = recipe.getResultItem(minecraft.level.registryAccess());
        graphics.drawCenteredString(font, output.getHoverName(), x + PAGE_WIDTH / 2, y, 0x303030);

        int gridX = x + 5;
        int gridY = y + 22;
        if (recipe instanceof AbstractCookingRecipe cooking) {
            renderCookingRecipePage(graphics, cooking, output, x, gridY, cycleIndex());
            drawWrappedText(graphics, Component.literal(holder.id().toString()), x + 5, gridY + 76,
                    PAGE_WIDTH - 10, 0x777064, 2);
            return;
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                VanillaGuiPainter.slot(graphics, gridX + column * 18, gridY + row * 18, true);
            }
        }
        List<Ingredient> ingredients = recipe.getIngredients();
        int cycle = cycleIndex();
        int shapedWidth = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        int shapedHeight = recipe instanceof ShapedRecipe shaped ? shaped.getHeight() : 3;
        for (int index = 0; index < Math.min(9, ingredients.size()); index++) {
            Ingredient ingredient = ingredients.get(index);
            ItemStack[] choices = ingredient.getItems();
            if (choices.length == 0) continue;
            ItemStack stack = choices[cycle % choices.length];
            int column = recipe instanceof ShapedRecipe ? index % shapedWidth : index % 3;
            int row = recipe instanceof ShapedRecipe ? index / shapedWidth : index / 3;
            int slotX = gridX + column * 18;
            int slotY = gridY + row * 18;
            graphics.renderItem(stack, slotX, slotY);
            guideStackHits.add(new GuideStackHit(slotX, slotY, stack));
        }

        int arrowX = x + 64;
        int arrowY = gridY + 19;
        VanillaGuiPainter.craftingArrow(graphics, arrowX, arrowY);
        int resultX = x + 107;
        int resultY = gridY + 18;
        VanillaGuiPainter.craftingResultSlot(graphics, resultX - 4, resultY - 4);
        graphics.renderItem(output, resultX, resultY);
        graphics.renderItemDecorations(font, output, resultX, resultY);
        guideStackHits.add(new GuideStackHit(resultX, resultY, output));

        String type = recipe instanceof AbstractCookingRecipe
                ? Component.translatable("guide.cultivation.jade.recipes.cooking").getString()
                : recipe instanceof CraftingRecipe
                ? Component.translatable("guide.cultivation.jade.recipes.crafting").getString()
                : BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()).getPath();
        graphics.drawCenteredString(font, type, x + PAGE_WIDTH / 2, gridY + 63, 0x555555);
        drawWrappedText(graphics, Component.literal(holder.id().toString()), x + 5, gridY + 76,
                PAGE_WIDTH - 10, 0x777064, 2);
    }

    private void renderCookingRecipePage(GuiGraphics graphics, AbstractCookingRecipe recipe,
                                         ItemStack output, int x, int y, int cycle) {
        int inputX = x + 14;
        int inputY = y + 18;
        VanillaGuiPainter.slot(graphics, inputX, inputY, true);
        Ingredient ingredient = recipe.getIngredients().isEmpty()
                ? Ingredient.EMPTY : recipe.getIngredients().getFirst();
        ItemStack[] choices = ingredient.getItems();
        if (choices.length > 0) {
            ItemStack input = choices[cycle % choices.length];
            graphics.renderItem(input, inputX, inputY);
            guideStackHits.add(new GuideStackHit(inputX, inputY, input));
        }
        VanillaGuiPainter.craftingArrow(graphics, x + 51, y + 19);
        int resultX = x + 101;
        int resultY = y + 18;
        VanillaGuiPainter.craftingResultSlot(graphics, resultX - 4, resultY - 4);
        graphics.renderItem(output, resultX, resultY);
        graphics.renderItemDecorations(font, output, resultX, resultY);
        guideStackHits.add(new GuideStackHit(resultX, resultY, output));
        graphics.drawCenteredString(font,
                Component.translatable("guide.cultivation.jade.recipes.cooking"),
                x + PAGE_WIDTH / 2, y + 56, 0x555555);
    }

    private int cycleIndex() {
        return (int) ((minecraft.level.getGameTime() / 20L) & Integer.MAX_VALUE);
    }

    private String formatRecipe(RecipeHolder<?> holder) {
        ItemStack result = holder.value().getResultItem(minecraft.level.registryAccess());
        String type = holder.value() instanceof AbstractCookingRecipe
                ? Component.translatable("guide.cultivation.jade.recipes.cooking").getString()
                : holder.value() instanceof CraftingRecipe
                ? Component.translatable("guide.cultivation.jade.recipes.crafting").getString()
                : BuiltInRegistries.RECIPE_SERIALIZER.getKey(holder.value().getSerializer()).getPath();
        List<String> ingredients = new ArrayList<>();
        int slot = 1;
        for (Ingredient ingredient : holder.value().getIngredients()) {
            if (ingredient.isEmpty()) {
                ingredients.add(slot++ + ": -");
                continue;
            }
            String choices = java.util.Arrays.stream(ingredient.getItems())
                    .map(stack -> stack.getHoverName().getString())
                    .distinct().limit(4).collect(java.util.stream.Collectors.joining("/"));
            ingredients.add(slot++ + ": " + choices);
        }
        String output = result.getHoverName().getString() + " x" + result.getCount();
        return Component.translatable("guide.cultivation.jade.recipes.entry", output, type,
                ingredients.isEmpty() ? "-" : String.join(", ", ingredients)).getString();
    }

    private void onSearchChanged(String query) {
        session.setSearchQuery(query);
        if (suppressSearchResponder) return;
        if (query.isBlank()) {
            showHome(false, true);
        } else {
            showSearch(query, 0, true);
        }
    }

    private void showSearch(String query, int requestedPage, boolean pushHistory) {
        setSearchValue(query);
        mode = Mode.SEARCH;
        searchResults = JadeGuideSearch.search(searchDocuments(), query);
        int lastPage = Math.max(0, (searchResults.size() + 8) / 9 - 1);
        currentPage = Mth.clamp(requestedPage, 0, lastPage);
        if (pushHistory && !applyingHistory) {
            session.history().push(JadeGuideLocation.search(query, currentPage));
        }
        rebuildDynamicButtons();
    }

    private void setSearchValue(String value) {
        if (searchBox == null || searchBox.getValue().equals(value)) return;
        suppressSearchResponder = true;
        searchBox.setValue(value);
        suppressSearchResponder = false;
        session.setSearchQuery(value);
    }

    private List<JadeGuideSearch.Document> searchDocuments() {
        List<JadeGuideSearch.Document> documents = new ArrayList<>();
        for (JadeGuideChapter chapter : book.chapters()) {
            boolean unlocked = chapter.isUnlocked(stage);
            documents.add(new JadeGuideSearch.Document(chapter.id(),
                    Component.translatable(chapter.titleKey()).getString(),
                    Component.translatable(unlocked ? chapter.bodyKey() : chapter.summaryKey()).getString(),
                    List.of(Component.translatable(chapter.keywordsKey()).getString().split(",")),
                    unlocked));
        }
        return documents;
    }

    private void rebuildDynamicButtons() {
        for (Button button : dynamicButtons) removeWidget(button);
        dynamicButtons.clear();
        int left = bookLeft();
        int top = bookTop();
        switch (mode) {
            case HOME -> {
                int y = top + CONTENT_Y;
                for (JadeGuideCategory category : book.categories()) {
                    addDynamic(Button.builder(Component.translatable(category.titleKey()),
                                    ignored -> openCategory(category.id()))
                            .bounds(left + RIGHT_PAGE_X, y, PAGE_WIDTH, BUTTON_HEIGHT).build());
                    y += 17;
                }
                int recentY = top + 184;
                if (!session.recent().isEmpty()) {
                    JadeGuideChapter recent = book.chaptersById().get(session.recent().getFirst());
                    if (recent != null) addDynamic(Button.builder(
                                    Component.translatable("guide.cultivation.jade.recent",
                                            Component.translatable(recent.titleKey())),
                                    ignored -> openChapter(recent, session.lastPage(recent.id()), true))
                            .bounds(left + RIGHT_PAGE_X, recentY, PAGE_WIDTH, BUTTON_HEIGHT).build());
                }
            }
            case CATEGORY -> {
                int y = top + CONTENT_Y;
                for (JadeGuideChapter chapter : book.chaptersIn(selectedCategory)) {
                    MutableComponent label = Component.translatable(chapter.titleKey());
                    if (!chapter.isUnlocked(stage)) label.append(Component.literal(" ["))
                            .append(Component.translatable("guide.cultivation.jade.lock.short"))
                            .append(Component.literal("]")).withStyle(ChatFormatting.DARK_GRAY);
                    addDynamic(Button.builder(label, ignored -> openChapter(chapter,
                                    session.lastPage(chapter.id()), true))
                            .bounds(left + RIGHT_PAGE_X, y, PAGE_WIDTH, BUTTON_HEIGHT)
                            .tooltip(Tooltip.create(chapter.isUnlocked(stage)
                                    ? Component.translatable(chapter.summaryKey())
                                    : Component.translatable(chapter.lockSummaryKey(), chapter.minimumStage())))
                            .build());
                    y += 17;
                }
            }
            case SEARCH -> {
                int start = currentPage * 9;
                int y = top + CONTENT_Y;
                for (int index = start; index < Math.min(start + 9, searchResults.size()); index++) {
                    JadeGuideSearch.Result result = searchResults.get(index);
                    MutableComponent label = Component.literal(result.title());
                    if (!result.unlocked()) label.append(Component.literal(" ["))
                            .append(Component.translatable("guide.cultivation.jade.lock.short"))
                            .append(Component.literal("]"));
                    JadeGuideChapter chapter = book.chaptersById().get(result.chapterId());
                    addDynamic(Button.builder(label, ignored -> openChapter(chapter,
                                    session.lastPage(chapter.id()), true))
                            .bounds(left + RIGHT_PAGE_X, y, PAGE_WIDTH, BUTTON_HEIGHT)
                            .tooltip(Tooltip.create(Component.literal(result.body()))).build());
                    y += 17;
                }
            }
            case CHAPTER -> {
            }
        }
        updateControlState();
    }

    private void addDynamic(Button button) {
        dynamicButtons.add(addRenderableWidget(button));
    }

    private void previousPage() {
        if (mode == Mode.CHAPTER && currentPage > 0) {
            currentPage = Math.max(0, currentPage - 2);
            saveCurrentLocation();
        } else if (mode == Mode.SEARCH && currentPage > 0) {
            currentPage--;
            session.history().push(JadeGuideLocation.search(searchBox.getValue(), currentPage));
            rebuildDynamicButtons();
        }
        updateControlState();
    }

    private void nextPage() {
        int pageCount = pageCount();
        int step = mode == Mode.CHAPTER ? 2 : 1;
        if (currentPage + step < pageCount) {
            currentPage += step;
            if (mode == Mode.SEARCH) {
                session.history().push(JadeGuideLocation.search(searchBox.getValue(), currentPage));
                rebuildDynamicButtons();
            }
            else saveCurrentLocation();
        }
        updateControlState();
    }

    private void saveCurrentLocation() {
        if (selectedChapter == null) return;
        session.recordChapter(selectedChapter.id(), currentPage);
        session.history().push(JadeGuideLocation.chapter(selectedChapter.id(), currentPage, searchBox.getValue()));
    }

    private int pageCount() {
        if (mode == Mode.CHAPTER) return Math.max(1, chapterPages.size());
        if (mode == Mode.SEARCH) return Math.max(1, (searchResults.size() + 8) / 9);
        return 1;
    }

    private void navigateHistory(boolean forward) {
        JadeGuideHistory history = session.history();
        JadeGuideLocation location = (forward ? history.forward() : history.back()).orElse(null);
        if (location == null) return;
        restoreLocation(location);
    }

    private void restoreLocation(JadeGuideLocation location) {
        applyingHistory = true;
        try {
            switch (location.kind()) {
                case HOME -> showHome(true, false);
                case CATEGORY -> {
                    setSearchValue(location.searchQuery());
                    openCategory(location.targetId(), false);
                }
                case SEARCH -> showSearch(location.searchQuery(), location.pageIndex(), false);
                case CHAPTER -> {
                    JadeGuideChapter chapter = book.chaptersById().get(location.targetId());
                    if (chapter != null) {
                        setSearchValue(location.searchQuery());
                        openChapter(chapter, location.pageIndex(), false);
                    }
                }
            }
        } finally {
            applyingHistory = false;
        }
    }

    private void updateControlState() {
        if (historyBack == null) return;
        historyBack.active = session.history().canBack();
        historyForward.active = session.history().canForward();
        pageBack.visible = (mode == Mode.CHAPTER || mode == Mode.SEARCH) && currentPage > 0;
        pageForward.visible = (mode == Mode.CHAPTER || mode == Mode.SEARCH)
                && currentPage + (mode == Mode.CHAPTER ? 2 : 1) < pageCount();
    }

    private Component screenHeading() {
        return switch (mode) {
            case HOME -> Component.translatable("guide.cultivation.jade.title");
            case CATEGORY -> book.categories().stream().filter(category -> category.id().equals(selectedCategory))
                    .findFirst().<Component>map(category -> Component.translatable(category.titleKey()))
                    .orElseGet(() -> Component.translatable("guide.cultivation.jade.title"));
            case CHAPTER -> selectedChapter == null ? Component.translatable("guide.cultivation.jade.title")
                    : Component.translatable(selectedChapter.titleKey());
            case SEARCH -> Component.translatable("guide.cultivation.jade.search_results", searchResults.size());
        };
    }

    private void renderRuntimeIcon(GuiGraphics graphics, ResourceLocation id, int x, int y) {
        BuiltInRegistries.ITEM.getOptional(id)
                .ifPresent(item -> graphics.renderItem(new ItemStack(item), x, y));
    }

    private int bookLeft() {
        return (width - BOOK_WIDTH) / 2;
    }

    private int bookTop() {
        return (height - BOOK_HEIGHT) / 2;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_F) {
            setInitialFocus(searchBox);
            searchBox.setFocused(true);
            return true;
        }
        if (searchBox.isFocused()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            previousPage();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            nextPage();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchBox.isFocused()) {
            navigateHistory(false);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME && !searchBox.isFocused()) {
            showHome(true);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public Component getNarrationMessage() {
        return Component.translatable("guide.cultivation.jade.narration", screenHeading(),
                currentPage + 1, pageCount());
    }

    @Override
    protected void updateNarrationState(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getNarrationMessage());
        output.add(NarratedElementType.USAGE, Component.translatable("guide.cultivation.jade.narration_usage"));
        super.updateNarrationState(output);
    }

    private enum Mode {
        HOME,
        CATEGORY,
        CHAPTER,
        SEARCH
    }

    private record GuideStackHit(int x, int y, ItemStack stack) {
        private GuideStackHit {
            stack = stack.copy();
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
        }
    }
}
