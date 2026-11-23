package net.vsh33.dinosaurs.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.vsh33.dinosaurs.Dinosaurs;

/**
 * A shaped crafting recipe (same JSON as {@code minecraft:crafting_shaped} plus {@code source}, type
 * {@code dinosaurs:crafting_shaped_keep_components}) whose result keeps the data of the {@code source} ingredient's item:
 * enchantments, custom name, repair cost, lore and every other component change. Damage is carried over proportionally
 * to the new max durability (rounded up, so crafting never repairs). Used to upgrade an enchanted bow into the Dinosaur Bone Bow.
 */
public class KeepComponentsShapedRecipe extends ShapedRecipe {
	public static final MapCodec<KeepComponentsShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(
					Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
					CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
					ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
					Ingredient.CODEC_NONEMPTY.fieldOf("source").forGetter(o -> o.source),
					ItemStack.STRICT_CODEC.fieldOf("result").forGetter(o -> o.result))
					.apply(i, KeepComponentsShapedRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, KeepComponentsShapedRecipe> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, ShapedRecipe::getGroup,
			CraftingBookCategory.STREAM_CODEC, ShapedRecipe::category,
			ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
			Ingredient.CONTENTS_STREAM_CODEC, o -> o.source,
			ItemStack.STREAM_CODEC, o -> o.result,
			KeepComponentsShapedRecipe::new);
	public static final RecipeSerializer<KeepComponentsShapedRecipe> SERIALIZER = new RecipeSerializer<>() {
		@Override
		public MapCodec<KeepComponentsShapedRecipe> codec() {
			return MAP_CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, KeepComponentsShapedRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	};

	private final ShapedRecipePattern pattern;
	private final Ingredient source;
	private final ItemStack result;

	public KeepComponentsShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, Ingredient source, ItemStack result) {
		super(group, category, pattern, result);
		this.pattern = pattern;
		this.source = source;
		this.result = result;
	}

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Dinosaurs.id("crafting_shaped_keep_components"), SERIALIZER);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider provider) {
		ItemStack plain = this.result.copy();
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
}
