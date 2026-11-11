package net.vsh33.dinosaurs.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Herbivorous dinosaur. Neutral species (Triceratops, Stegosaurus, Ankylosaurus) fight back when hurt;
 * passive species (Brachiosaurus, Parasaurolophus) run away from attackers and from carnivores.
 */
public class HerbivoreDino extends PathfinderMob implements AttackAnimated {
	private int attackTicks;

	public HerbivoreDino(EntityType<? extends HerbivoreDino> type, Level level) {
		super(type, level);
		this.xpReward = profile().xp();
	}

	protected DinoProfile profile() {
		return ModEntities.profile(this.getType());
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		if (profile().neutral()) {
			this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25, true));
			this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		} else {
			this.goalSelector.addGoal(1, new PanicGoal(this, 1.8));
			this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, HostileDino.class, 14.0F, 1.0, 1.6));
		}
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		// Like vanilla animals, herbivores stay in the world.
		return false;
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
	public void aiStep() {
		super.aiStep();
		if (this.attackTicks > 0) {
			this.attackTicks--;
		}
	}

	@Override
	public int dinosaurs$getAttackTicks() {
		return attackTicks;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return profile().ambient();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return profile().hurt();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return profile().death();
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		if (profile().step() != null) {
			this.playSound(profile().step(), 0.3F * profile().volume(), profile().pitch());
		} else {
			super.playStepSound(pos, state);
		}
	}

	@Override
	public float getVoicePitch() {
		return profile().pitch() * (0.9F + this.random.nextFloat() * 0.2F);
	}

	@Override
	protected float getSoundVolume() {
		return profile().volume();
	}
}
