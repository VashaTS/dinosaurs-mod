package net.vsh33.dinosaurs.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.vsh33.dinosaurs.item.ModItems;
import org.jspecify.annotations.Nullable;

/**
 * Triceratops: a neutral herbivore that can be saddled with a Dinosaur Saddle and ridden.
 * <ul>
 *     <li>use a Dinosaur Saddle on it to saddle it;</li>
 *     <li>use it with an empty hand (not sneaking) to mount, then steer with the movement keys;</li>
 *     <li>sneak + use with an empty hand to take the saddle back off.</li>
 * </ul>
 * The saddle is saved with the entity and dropped when it dies.
 */
public class Triceratops extends HerbivoreDino {
	private static final EntityDataAccessor<Boolean> DATA_SADDLED = SynchedEntityData.defineId(Triceratops.class, EntityDataSerializers.BOOLEAN);

	public Triceratops(EntityType<? extends Triceratops> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_SADDLED, false);
	}

	public boolean isSaddled() {
		return this.entityData.get(DATA_SADDLED);
	}

	public void setSaddled(boolean saddled) {
		this.entityData.set(DATA_SADDLED, saddled);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Saddled", this.isSaddled());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setSaddled(input.getBooleanOr("Saddled", false));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!this.isSaddled() && stack.getItem() == ModItems.DINOSAUR_SADDLE && this.isAlive()) {
			if (!this.level().isClientSide()) {
				this.setSaddled(true);
				this.level().playSound(null, this, SoundEvents.HORSE_SADDLE.value(), this.getSoundSource(), 1.0F, 0.8F);
				stack.consume(1, player);
				if (this.getTarget() == player) {
					this.setTarget(null);
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (this.isSaddled() && !this.isVehicle()) {
			if (player.isSecondaryUseActive()) {
				if (stack.isEmpty()) {
					if (!this.level().isClientSide()) {
						this.setSaddled(false);
						this.level().playSound(null, this, SoundEvents.SADDLE_UNEQUIP.value(), this.getSoundSource(), 1.0F, 0.8F);
						ItemStack saddle = new ItemStack(ModItems.DINOSAUR_SADDLE);
						if (!player.getInventory().add(saddle)) {
							this.spawnAtLocation((ServerLevel) this.level(), saddle);
						}
					}
					return InteractionResult.SUCCESS;
				}
			} else {
				if (!this.level().isClientSide()) {
					if (this.getTarget() == player) {
						this.setTarget(null);
					}
					player.startRiding(this);
				}
				return InteractionResult.SUCCESS;
			}
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		// Never fight the rider.
		if (!this.level().isClientSide() && this.getTarget() != null && this.getTarget() == this.getFirstPassenger()) {
			this.setTarget(null);
		}
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		if (this.isSaddled() && this.getFirstPassenger() instanceof Player player) {
			return player;
		}
		return super.getControllingPassenger();
	}

	@Override
	protected void tickRidden(Player controller, Vec3 riddenInput) {
		super.tickRidden(controller, riddenInput);
		this.setRot(controller.getYRot(), 0.0F);
		this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
	}

	@Override
	protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
		float sideways = controller.xxa * 0.5F;
		float forward = controller.zza;
		if (forward <= 0.0F) {
			forward *= 0.25F; // walking backwards is slow
		}
		return new Vec3(sideways, 0.0, forward);
	}

	@Override
	protected float getRiddenSpeed(Player controller) {
		// Base speed 0.24 -> a little slower than an average horse, faster than walking.
		return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
		super.positionRider(passenger, moveFunction);
		if (passenger instanceof LivingEntity living) {
			living.yBodyRot = this.yBodyRot;
		}
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		if (this.isSaddled()) {
			this.spawnAtLocation(level, new ItemStack(ModItems.DINOSAUR_SADDLE));
			this.setSaddled(false);
		}
	}
}
