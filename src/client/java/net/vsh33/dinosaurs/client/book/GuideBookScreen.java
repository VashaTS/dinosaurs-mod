package net.vsh33.dinosaurs.client.book;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.vsh33.dinosaurs.Dinosaurs;
import net.vsh33.dinosaurs.client.book.GuideBookContents.Category;
import net.vsh33.dinosaurs.client.book.GuideBookContents.Entry;
import net.vsh33.dinosaurs.client.book.GuideBookContents.Framing;
import net.vsh33.dinosaurs.client.book.GuideBookContents.Location;
import net.vsh33.dinosaurs.client.book.GuideBookContents.RecipeGrid;
import net.vsh33.dinosaurs.client.book.GuideBookContents.StatRow;
import net.vsh33.dinosaurs.config.DinoConfig;
import net.vsh33.dinosaurs.entity.ModEntities;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Dinosaur field guide: a two-page book with clickable chapters and links, a live
 * 3D picture of every dinosaur (drag to rotate) and crafting grids drawn with real items.
 */
public class GuideBookScreen extends Screen {
	public static final Identifier TEXTURE = Dinosaurs.id("textures/gui/guidebook.png");
	public static final int BOOK_W = 256;
	public static final int BOOK_H = 192;
	public static final int TEX = 256;
	public static final int PAGE_W = 104;
	public static final int LEFT = 16;
	public static final int RIGHT = 138;
	public static final int TOP = 14;
	public static final int PAGE_BOTTOM = 164;
	public static final int PAGE_H = PAGE_BOTTOM - TOP;
	public static final int PIC_H = 66;

	private static final int SLOT = 18;
	private static final int SLOT_U = 0;
	private static final int SLOT_HOVER_U = 18;
	private static final int ARROW_U = 40;
	private static final int ARROW_V = 194;
	private static final int ARROW_W = 22;
	private static final int ARROW_H = 15;
	private static final int HOME_U = 64;
	private static final int BACK_U = 80;
	private static final int ICON = 16;
	private static final int RESULT_U = 114;
	private static final int WIDGET_V = 192;
	private static final int RECIPE_H = 3 * SLOT;

	private static final int INK = 0xFF3A2A18;
	private static final int INK_SOFT = 0xFF5A4430;
	private static final int LINK = 0xFF2C4A7C;
	private static final int LINK_HOVER = 0xFF1A3060;
	private static final int OFF = 0xFF8B2A22;
	private static final int ON = 0xFF2E6B3A;
	private static final int HOVER_FILL = 0x332A1C10;
	private static final int FRAME = 0xFFB08A4A;
	private static final int FRAME_DARK = 0xFF7A5A30;
	private static final int PIC_SKY_TOP = 0xFFBFD8E6;
	private static final int PIC_SKY_BOTTOM = 0xFFE9E2C4;
	private static final int PIC_GROUND = 0xFF8FA65A;
	public static final int ICON_TITLE_W = PAGE_W - 18;
	/** Chapter list row: icon plus up to two lines of link text. */
	public static final int LIST_ROW = 22;
	private static final float DEFAULT_YAW = -35.0F;

	private final GuideBookContents contents;
	private final List<Location> history = new ArrayList<>();
	private final List<Hotspot> hotspots = new ArrayList<>();
	private final Map<EntityType<?>, Mob> previews = new HashMap<>();
	private Location location = Location.index();
	private int leftPos;
	private int topPos;
	private PageButton prevButton;
	private PageButton nextButton;
	private float yaw = DEFAULT_YAW;
	private int picX0;
	private int picY0;
	private int picX1;
	private int picY1;
	private boolean draggingPicture;

	private record Hotspot(int x, int y, int w, int h, Runnable action, ItemStack tooltipItem, Component tooltipText) {
		boolean contains(double mx, double my) {
			return mx >= x && my >= y && mx < x + w && my < y + h;
		}
	}

	public GuideBookScreen() {
		super(Component.translatable("item.dinosaurs.guide_book"));
		this.contents = GuideBookContents.create();
	}

	@Override
	protected void init() {
		this.leftPos = (this.width - BOOK_W) / 2;
		this.topPos = (this.height - BOOK_H) / 2;
		this.prevButton = this.addRenderableWidget(new PageButton(this.leftPos + 18, this.topPos + 172, false, button -> this.turnPage(-1), true));
		this.nextButton = this.addRenderableWidget(new PageButton(this.leftPos + BOOK_W - 41, this.topPos + 172, true, button -> this.turnPage(1), true));
		this.updateButtons();
	}

	public void openIndex() {
		this.goTo(Location.index(), true);
	}

	public void openDinoList() {
		this.goTo(Location.dinoList(), true);
	}

	public void openItemList() {
		this.goTo(Location.itemList(), true);
	}

	public void openSettings() {
		this.goTo(Location.settings(), true);
	}

