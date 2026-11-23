package net.vsh33.dinosaurs.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.vsh33.dinosaurs.Dinosaurs;

/**
 * A shaped crafting recipe (same JSON as {@code minecraft:crafting_shaped} plus {@code source}, type
 * {@code dinosaurs:crafting_shaped_keep_components}) whose result keeps the NBT of the {@code source} ingredient's item:
 * enchantments, custom name, repair cost, lore and the rest. Damage is carried over proportionally to the new max
 * durability (rounded up, so crafting never repairs). Used to upgrade an enchanted bow into the Dinosaur Bone Bow.
 */
public class KeepComponentsShapedRecipe extends ShapedRecipe {
	public static final RecipeSerializer<KeepComponentsShapedRecipe> SERIALIZER = new RecipeSerializer<>() {
		@Override
		public KeepComponentsShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
			ShapedRecipe shaped = RecipeSerializer.SHAPED_RECIPE.fromJson(id, json);
			return new KeepComponentsShapedRecipe(shaped, Ingredient.fromJson(json.get("source"), false));
		}

		@Override
		public KeepComponentsShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
			ShapedRecipe shaped = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf);
			return new KeepComponentsShapedRecipe(shaped, Ingredient.fromNetwork(buf));
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, KeepComponentsShapedRecipe recipe) {
			RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
			recipe.source.toNetwork(buf);
		}
	};

	private final Ingredient source;
	private final ItemStack result;

	public KeepComponentsShapedRecipe(ShapedRecipe shaped, Ingredient source) {
		super(shaped.getId(), shaped.getGroup(), shaped.category(), shaped.getWidth(), shaped.getHeight(), shaped.getIngredients(),
				shaped.getResultItem(RegistryAccess.EMPTY));
		this.source = source;
		this.result = shaped.getResultItem(RegistryAccess.EMPTY);
	}

	public static void register() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Dinosaurs.id("crafting_shaped_keep_components"), SERIALIZER);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public ItemStack assemble(CraftingContainer input, RegistryAccess access) {
		ItemStack plain = this.result.copy();
		for (int i = 0; i < input.getContainerSize(); i++) {
			ItemStack stack = input.getItem(i);
			if (!stack.isEmpty() && this.source.test(stack)) {
				return keepComponents(stack, plain);
			}
		}
		return plain;
	}

	/** {@code plain} with the NBT of {@code from}; damage scaled to the new max durability. */
	public static ItemStack keepComponents(ItemStack from, ItemStack plain) {
		ItemStack out = plain.copy();
		if (from.getTag() != null) {
			out.setTag(from.getTag().copy());
		}
		out.setDamageValue(0);
		if (out.isDamageableItem() && from.isDamageableItem() && from.getDamageValue() > 0) {
			int newMax = out.getMaxDamage();
			int scaled = (int) Math.ceil(from.getDamageValue() * (double) newMax / from.getMaxDamage());
			out.setDamageValue(Math.min(scaled, newMax - 1));
		}
		return out;
	}
}
