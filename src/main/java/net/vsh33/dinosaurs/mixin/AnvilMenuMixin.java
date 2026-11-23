package net.vsh33.dinosaurs.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.entity.player.Inventory;
import net.vsh33.dinosaurs.item.BoneBowEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks the anvil's left item so Infinity + Mending may be combined on the Dinosaur Bone Bow. */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
	private AnvilMenuMixin(MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(type, containerId, inventory, access);
	}

	@Inject(method = "createResult", at = @At("HEAD"))
	private void dinosaurs$beginBoneBow(CallbackInfo ci) {
		BoneBowEnchantments.begin(this.inputSlots.getItem(0));
	}

	@Inject(method = "createResult", at = @At("RETURN"))
	private void dinosaurs$endBoneBow(CallbackInfo ci) {
		BoneBowEnchantments.end();
	}
}