	public void openEntry(String id) {
		this.goTo(Location.entry(id), true);
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	public Location location() {
		return this.location;
	}

	private void goTo(Location target, boolean recordHistory) {
		if (recordHistory && !GuideBookContents.same(this.location, target)) {
			this.history.add(this.location);
			if (this.history.size() > 64) {
				this.history.removeFirst();
			}
		}
		this.location = target;
		this.yaw = DEFAULT_YAW;
		this.updateButtons();
	}

	private void goBack() {
		if (this.history.isEmpty()) {
			return;
		}
		this.location = this.history.removeLast();
		this.yaw = DEFAULT_YAW;
		this.updateButtons();
		this.pageSound();
	}

	private void turnPage(int delta) {
		int index = this.contents.indexOf(this.location) + delta;
		List<Location> order = this.contents.readingOrder();
		if (index < 0 || index >= order.size()) {
			return;
		}
		this.goTo(order.get(index), true);
	}

	private void updateButtons() {
		int index = this.contents.indexOf(this.location);
		if (this.prevButton != null) {
			this.prevButton.visible = index > 0;
			this.nextButton.visible = index < this.contents.readingOrder().size() - 1;
		}
	}

	private void pageSound() {
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
	}

	private void clickSound() {
		this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}

	@Override
	public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, BOOK_W, BOOK_H, TEX, TEX);
		this.hotspots.clear();
		this.picX1 = this.picX0;
		switch (this.location.section()) {
			case INDEX -> this.drawIndex(graphics, mouseX, mouseY);
			case DINO_LIST -> this.drawList(graphics, mouseX, mouseY, Component.translatable("book.dinosaurs.category.dinosaurs"), this.contents.dinoEntries());
			case ITEM_LIST -> this.drawList(graphics, mouseX, mouseY, Component.translatable("book.dinosaurs.category.items"), this.contents.itemEntries());
			case SETTINGS -> this.drawSettings(graphics);
			case ENTRY -> this.drawEntry(graphics, mouseX, mouseY);
		}
		this.drawNavIcons(graphics, mouseX, mouseY);
		this.applyHoverTooltip(graphics, mouseX, mouseY);
	}

	private void drawIndex(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int lx = this.leftPos + LEFT;
		int ly = this.topPos + TOP;
		int y = ly + this.drawTitle(graphics, lx, ly, PAGE_W, Component.translatable("book.dinosaurs.title_heading"));
		y = this.drawWrapped(graphics, Component.translatable("book.dinosaurs.intro"), lx, y, PAGE_W, INK);
		y += 6;
		this.drawRecipe(graphics, lx, y, GuideBookContents.guideBookRecipe(), Component.translatable("book.dinosaurs.heading.book_recipe"), mouseX, mouseY);

		int rx = this.leftPos + RIGHT;
		int ry = this.topPos + TOP;
		ry += this.drawTitle(graphics, rx, ry, PAGE_W, Component.translatable("book.dinosaurs.heading.index"));
		for (Category category : this.contents.categories()) {
			ry += this.drawLinkRow(graphics, rx, ry, PAGE_W, category.icon(), category.title(), category.target(), mouseX, mouseY, 22);
		}
		ry += 4;
		this.drawWrapped(graphics, Component.translatable("book.dinosaurs.index_hint"), rx, ry, PAGE_W, INK_SOFT);
	}

	private void drawList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Component title, List<Entry> entries) {
		int lx = this.leftPos + LEFT;
		int ly = this.topPos + TOP;
		ly += this.drawTitle(graphics, lx, ly, PAGE_W, title);
		int bottom = this.topPos + PAGE_BOTTOM;
		int row = listRow(this.font, title, entries);
		int i = 0;
		while (i < entries.size() && ly + rowHeight(this.font, entries.get(i).title(), row) <= bottom) {
			Entry entry = entries.get(i++);
			ly += this.drawLinkRow(graphics, lx, ly, PAGE_W, entry.icon(), entry.title(), Location.entry(entry.id()), mouseX, mouseY, row);
		}
		int rx = this.leftPos + RIGHT;
		int ry = this.topPos + TOP;
		while (i < entries.size() && ry + rowHeight(this.font, entries.get(i).title(), row) <= bottom) {
			Entry entry = entries.get(i++);
			ry += this.drawLinkRow(graphics, rx, ry, PAGE_W, entry.icon(), entry.title(), Location.entry(entry.id()), mouseX, mouseY, row);
		}
	}

	private void drawSettings(GuiGraphicsExtractor graphics) {
		DinoConfig config = DinoConfig.get();
		int lx = this.leftPos + LEFT;
		int ly = this.topPos + TOP;
		ly += this.drawTitle(graphics, lx, ly, PAGE_W, Component.translatable("book.dinosaurs.category.settings"));
		ly = this.drawWrapped(graphics, Component.translatable("book.dinosaurs.settings.daylight"), lx, ly, PAGE_W, INK);
		ly += 2;
		ly = this.drawWrapped(graphics, status(config.hostileSpawnInDaylight), lx, ly, PAGE_W, config.hostileSpawnInDaylight ? ON : OFF);
		ly += 6;
		this.drawWrapped(graphics, Component.translatable("book.dinosaurs.settings.how"), lx, ly, PAGE_W, INK_SOFT);

		int rx = this.leftPos + RIGHT;
		int ry = this.topPos + TOP;
		ry += this.drawTitle(graphics, rx, ry, PAGE_W, Component.translatable("book.dinosaurs.settings.spawning"));
		ry = this.drawWrapped(graphics, Component.translatable("book.dinosaurs.settings.legend"), rx, ry, PAGE_W, INK_SOFT) + 3;
		for (Entry entry : this.contents.dinoEntries()) {
			boolean on = config.isSpawnEnabled(entry.entity());
			Component name = on ? entry.title() : entry.title().copy().withStyle(ChatFormatting.STRIKETHROUGH);
			graphics.text(this.font, this.fitOneLine(name, PAGE_W), rx, ry, on ? ON : OFF, false);
			ry += 10;
		}
	}

	private static Component status(boolean on) {
		return Component.translatable(on ? "book.dinosaurs.status.on" : "book.dinosaurs.status.off");
	}

	private void drawEntry(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Entry entry = this.contents.entry(this.location.entryId());
		if (entry == null) {
			return;
		}
		if (entry.isDino()) {
			this.drawDino(graphics, entry, mouseX, mouseY);
		} else {
			this.drawItem(graphics, entry, mouseX, mouseY);
		}
	}

	private void drawDino(GuiGraphicsExtractor graphics, Entry entry, int mouseX, int mouseY) {
		int lx = this.leftPos + LEFT;
		int ly = this.topPos + TOP;
		ly += this.drawTitle(graphics, lx, ly, PAGE_W, entry.title());
		this.drawPicture(graphics, entry.entity(), lx, ly, PAGE_W, PIC_H, mouseX, mouseY);
		ly += PIC_H + 4;
		this.drawWrapped(graphics, entry.body(), lx, ly, PAGE_W, INK);

		int rx = this.leftPos + RIGHT;
		int ry = this.topPos + TOP;
		ry += this.drawTitle(graphics, rx, ry, PAGE_W, Component.translatable("book.dinosaurs.heading.habitat"));
		ry = this.drawWrapped(graphics, entry.habitat(), rx, ry, PAGE_W, INK);
		ry += 6;
		ry = this.drawWrapped(graphics, Component.translatable("book.dinosaurs.heading.drops").copy().withStyle(ChatFormatting.BOLD), rx, ry, PAGE_W, INK) + 2;
		int sx = rx;
		for (ItemStack drop : entry.drops()) {
			this.drawSlot(graphics, sx, ry, drop, false, mouseX, mouseY);
			sx += SLOT + 2;
		}
	}

	private void drawItem(GuiGraphicsExtractor graphics, Entry entry, int mouseX, int mouseY) {
		int lx = this.leftPos + LEFT;
		int ly = this.topPos + TOP;
		int y = ly + this.drawIconTitle(graphics, lx, ly, entry.icon(), entry.title());
		boolean statsPage = !entry.stats().isEmpty();
		if (!statsPage) {
			y = this.drawWrapped(graphics, entry.body(), lx, y, PAGE_W, INK);
		}
		if (!entry.droppedBy().isEmpty()) {
			y += 5;
			y = this.drawWrapped(graphics, Component.translatable("book.dinosaurs.heading.dropped_by"), lx, y, PAGE_W, INK_SOFT) + 1;
			if (entry.droppedBy().size() > DROP_LIST_MAX) {
				// Many sources: a grid of spawn-egg slots, each linking to its dinosaur.
				for (int i = 0; i < entry.droppedBy().size(); i++) {
					Entry dino = this.contents.entryFor(entry.droppedBy().get(i));
					this.drawSlot(graphics, lx + (i % DROP_GRID_COLS) * (SLOT + 2), y + (i / DROP_GRID_COLS) * (SLOT + 2), dino.icon(), false, mouseX, mouseY);
				}
				y += droppedByH(entry.droppedBy().size());
			} else {
				for (EntityType<? extends Mob> type : entry.droppedBy()) {
					Entry dino = this.contents.entryFor(type);
					y += this.drawLinkRow(graphics, lx, y, PAGE_W, dino.icon(), dino.title(), Location.entry(dino.id()), mouseX, mouseY, LIST_ROW);
				}
			}
		}
		if (statsPage) {
			// Armor: the stats table fills the left page, the description goes under the recipe.
			this.drawStats(graphics, lx, y, entry);
		}

		int rx = this.leftPos + RIGHT;
		int ry = this.topPos + TOP;
		for (int i = 0; i < entry.recipes().size(); i++) {
			RecipeGrid grid = entry.recipes().get(i);
			boolean header = showHeader(entry, i);
			ry += recipeGap(entry, i);
			ry += this.drawRecipe(graphics, rx, ry, grid, header ? recipeHeader(entry, grid) : null, mouseX, mouseY);
		}
		if (statsPage) {
			this.drawWrapped(graphics, entry.body(), rx, ry + 6, PAGE_W, INK);
		}
	}

	/** A run of smithing recipes under the same heading (the three armor pieces) shares one heading. */
	private static boolean showHeader(Entry entry, int i) {
		if (i == 0) {
			return true;
		}
		RecipeGrid grid = entry.recipes().get(i);
		RecipeGrid prev = entry.recipes().get(i - 1);
		return !(grid.smithing() && prev.smithing() && recipeHeader(entry, grid).equals(recipeHeader(entry, prev)));
	}

	private static int recipeGap(Entry entry, int i) {
		return i == 0 ? 0 : (showHeader(entry, i) ? 8 : 2);
	}

	private static int recipeH(Font font, Entry entry, int i) {
		RecipeGrid grid = entry.recipes().get(i);
		return (showHeader(entry, i) ? textH(font, bold(recipeHeader(entry, grid))) + 3 : 0) + (grid.smithing() ? SLOT : RECIPE_H);
	}

	/** Height of the right page's recipe column (without the text under it). */
	private static int recipesH(Font font, Entry entry) {
		int h = 0;
		for (int i = 0; i < entry.recipes().size(); i++) {
			h += recipeGap(entry, i) + recipeH(font, entry, i);
		}
		return h;
	}

	/** Up to this many "dropped by" dinosaurs are listed with names; more become a slot grid. */
	private static final int DROP_LIST_MAX = 3;
	private static final int DROP_GRID_COLS = (PAGE_W + 2) / (SLOT + 2);

	private static int droppedByH(int count) {
		if (count > DROP_LIST_MAX) {
			int rows = (count + DROP_GRID_COLS - 1) / DROP_GRID_COLS;
			return rows * (SLOT + 2);
		}
		return LIST_ROW * count;
	}

	private static final int STATS_LABEL_W = PAGE_W - 39;
	private static final int STATS_VALUE_MAX_W = 24;
	private static final int STATS_HEAD_H = 18;

	/** Armor table: label, this item's value (under its icon) and the netherite helmet's (under its icon). */
	private void drawStats(GuiGraphicsExtractor graphics, int x, int y, Entry entry) {
		y += 2;
		int c2 = x + PAGE_W - 8;
		int c1 = c2 - statsPitch(this.font, entry.stats());
		int labelW = statsLabelW(this.font, entry.stats());
		graphics.text(this.font, this.fitOneLine(bold(Component.translatable("book.dinosaurs.stats.heading")), statsHeadingW(this.font, entry.stats())), x, y + 5, INK, false);
		graphics.item(entry.icon(), c1 - 8, y);
		this.hotspots.add(new Hotspot(c1 - 8, y, 16, 16, null, entry.icon(), null));
		graphics.item(GuideBookContents.statsReference(entry), c2 - 8, y);
		this.hotspots.add(new Hotspot(c2 - 8, y, 16, 16, null, GuideBookContents.statsReference(entry), null));
		y += STATS_HEAD_H;
		graphics.fill(x, y - 1, x + PAGE_W, y, FRAME);
		y += 1;
		for (StatRow row : entry.stats()) {
			List<FormattedCharSequence> lines = this.font.split(row.label(), labelW);
			int yy = y;
			for (FormattedCharSequence line : lines) {
				graphics.text(this.font, line, x, yy, INK, false);
				yy += 9;
			}
			graphics.text(this.font, row.value(), c1 - this.font.width(row.value()) / 2, y, ON, false);
			graphics.text(this.font, row.netherite(), c2 - this.font.width(row.netherite()) / 2, y, INK_SOFT, false);
			y += statRowH(this.font, row, labelW);
		}
	}

	/** Distance between the two value columns: 21 px, more when a row's two values would touch (e.g. 1152 next to 384). */
	private static int statsPitch(Font font, List<StatRow> stats) {
		int pitch = 21;
		for (StatRow row : stats) {
			pitch = Math.max(pitch, (font.width(row.value()) + font.width(row.netherite()) + 1) / 2 + 3);
		}
		return pitch;
	}

	/** Label column: up to the widest value in the first value column (never wider than the default). */
	private static int statsLabelW(Font font, List<StatRow> stats) {
		int c1 = PAGE_W - 8 - statsPitch(font, stats);
		int w = 0;
		for (StatRow row : stats) {
			w = Math.max(w, font.width(row.value()));
		}
		return Math.min(STATS_LABEL_W, c1 - w / 2 - 1);
	}

	/** The heading only has to clear the first column's icon. */
	private static int statsHeadingW(Font font, List<StatRow> stats) {
		return PAGE_W - 8 - statsPitch(font, stats) - 8 - 2;
	}

	private static int statRowH(Font font, StatRow row, int labelW) {
		return Math.max(1, font.split(row.label(), labelW).size()) * 9 + 1;
	}

	private static int statsH(Font font, List<StatRow> stats) {
		if (stats.isEmpty()) {
			return 0;
		}
		int labelW = statsLabelW(font, stats);
		int h = 2 + STATS_HEAD_H + 1;
		for (StatRow row : stats) {
			h += statRowH(font, row, labelW);
		}
		return h;
	}

	private static Component recipeHeader(Entry entry, RecipeGrid grid) {
		if (!entry.covers(grid.result())) {
			return Component.translatable("book.dinosaurs.heading.used_in");
		}
		if (grid.smithing()) {
			return Component.translatable("book.dinosaurs.heading.smithing");
		}
		return Component.translatable(grid.shaped() ? "book.dinosaurs.heading.crafting" : "book.dinosaurs.heading.shapeless");
	}

	// ---- 3D picture

	private Mob preview(EntityType<? extends Mob> type) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return null;
		}
		return this.previews.computeIfAbsent(type, t -> {
			Mob mob = type.create(mc.level, EntitySpawnReason.LOAD);
			if (mob != null) {
				// Never added to the level; a negative id keeps it clear of real entities.
				mob.setId(-1000 - ModEntities.ALL.indexOf(type));
			}
			return mob;
		});
	}

	private void drawPicture(GuiGraphicsExtractor graphics, EntityType<? extends Mob> type, int x, int y, int w, int h, int mouseX, int mouseY) {
		// Frame, sky gradient and a strip of grass (or water for the Megalodon).
		graphics.fill(x, y, x + w, y + h, FRAME_DARK);
		boolean water = type == ModEntities.MEGALODON;
		graphics.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, water ? 0xFF6FA8C8 : PIC_SKY_TOP, water ? 0xFF2F6286 : PIC_SKY_BOTTOM);
		if (!water) {
			graphics.fill(x + 1, y + h - 9, x + w - 1, y + h - 1, PIC_GROUND);
			graphics.fill(x + 1, y + h - 9, x + w - 1, y + h - 8, 0xFF6E8A40);
		}
		this.picX0 = x;
		this.picY0 = y;
		this.picX1 = x + w;
		this.picY1 = y + h;
		this.hotspots.add(new Hotspot(x, y, w, h, null, ItemStack.EMPTY, Component.translatable("book.dinosaurs.drag_to_rotate")));
		Mob mob = this.preview(type);
		if (mob == null) {
			return;
		}
		renderEntity(graphics, mob, GuideBookContents.framing(type), this.yaw, x + 1, y + 1, x + w - 1, y + h - 1);
	}

	private static void renderEntity(GuiGraphicsExtractor graphics, LivingEntity entity, Framing framing, float yaw, int x0, int y0, int x1, int y1) {
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
		EntityRenderState state = renderer.createRenderState(entity, 1.0F);
		state.shadowPieces.clear();
		state.outlineColor = 0;
		Quaternionf camera = new Quaternionf().rotateX(-12.0F * (float) (Math.PI / 180.0));
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(camera);
		if (state instanceof LivingEntityRenderState living) {
			living.bodyRot = 180.0F + yaw;
			living.yRot = 0.0F;
			living.xRot = 0.0F;
			living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
			living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
			living.scale = 1.0F;
		}
		float ty = state.boundingBoxHeight / 2.0F;
		if (framing.grounded()) {
			// Feet on the grass strip: the origin sits (ground - centre) pixels below the box centre.
			ty = ((y1 - 5) - (y0 + y1) / 2.0F) / framing.size();
		}
		Vector3f translation = new Vector3f(framing.offsetX(), ty, 0.0F);
		graphics.entity(state, framing.size(), translation, rotation, camera, x0, y0, x1, y1);
	}

	@Override
	public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
		if (this.draggingPicture) {
			this.yaw += (float) dx * 2.0F;
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(final MouseButtonEvent event) {
		this.draggingPicture = false;
		return super.mouseReleased(event);
	}

	// ---- widgets

	/** Icon + link text (wrapped to at most two lines, vertically centred on the icon). */
	private int drawLinkRow(GuiGraphicsExtractor graphics, int x, int y, int width, ItemStack icon, Component title, Location target, int mouseX, int mouseY, int height) {
		height = Math.max(height, rowHeight(this.font, title, height));
		boolean hover = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height - 1;
		if (hover) {
			graphics.fill(x - 1, y - 1, x + width, y + height - 1, HOVER_FILL);
		}
		int iconY = y + (height - 2 - 16) / 2;

		graphics.item(icon, x, iconY);
		int textX = x + 18;
		int textW = width - 18;
		Component label = hover
				? title.copy().withStyle(Style.EMPTY.withUnderlined(true).withColor(LINK_HOVER))
				: title.copy().withStyle(Style.EMPTY.withColor(LINK));
		List<FormattedCharSequence> lines = this.font.split(label, textW);
		int shown = Math.min(3, lines.size());
		int textY = iconY + 8 - shown * 9 / 2 + 1;
		for (int i = 0; i < shown; i++) {
			graphics.text(this.font, lines.get(i), textX, textY + i * 9, hover ? LINK_HOVER : LINK, false);
		}
		this.hotspots.add(new Hotspot(x, y, width, height - 1, () -> {
			this.clickSound();
			this.goTo(target, true);
		}, ItemStack.EMPTY, null));
		return height;
	}

	/** List row height: 22 px for one or two lines of link text, 31 px for three. */
	private static int rowHeight(Font font, Component title, int row) {
		return font.split(title, PAGE_W - 18).size() > 2 ? row + 9 : row;
	}

	/** Compact rows (just the 16 px icon) when the roomy ones would need more than the two pages. */
	public static final int LIST_ROW_COMPACT = 18;

	private static int listRow(Font font, Component title, List<Entry> entries) {
		return listPages(font, title, entries, LIST_ROW) <= 2 ? LIST_ROW : LIST_ROW_COMPACT;
	}

	private static int listPages(Font font, Component title, List<Entry> entries, int row) {
		int y = titleH(font, title);
		int page = 0;
		for (Entry entry : entries) {
			int h = rowHeight(font, entry.title(), row);
			if (y + h > PAGE_H) {
				page++;
				y = 0;
			}
			y += h;
		}
		return page + 1;
	}

	private Component fitOneLine(Component text, int width) {
		if (this.font.width(text) <= width) {
			return text;
		}
		String ellipsis = "...";
		int budget = Math.max(0, width - this.font.width(ellipsis));
		return Component.literal(this.font.plainSubstrByWidth(text.getString(), budget) + ellipsis).withStyle(text.getStyle());
	}

	private int drawWrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
		for (FormattedCharSequence line : this.font.split(text, width)) {
			graphics.text(this.font, line, x, y, color, false);
			y += 9;
		}
		return y;
	}

	/** 16px item icon with a bold, wrapped title beside it and a rule underneath; returns the height used. */
	private int drawIconTitle(GuiGraphicsExtractor graphics, int x, int y, ItemStack icon, Component title) {
		List<FormattedCharSequence> lines = this.font.split(bold(title), ICON_TITLE_W);
		int textH = lines.size() * 9;
		int h = Math.max(16, textH);
		graphics.item(icon, x, y + (h - 16) / 2);
		this.hotspots.add(new Hotspot(x, y + (h - 16) / 2, 16, 16, null, icon, null));
		int yy = y + (h - textH) / 2 + 1;
		int lineW = 0;
		for (FormattedCharSequence line : lines) {
			graphics.text(this.font, line, x + 18, yy, INK, false);
			lineW = Math.max(lineW, this.font.width(line));
			yy += 9;
		}
		graphics.fill(x, y + h + 2, x + Math.min(PAGE_W, 18 + lineW), y + h + 3, FRAME);
		return iconTitleH(this.font, title);
	}

	private static int iconTitleH(Font font, Component title) {
		return Math.max(16, font.split(bold(title), ICON_TITLE_W).size() * 9) + 6;
	}

	private int drawTitle(GuiGraphicsExtractor graphics, int x, int y, int width, Component title) {
		Component bold = title.copy().withStyle(ChatFormatting.BOLD);
		List<FormattedCharSequence> lines = this.font.split(bold, width);
		int yy = y;
		int lineW = 24;
		for (FormattedCharSequence line : lines) {
			graphics.text(this.font, line, x, yy, INK, false);
			lineW = Math.max(lineW, this.font.width(line));
			yy += 9;
		}
		lineW = Math.min(width, lineW);
		graphics.fill(x, yy + 1, x + lineW, yy + 2, FRAME);
		return yy + 4 - y;
	}

	private int drawRecipe(GuiGraphicsExtractor graphics, int x, int y, RecipeGrid grid, Component header, int mouseX, int mouseY) {
		int gridY = header == null ? y : this.drawWrapped(graphics, header.copy().withStyle(ChatFormatting.BOLD), x, y, PAGE_W, INK) + 3;
		long tick = System.currentTimeMillis() / 1000L;
		if (grid.smithing()) {
			// Smithing table row: template, base, addition, arrow, result (102 px).
			for (int i = 0; i < 3; i++) {
				List<ItemStack> options = grid.slots().get(i);
				ItemStack stack = options.isEmpty() ? ItemStack.EMPTY : options.get((int) (tick % options.size()));
				this.drawSlot(graphics, x + i * SLOT, gridY, stack, false, mouseX, mouseY);
			}
			int arrowX = x + 3 * SLOT + 4;
			graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, arrowX, gridY + (SLOT - ARROW_H) / 2, ARROW_U, ARROW_V, ARROW_W, ARROW_H, TEX, TEX);
			this.drawSlot(graphics, arrowX + ARROW_W + 4, gridY, grid.result(), true, mouseX, mouseY);
			return gridY + SLOT - y;
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				List<ItemStack> options = grid.slots().get(row * 3 + col);
				ItemStack stack = options.isEmpty() ? ItemStack.EMPTY : options.get((int) (tick % options.size()));
				this.drawSlot(graphics, x + col * SLOT, gridY + row * SLOT, stack, false, mouseX, mouseY);
			}
		}
		int arrowX = x + 3 * SLOT + 4;
		int arrowY = gridY + SLOT + (SLOT - ARROW_H) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, arrowX, arrowY, ARROW_U, ARROW_V, ARROW_W, ARROW_H, TEX, TEX);
		this.drawSlot(graphics, arrowX + ARROW_W + 4, gridY + SLOT, grid.result(), true, mouseX, mouseY);
		return gridY + RECIPE_H - y;
	}

	/** Item slot with tooltip; clicking it opens the item's (or species') entry when the book has one. */
	private void drawSlot(GuiGraphicsExtractor graphics, int x, int y, ItemStack stack, boolean result, int mouseX, int mouseY) {
		boolean hover = !stack.isEmpty() && mouseX >= x && mouseY >= y && mouseX < x + SLOT && mouseY < y + SLOT;
		int u = result ? RESULT_U : (hover ? SLOT_HOVER_U : SLOT_U);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, WIDGET_V, SLOT, SLOT, TEX, TEX);
		if (stack.isEmpty()) {
			return;
		}
		graphics.item(stack, x + 1, y + 1);
		if (stack.getCount() > 1) {
			String amount = String.valueOf(stack.getCount());
			graphics.text(this.font, amount, x + SLOT - 3 - this.font.width(amount), y + SLOT - 2 - this.font.lineHeight, 0xFFFFFFFF, true);
		}
		Entry target = this.contents.entryFor(stack);
		Entry here = this.location.entryId() == null ? null : this.contents.entry(this.location.entryId());
		Runnable action = target == null || target == here ? null : () -> {
			this.clickSound();
			this.goTo(Location.entry(target.id()), true);
		};
		this.hotspots.add(new Hotspot(x, y, SLOT, SLOT, action, stack, null));
	}

	private void drawNavIcons(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int backX = this.leftPos + 102;
		int homeX = this.leftPos + 138;
		int iconY = this.topPos + 174;
		boolean backHover = mouseX >= backX && mouseY >= iconY && mouseX < backX + ICON && mouseY < iconY + ICON;
		boolean homeHover = mouseX >= homeX && mouseY >= iconY && mouseX < homeX + ICON && mouseY < iconY + ICON;
		if (backHover && !this.history.isEmpty()) {
			graphics.fill(backX - 1, iconY - 1, backX + ICON + 1, iconY + ICON + 1, HOVER_FILL);
		}
		if (homeHover) {
			graphics.fill(homeX - 1, iconY - 1, homeX + ICON + 1, iconY + ICON + 1, HOVER_FILL);
		}
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, backX, iconY, BACK_U, WIDGET_V, ICON, ICON, TEX, TEX);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, homeX, iconY, HOME_U, WIDGET_V, ICON, ICON, TEX, TEX);
		if (this.history.isEmpty()) {
			graphics.fill(backX, iconY, backX + ICON, iconY + ICON, 0x66805030);
		}
		this.hotspots.add(new Hotspot(backX, iconY, ICON, ICON, () -> {
			if (!this.history.isEmpty()) {
				this.goBack();
			}
		}, ItemStack.EMPTY, Component.translatable("book.dinosaurs.nav.back")));
		this.hotspots.add(new Hotspot(homeX, iconY, ICON, ICON, () -> {
			this.clickSound();
			this.goTo(Location.index(), true);
		}, ItemStack.EMPTY, Component.translatable("book.dinosaurs.nav.index")));
	}

	private void applyHoverTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Hotspot hit = null;
		for (Hotspot hotspot : this.hotspots) {
			if (hotspot.contains(mouseX, mouseY)) {
				hit = hotspot;
			}
		}
		if (hit == null) {
			return;
		}
		if (hit.tooltipItem != null && !hit.tooltipItem.isEmpty()) {
			graphics.setTooltipForNextFrame(this.font, hit.tooltipItem, mouseX, mouseY);
		} else if (hit.tooltipText != null) {
			graphics.setTooltipForNextFrame(this.font, hit.tooltipText, mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		if (event.button() == 0) {
			if (this.picX1 > this.picX0 && event.x() >= this.picX0 && event.x() < this.picX1 && event.y() >= this.picY0 && event.y() < this.picY1) {
				this.draggingPicture = true;
				return true;
			}
			for (int i = this.hotspots.size() - 1; i >= 0; i--) {
				Hotspot hotspot = this.hotspots.get(i);
				if (hotspot.action != null && hotspot.contains(event.x(), event.y())) {
					hotspot.action.run();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(final KeyEvent event) {
		if (super.keyPressed(event)) {
			return true;
		}
		if (event.isLeft()) {
			this.turnPage(-1);
			return true;
		}
		if (event.isRight()) {
			this.turnPage(1);
			return true;
		}
		return false;
	}

	// ---- layout check used by the client game test

	/**
	 * Fails the client game test if any translated text is too long for its page or column,
	 * measuring every page the same way it is drawn.
	 */
	public static void assertContentsFit(Font font, String lang) {
		GuideBookContents contents = GuideBookContents.create();
		StringBuilder errors = new StringBuilder();
		// Index: title + intro + recipe on the left; contents + hint on the right.
		int left = titleH(font, Component.translatable("book.dinosaurs.title_heading"))
				+ textH(font, Component.translatable("book.dinosaurs.intro")) + 6
				+ textH(font, bold(Component.translatable("book.dinosaurs.heading.book_recipe"))) + 3 + RECIPE_H;
		fit(errors, lang, "index.left", left);
		int right = titleH(font, Component.translatable("book.dinosaurs.heading.index")) + 22 * contents.categories().size() + 4
				+ textH(font, Component.translatable("book.dinosaurs.index_hint"));
		fit(errors, lang, "index.right", right);
		// Settings.
		DinoConfig config = DinoConfig.get();
		left = titleH(font, Component.translatable("book.dinosaurs.category.settings"))
				+ textH(font, Component.translatable("book.dinosaurs.settings.daylight")) + 2
				+ Math.max(textH(font, status(true)), textH(font, status(false))) + 6
				+ textH(font, Component.translatable("book.dinosaurs.settings.how"));
		fit(errors, lang, "settings.left", left);
		for (Entry entry : contents.dinoEntries()) {
			width(font, errors, lang, "settings." + entry.id(), entry.title(), 1, PAGE_W);
		}
		fit(errors, lang, "settings.right", titleH(font, Component.translatable("book.dinosaurs.settings.spawning"))
				+ textH(font, Component.translatable("book.dinosaurs.settings.legend")) + 3 + 10 * contents.dinoEntries().size());
		for (String key : new String[]{"heading.crafting", "heading.shapeless", "heading.used_in", "heading.drops", "heading.book_recipe", "heading.smithing"}) {
			width(font, errors, lang, key, bold(Component.translatable("book.dinosaurs." + key)), 1);
		}
		listFits(font, errors, lang, "list.dinosaurs", Component.translatable("book.dinosaurs.category.dinosaurs"), contents.dinoEntries());
		listFits(font, errors, lang, "list.items", Component.translatable("book.dinosaurs.category.items"), contents.itemEntries());
		for (Category category : contents.categories()) {
			width(font, errors, lang, "category." + category.id(), category.title(), 2, PAGE_W - 18);
		}
		for (Entry entry : contents.dinoEntries()) {
			width(font, errors, lang, "list." + entry.id(), entry.title(), 3, PAGE_W - 18);
			int l = titleH(font, entry.title()) + PIC_H + 4 + textH(font, entry.body());
			fit(errors, lang, entry.id() + ".left", l);
			int r = titleH(font, Component.translatable("book.dinosaurs.heading.habitat")) + textH(font, entry.habitat()) + 6
					+ textH(font, bold(Component.translatable("book.dinosaurs.heading.drops"))) + 2 + SLOT;
			fit(errors, lang, entry.id() + ".right", r);
			widest(font, errors, lang, entry.id(), entry.body());
			widest(font, errors, lang, entry.id() + ".habitat", entry.habitat());
		}
		for (Entry entry : contents.itemEntries()) {
			width(font, errors, lang, "list." + entry.id(), entry.title(), 3, PAGE_W - 18);
			int titleLines = font.split(bold(entry.title()), ICON_TITLE_W).size();
			boolean statsPage = !entry.stats().isEmpty();
			int l = iconTitleH(font, entry.title()) + (statsPage ? 0 : textH(font, entry.body()));
			if (!entry.droppedBy().isEmpty()) {
				l += 5 + textH(font, Component.translatable("book.dinosaurs.heading.dropped_by")) + 1 + droppedByH(entry.droppedBy().size());
			}
			l += statsH(font, entry.stats());
			int labelW = statsLabelW(font, entry.stats());
			for (StatRow row : entry.stats()) {
				width(font, errors, lang, entry.id() + ".stats", row.label(), 2, labelW);
				for (String word : row.label().getString().split(" ")) {
					if (font.width(word) > labelW) {
						errors.append(' ').append(entry.id()).append(".stats word wider than ").append(labelW).append("px: ").append(word).append('\n');
					}
				}
				width(font, errors, lang, entry.id() + ".stats.value", Component.literal(row.value()), 1, STATS_VALUE_MAX_W);
				width(font, errors, lang, entry.id() + ".stats.netherite", Component.literal(row.netherite()), 1, STATS_VALUE_MAX_W);
			}
			if (!entry.stats().isEmpty()) {
				width(font, errors, lang, entry.id() + ".stats.heading", bold(Component.translatable("book.dinosaurs.stats.heading")), 1, statsHeadingW(font, entry.stats()));
			}
			fit(errors, lang, entry.id() + ".left", l);
			if (titleLines > 4) {
				errors.append(' ').append(entry.id()).append(" title has ").append(titleLines).append(" lines\n");
			}
			fit(errors, lang, entry.id() + ".right", recipesH(font, entry) + (statsPage ? 6 + textH(font, entry.body()) : 0));
			widest(font, errors, lang, entry.id(), entry.body());
		}
		if (!errors.isEmpty()) {
			throw new AssertionError("Guide book text overflows in " + lang + ":\n" + errors);
		}
	}

	/** Both pages of a chapter list must hold all its entries. */
	private static void listFits(Font font, StringBuilder errors, String lang, String name, Component title, List<Entry> entries) {
		int row = listRow(font, title, entries);
		int pages = listPages(font, title, entries, row);
		System.out.println("[guide-book] " + lang + " " + name + " row=" + row + " pages=" + pages + "/2");
		if (pages > 2) {
			errors.append(' ').append(name).append(" needs more than two pages\n");
		}
	}

	private static Component bold(Component c) {
		return c.copy().withStyle(ChatFormatting.BOLD);
	}

	private static int titleH(Font font, Component title) {
		return font.split(bold(title), PAGE_W).size() * 9 + 4;
	}

	private static int textH(Font font, Component text) {
		return font.split(text, PAGE_W).size() * 9;
	}

	private static void fit(StringBuilder errors, String lang, String name, int height) {
		System.out.println("[guide-book] " + lang + " " + name + " height=" + height + "/" + PAGE_H);
		if (height > PAGE_H) {
			errors.append(' ').append(name).append(" is ").append(height).append("px tall (max ").append(PAGE_H).append(")\n");
		}
	}

	private static void width(Font font, StringBuilder errors, String lang, String name, Component text, int maxLines) {
		width(font, errors, lang, name, text, maxLines, PAGE_W);
	}

	private static void width(Font font, StringBuilder errors, String lang, String name, Component text, int maxLines, int w) {
		if (font.split(text, w).size() > maxLines) {
			errors.append(' ').append(name).append(" does not fit on ").append(maxLines).append(" line(s) of ").append(w).append("px: ")
					.append(text.getString()).append('\n');
		}
	}

	private static void widest(Font font, StringBuilder errors, String lang, String name, Component text) {
		for (FormattedCharSequence line : font.split(text, PAGE_W)) {
			if (font.width(line) > PAGE_W) {
				errors.append(' ').append(name).append(" has a line wider than the page\n");
			}
		}
	}
}
