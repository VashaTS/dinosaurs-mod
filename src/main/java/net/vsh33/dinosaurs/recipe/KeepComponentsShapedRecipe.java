package net.vsh33.dinosaurs.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.vsh33.dinosaurs.Dinosaurs;

/**
 * A shaped crafting recipe (same JSON as {@code minecraft:crafting_shaped}, type {@code dinosaurs:crafting_shaped_keep_components})
 * whose result keeps the data of the {@code source} ingredient's item: enchantments, custom name, repair cost, lore and
 * every other component change. Damage is carried over proportionally to the new max durability (rounded up, so
 * crafting never repairs). Used to upgrade an enchanted bow into the Dinosaur Bone Bow.
 */
public class KeepComponentsShapedRecipe extends NormalCraftingRecipe {
	public static final MapCodec<KeepComponentsShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(
					Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
					CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
					ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
					Ingredient.CODEC.fieldOf("source").forGetter(o -> o.source),
					ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result))
					.apply(i, KeepComponentsShapedRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, KeepComponentsShapedRecipe> STREAM_CODEC = StreamCodec.composite(
			Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
			CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
			ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
			Ingredient.CONTENTS_STREAM_CODEC, o -> o.source,
			ItemStackTemplate.STREAM_CODEC, o -> o.result,
			KeepComponentsShapedRecipe::new);
	public static final RecipeSerializer<KeepComponentsShapedRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	private final ShapedRecipePattern pattern;
	private final Ingredient source;
	private final ItemStackTemplate result;

	public KeepComponentsShapedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern,
			Ingredient source, ItemStackTemplate result) {
		super(commonInfo, bookInfo);
		this.pattern = pattern;
		this.source = source;
		this.result = result;
	}

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Dinosaurs.id("crafting_shaped_keep_components"), SERIALIZER);
	}

	@Override
	public RecipeSerializer<KeepComponentsShapedRecipe> getSerializer() {
		return SERIALIZER;
	}

	public List<java.util.Optional<Ingredient>> getIngredients() {
		return this.pattern.ingredients();
	}

	@Override
	protected PlacementInfo createPlacementInfo() {
		return PlacementInfo.createFromOptionals(this.pattern.ingredients());
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return this.pattern.matches(input);
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		ItemStack plain = this.result.create();
		for (ItemStack stack : input.items()) {
			if (!stack.isEmpty() && this.source.test(stack)) {
				return keepComponents(stack, plain);
			}
		}
		return plain;
	}

	/** {@code plain} with every component change of {@code from}; damage scaled to the new max durability. */
	public static ItemStack keepComponents(ItemStack from, ItemStack plain) {
		ItemStack out = plain.copy();
		out.applyComponents(from.getComponentsPatch());
		// The source's own max durability must never override the result's (only relevant if it was patched).
		if (plain.has(DataComponents.MAX_DAMAGE)) {
			out.set(DataComponents.MAX_DAMAGE, plain.get(DataComponents.MAX_DAMAGE));
			if (from.isDamageableItem() && from.getDamageValue() > 0) {
				int newMax = plain.getMaxDamage();
				int scaled = (int) Math.ceil(from.getDamageValue() * (double) newMax / from.getMaxDamage());
				out.setDamageValue(Math.min(scaled, newMax - 1));
			}
		}
		return out;
	}

	public int getWidth() {
		return this.pattern.width();
	}

	public int getHeight() {
		return this.pattern.height();
	}

	@Override
	public List<RecipeDisplay> display() {
		return List.of(new ShapedCraftingRecipeDisplay(
				this.pattern.width(),
				this.pattern.height(),
				this.pattern.ingredients().stream().map(e -> e.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE)).toList(),
				new SlotDisplay.ItemStackSlotDisplay(this.result),
				new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
	}
}
