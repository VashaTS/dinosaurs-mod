package net.vsh33.dinosaurs.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Giant prehistoric shark. Lives in deep oceans and attacks players that are in the water. */
public class Megalodon extends WaterAnimal implements AttackAnimated {
	private int attackTicks;

	public Megalodon(EntityType<? extends Megalodon> type, Level level) {
		super(type, level);
		this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 0.02F, 0.1F, true);
		this.lookControl = new SmoothSwimmingLookControl(this, 10);
		this.xpReward = 15;
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WaterBoundPathNavigation(this, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 40));
		this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
				(target, level) -> target.isInWater()));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.attackTicks > 0) {
			this.attackTicks--;
		}
		LivingEntity target = this.getTarget();
		if (!this.level().isClientSide() && target != null && !target.isInWater()) {
			// Only dangerous in the water: lose interest once the target climbs out.
			this.setTarget(null);
		}
	}

	@Override
	protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
		this.moveRelative(this.getSpeed(), input);
		this.move(MoverType.SELF, this.getDeltaMovement());
		this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
		if (this.getTarget() == null) {
			this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			level.broadcastEntityEvent(this, ATTACK_EVENT);
		}
		return hit;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == ATTACK_EVENT) {
			this.attackTicks = 10;
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public int dinosaurs$getAttackTicks() {
		return attackTicks;
	}

	@Override
	public int getMaxHeadXRot() {
		return 1;
	}

	@Override
	public int getMaxHeadYRot() {
		return 1;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.DOLPHIN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.DOLPHIN_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.5F + this.random.nextFloat() * 0.1F;
	}
}
