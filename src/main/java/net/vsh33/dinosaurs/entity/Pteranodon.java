package net.vsh33.dinosaurs.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Flying reptile of the mountains. Glides around, then picks the highest nearby point
 * (a peak, a cliff top or a tree crown) and perches there for a while. Passive: flees when hurt.
 */
public class Pteranodon extends PathfinderMob {
	public Pteranodon(EntityType<? extends Pteranodon> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl<>(this, 10, false);
		this.xpReward = 3;
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
		nav.setCanOpenDoors(false);
		nav.setCanFloat(true);
		return nav;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
		this.goalSelector.addGoal(3, new PerchOnHighPointGoal(this));
		this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 1.0));
		this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	public boolean isFlying() {
		return !this.onGround();
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.PARROT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PARROT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PARROT_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.55F + this.random.nextFloat() * 0.1F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}

	/** Fly to the highest reachable point nearby and sit there. */
	static class PerchOnHighPointGoal extends Goal {
		private final Pteranodon mob;
		private Vec3 perch;
		private int flyTicks;
		private int perchTicks;

		PerchOnHighPointGoal(Pteranodon mob) {
			this.mob = mob;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			if (mob.isPassenger() || mob.getRandom().nextInt(160) != 0) {
				return false;
			}
			this.perch = findPerch();
			return this.perch != null;
		}

		private Vec3 findPerch() {
			Level level = mob.level();
			BlockPos origin = mob.blockPosition();
			BlockPos best = null;
			for (int dx = -24; dx <= 24; dx += 3) {
				for (int dz = -24; dz <= 24; dz += 3) {
					int x = origin.getX() + dx;
					int z = origin.getZ() + dz;
					int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
					BlockPos top = new BlockPos(x, y, z);
					if (!level.getFluidState(top.below()).isEmpty()) {
						continue;
					}
					if (best == null || y > best.getY()) {
						best = top;
					}
				}
			}
			if (best == null || best.getY() < origin.getY() - 6) {
				return null;
			}
			return Vec3.atBottomCenterOf(best);
		}

		@Override
		public void start() {
			this.flyTicks = 0;
			this.perchTicks = 0;
			mob.getNavigation().moveTo(perch.x, perch.y + 0.5, perch.z, 1.2);
		}

		@Override
		public boolean canContinueToUse() {
			if (perch == null || mob.getLastHurtByMob() != null) {
				return false;
			}
			if (perchTicks > 0) {
				return true;
			}
			return flyTicks < 400;
		}

		@Override
		public void tick() {
			if (perchTicks > 0) {
				perchTicks--;
				if (perchTicks == 0) {
					perch = null;
				}
				return;
			}
			flyTicks++;
			double dist = mob.position().distanceToSqr(perch);
			if (dist < 2.5) {
				mob.getNavigation().stop();
				mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.2, 1.0, 0.2));
				perchTicks = 300 + mob.getRandom().nextInt(600);
			} else if (mob.getNavigation().isDone()) {
				mob.getNavigation().moveTo(perch.x, perch.y + 0.5, perch.z, 1.2);
				if (mob.getNavigation().isDone()) {
					// Path could not be found: fly straight towards it.
					mob.getMoveControl().setWantedPosition(perch.x, perch.y + 0.5, perch.z, 1.2);
				}
			}
		}

		@Override
		public void stop() {
			perch = null;
			perchTicks = 0;
			mob.getNavigation().stop();
		}
	}
}
