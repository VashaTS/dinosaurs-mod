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
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Giant prehistoric shark. Lives in deep oceans and attacks players that are in the water. */
public class Megalodon extends WaterAnimal implements AttackAnimated {
	/**
	 * 1.7.1: no random despawn while a player is this close. Vanilla lets a water creature vanish at random (1 in 800 per
	 * tick after 30 s without a player within 32 blocks), and the 14-block shark is easy to see from much further away than
	 * that, so it could vanish right in front of a player swimming towards it (with the amulet it does not swim at the
	 * player, so it stays "idle"). Beyond 128 blocks it still despawns at once, like every water creature.
	 */
	public static final double NO_RANDOM_DESPAWN_DISTANCE = 96.0;

	private int attackTicks;
	private final DinoProtection protection = new DinoProtection();

	public Megalodon(EntityType<? extends Megalodon> type, Level level) {
		super(type, level);
		this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
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
				target -> target.isInWater()));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.attackTicks > 0) {
			this.attackTicks--;
		}
		if (!this.level().isClientSide()) {
			protection.tick(this);
		}
		LivingEntity target = this.getTarget();
		if (!this.level().isClientSide() && target != null && !target.isInWater()) {
			// Only dangerous in the water: lose interest once the target climbs out.
			this.setTarget(null);
		}
	}

	@Override
	public void travel(Vec3 input) {
		if (this.isControlledByLocalInstance() && this.isInWater()) {
			this.moveRelative(this.getSpeed(), input);
			this.move(MoverType.SELF, this.getDeltaMovement());
			this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
			if (this.getTarget() == null) {
				this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
			}
		} else {
			super.travel(input);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return distSqr > NO_RANDOM_DESPAWN_DISTANCE * NO_RANDOM_DESPAWN_DISTANCE && super.removeWhenFarAway(distSqr);
	}

	@Override
	public boolean hurt(DamageSource source, float damage) {
		if (!this.level().isClientSide()) {
			protection.onHurt(source);
		}
		return super.hurt(source, damage);
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		// Amulet of Dino Protection: leave its wearer alone unless they attacked this dinosaur first.
		return super.canAttack(target) && !protection.isShielded(target);
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		boolean hit = super.doHurtTarget(target);
		if (hit) {
			this.level().broadcastEntityEvent(this, ATTACK_EVENT);
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
	protected boolean shouldDespawnInPeaceful() {
		return true;
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

	@Override
	protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) {
		return ModEntities.eyeHeight(this.getType(), dimensions.height * 0.85F);
	}
}
