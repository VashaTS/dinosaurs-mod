package net.vsh33.dinosaurs.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;

/** Carnivorous dinosaur: hostile to players (T. rex, Velociraptor, Spinosaurus). */
public class HostileDino extends Monster implements AttackAnimated {
	private int attackTicks;

	public HostileDino(EntityType<? extends HostileDino> type, Level level) {
		super(type, level);
		this.xpReward = profile().xp();
		if (type == ModEntities.SPINOSAURUS) {
			// Spinosaurus was semi-aquatic: it is happy to walk through water.
			this.setPathfindingMalus(PathType.WATER, 0.0F);
		}
	}

	protected DinoProfile profile() {
		return ModEntities.profile(this.getType());
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
		if (this.getType() == ModEntities.SPINOSAURUS) {
			this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
		} else {
			this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		}
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		HurtByTargetGoal hurtBy = new HurtByTargetGoal(this);
		if (profile().packHunter()) {
			hurtBy.setAlertOthers();
		}
		this.targetSelector.addGoal(1, hurtBy);
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader level) {
		// Unlike vanilla monsters, dinosaurs do not prefer darkness.
		return 0.0F;
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
