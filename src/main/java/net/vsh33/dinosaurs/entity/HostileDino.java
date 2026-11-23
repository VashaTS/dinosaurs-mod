package net.vsh33.dinosaurs.entity;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.GameRules;
import java.util.UUID;
import net.vsh33.dinosaurs.item.ModItems;

/**
 * Carnivorous dinosaur: hostile to players (T. rex, Velociraptor, Spinosaurus).
 * <p>
 * Dino Collar (1.7.0, T. rex only): a player carrying the Amulet of Dino Protection uses a Dino Collar on a T. rex to
 * become its owner. A collared T. rex
 * <ul>
 *     <li>follows its owner like a wolf (and teleports to them when more than 12 blocks away);</li>
 *     <li>attacks what the owner attacks and whatever attacks the owner, and never the owner;</li>
 *     <li>ignores every other player unless they attack its owner (or the T. rex itself);</li>
 *     <li>never despawns (also not on Peaceful), does not stop players from sleeping;</li>
 *     <li>owner: sneak + use with an empty hand to make it sit / follow again; feed it meat to heal it.</li>
 * </ul>
 * The owner's UUID is saved in the entity data ("Owner"); the collar is dropped when the T. rex dies.
 */
public class HostileDino extends Monster implements AttackAnimated, OwnableEntity {
	private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
			SynchedEntityData.defineId(HostileDino.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Boolean> DATA_SITTING = SynchedEntityData.defineId(HostileDino.class, EntityDataSerializers.BOOLEAN);
	/** Teleport to the owner from this far away (blocks, squared), like wolves. */
	private static final double TELEPORT_DISTANCE_SQR = 12.0 * 12.0;

	private int attackTicks;
	private final DinoProtection protection = new DinoProtection();

	public HostileDino(EntityType<? extends HostileDino> type, Level level) {
		super(type, level);
		this.setMaxUpStep(ModEntities.stepHeight(type));
		this.xpReward = profile().xp();
		if (type == ModEntities.SPINOSAURUS) {
			// Spinosaurus was semi-aquatic: it is happy to walk through water.
			this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
		}
	}

	protected DinoProfile profile() {
		return ModEntities.profile(this.getType());
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_OWNER, Optional.empty());
		this.entityData.define(DATA_SITTING, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new SitGoal());
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
		this.goalSelector.addGoal(4, new FollowOwnerGoal(1.1, 10.0F, 4.0F));
		if (this.getType() == ModEntities.SPINOSAURUS) {
			this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
		} else {
			this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		}
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal());
		this.targetSelector.addGoal(2, new OwnerHurtTargetGoal());
		HurtByTargetGoal hurtBy = new HurtByTargetGoal(this);
		if (profile().packHunter()) {
			hurtBy.setAlertOthers();
		}
		this.targetSelector.addGoal(3, hurtBy);
		// Wild carnivores hunt players; a collared T. rex leaves other players alone.
		this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
				target -> this.huntsPlayers()));
	}

	// ---- Dino Collar: ownership

	/** Wild carnivores look for players to attack; a collared T. rex does not (only its owner's enemies). */
	public boolean huntsPlayers() {
		return !this.isCollared();
	}

	/** Only the T. rex can wear a Dino Collar. */
	public boolean canBeCollared() {
		return this.getType() == ModEntities.T_REX;
	}

	public boolean isCollared() {
		return this.entityData.get(DATA_OWNER).isPresent();
	}

	/** Last resolved owner (like 26.x EntityReference): kept while it is alive and still the owner. */
	private LivingEntity cachedOwner;

	@Override
	public LivingEntity getOwner() {
		UUID id = this.getOwnerUUID();
		if (id == null) return null;
		if (this.cachedOwner != null && !this.cachedOwner.isRemoved() && id.equals(this.cachedOwner.getUUID())) return this.cachedOwner;
		LivingEntity found = this.level().getPlayerByUUID(id);
		if (found == null && this.level() instanceof ServerLevel server && server.getEntity(id) instanceof LivingEntity living) found = living;
		this.cachedOwner = found;
		return found;
	}

	@Override
	public UUID getOwnerUUID() {
		return this.entityData.get(DATA_OWNER).orElse(null);
	}

	public void setOwnerUUID(UUID owner) {
		this.entityData.set(DATA_OWNER, Optional.ofNullable(owner));
	}

	/** True if this entity is the collared dinosaur's owner (works while the owner is offline too: compares UUIDs). */
	public boolean isOwnedBy(Entity entity) {
		UUID owner = this.getOwnerUUID();
		return owner != null && entity != null && owner.equals(entity.getUUID());
	}

	public boolean isOrderedToSit() {
		return this.entityData.get(DATA_SITTING);
	}

	public void setOrderedToSit(boolean sitting) {
		this.entityData.set(DATA_SITTING, sitting);
	}

	/**
	 * Puts the collar on: the player becomes the owner, the dinosaur stops fighting them, forgets its current target and
	 * becomes persistent. Server side; the caller checks the amulet and consumes the collar.
	 */
	public void collar(Player owner) {
		this.setOwnerUUID(owner.getUUID());
		this.cachedOwner = owner;
		this.setOrderedToSit(false);
		this.setTarget(null);
		this.setLastHurtByMob(null);
		this.getNavigation().stop();
		this.setPersistenceRequired();
	}

	@Override
	public void addAdditionalSaveData(CompoundTag output) {
		super.addAdditionalSaveData(output);
		if (this.getOwnerUUID() != null) {
			output.putUUID("Owner", this.getOwnerUUID());
		}
		output.putBoolean("Sitting", this.isOrderedToSit());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag input) {
		super.readAdditionalSaveData(input);
		UUID owner = input.hasUUID("Owner") ? input.getUUID("Owner") : null;
		this.setOwnerUUID(owner);
		this.setOrderedToSit(owner != null && input.getBoolean("Sitting"));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!this.canBeCollared() || !this.isAlive()) {
			return super.mobInteract(player, hand);
		}
		if (stack.is(ModItems.DINO_COLLAR)) {
			if (this.isCollared()) {
				if (!this.level().isClientSide()) {
					player.displayClientMessage(Component.translatable(this.isOwnedBy(player)
							? "message.dinosaurs.dino_collar.already_yours" : "message.dinosaurs.dino_collar.has_owner").withStyle(ChatFormatting.YELLOW), true);
				}
				return InteractionResult.FAIL;
			}
			if (!DinoProtection.hasAmulet(player)) {
				if (!this.level().isClientSide()) {
					player.displayClientMessage(Component.translatable("message.dinosaurs.dino_collar.need_amulet").withStyle(ChatFormatting.RED), true);
				}
				return InteractionResult.FAIL;
			}
			if (!this.level().isClientSide()) {
				this.collar(player);
				if (!player.getAbilities().instabuild) stack.shrink(1);
				this.level().playSound(null, this, SoundEvents.ARMOR_EQUIP_LEATHER, this.getSoundSource(), 1.0F, 0.8F);
				this.level().playSound(null, this, SoundEvents.CHAIN_PLACE, this.getSoundSource(), 1.0F, 1.0F);
				((ServerLevel) this.level()).sendParticles(ParticleTypes.HEART, this.getX(), this.getEyeY(), this.getZ(), 7, 0.8, 0.5, 0.8, 0.02);
				player.displayClientMessage(Component.translatable("message.dinosaurs.dino_collar.collared").withStyle(ChatFormatting.GREEN), true);
			}
			return InteractionResult.SUCCESS;
		}
		if (this.isOwnedBy(player)) {
			if ((stack.getItem().isEdible() && stack.getItem().getFoodProperties().isMeat()) && this.getHealth() < this.getMaxHealth()) {
				if (!this.level().isClientSide()) {
					var food = stack.getItem().getFoodProperties();
					this.heal(food != null ? 2.0F * food.getNutrition() : 8.0F);
					if (!player.getAbilities().instabuild) stack.shrink(1);
					this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.8F);
				}
				return InteractionResult.SUCCESS;
			}
			if (stack.isEmpty() && player.isSecondaryUseActive()) {
				if (!this.level().isClientSide()) {
					boolean sit = !this.isOrderedToSit();
					this.setOrderedToSit(sit);
					this.getNavigation().stop();
					this.setTarget(null);
					player.displayClientMessage(Component.translatable(sit ? "message.dinosaurs.dino_collar.sit" : "message.dinosaurs.dino_collar.follow"), true);
				}
				return InteractionResult.SUCCESS;
			}
		}
		return super.mobInteract(player, hand);
	}

	/** Same rules as wolves: not creepers or armor stands, not the owner's other pets, not players the owner may not hurt. */
	public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
		if (target instanceof Creeper || target instanceof ArmorStand || target == owner) {
			return false;
		}
		if (target instanceof OwnableEntity pet && pet.getOwnerUUID() != null && pet.getOwnerUUID().equals(owner.getUUID())) {
			return false;
		}
		return !(target instanceof Player targetPlayer && owner instanceof Player ownerPlayer && !ownerPlayer.canHarmPlayer(targetPlayer));
	}

	@Override
	public boolean isAlliedTo(Entity other) {
		if (this.isCollared()) {
			if (this.isOwnedBy(other)) {
				return true;
			}
			if (other instanceof OwnableEntity pet && pet.getOwnerUUID() != null && this.isOwnedBy(this.level().getPlayerByUUID(pet.getOwnerUUID()))) {
				return true;
			}
		}
		return super.isAlliedTo(other);
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return !this.isCollared() && super.removeWhenFarAway(distSqr);
	}

	@Override
	public boolean requiresCustomPersistence() {
		return this.isCollared() || super.requiresCustomPersistence();
	}

	@Override
	public void checkDespawn() {
		if (this.isCollared()) {
			// Never despawns, not even on Peaceful (where wild carnivores vanish).
			this.noActionTime = 0;
			return;
		}
		super.checkDespawn();
	}

	@Override
	public boolean isPreventingPlayerRest(Player player) {
		return !this.isCollared() && super.isPreventingPlayerRest(player);
	}

	@Override
	public void die(DamageSource source) {
		if (this.level() instanceof ServerLevel && this.isCollared() && this.getOwner() instanceof ServerPlayer owner
				&& ((ServerLevel) this.level()).getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) {
			owner.sendSystemMessage(this.getCombatTracker().getDeathMessage());
		}
		super.die(source);
	}

	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean killedByPlayer) {
		super.dropCustomDeathLoot(source, looting, killedByPlayer);
		if (this.isCollared()) {
			this.spawnAtLocation(new ItemStack(ModItems.DINO_COLLAR));
		}
	}

	// ---- combat

	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader level) {
		// Unlike vanilla monsters, dinosaurs do not prefer darkness.
		return 0.0F;
	}

	@Override
	public boolean hurt(DamageSource source, float damage) {
		if (this.level().isClientSide()) {
			return super.hurt(source, damage);
		}
		protection.onHurt(source);
		boolean hurt = super.hurt(source, damage);
		if (hurt && this.isOrderedToSit()) {
			this.setOrderedToSit(false); // stands up to defend itself, like a sitting wolf
		}
		return hurt;
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		if (this.isCollared()) {
			// Never the owner or the owner's other pets. The amulet does not shield other players from a collared T. rex
			// (it only targets players who attack its owner or itself).
			if (this.isOwnedBy(target)) {
				return false;
			}
			if (target instanceof OwnableEntity pet && pet.getOwnerUUID() != null && this.isOwnedBy(this.level().getPlayerByUUID(pet.getOwnerUUID()))) {
				return false;
			}
			return super.canAttack(target);
		}
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
	public void aiStep() {
		super.aiStep();
		if (this.attackTicks > 0) {
			this.attackTicks--;
		}
		if (!this.level().isClientSide()) {
			if (this.isCollared()) {
				LivingEntity target = this.getTarget();
				if (target != null && (this.isOwnedBy(target) || this.isOrderedToSit())) {
					this.setTarget(null);
				}
			} else {
				protection.tick(this);
			}
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

	// ---- companion goals (the vanilla ones need a TamableAnimal)

	/** Following the owner is possible: collared, standing, not riding, owner in this level and not a spectator. */
	private boolean canMoveToOwner(LivingEntity owner) {
		return owner != null && !this.isOrderedToSit() && !this.isPassenger() && !owner.isSpectator();
	}

	/** Teleports next to the owner (a free spot 2-4 blocks away the T. rex fits in), like a wolf left behind. */
	public boolean tryToTeleportToOwner() {
		LivingEntity owner = this.getOwner();
		if (owner == null) {
			return false;
		}
		BlockPos center = owner.blockPosition();
		for (int attempt = 0; attempt < 20; attempt++) {
			int dx = this.random.nextIntBetweenInclusive(-4, 4);
			int dz = this.random.nextIntBetweenInclusive(-4, 4);
			if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
				continue;
			}
			BlockPos pos = center.offset(dx, this.random.nextIntBetweenInclusive(-1, 1), dz);
			if (this.canTeleportTo(pos)) {
				this.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, this.getYRot(), this.getXRot());
				this.getNavigation().stop();
				return true;
			}
		}
		return false;
	}

	private boolean canTeleportTo(BlockPos pos) {
		if (WalkNodeEvaluator.getBlockPathTypeStatic(this.level(), pos.mutable()) != BlockPathTypes.WALKABLE) {
			return false;
		}
		if (this.level().getBlockState(pos.below()).getBlock() instanceof LeavesBlock) {
			return false;
		}
		BlockPos delta = pos.subtract(this.blockPosition());
		return this.level().noCollision(this, this.getBoundingBox().move(delta));
	}

	/** Stays put while ordered to sit (blocks walking, following and attacking). */
	private final class SitGoal extends Goal {
		SitGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return isCollared() && isOrderedToSit() && !isInWater() && onGround();
		}

		@Override
		public boolean canContinueToUse() {
			return isCollared() && isOrderedToSit();
		}

		@Override
		public void start() {
			getNavigation().stop();
		}
	}

	/** Like vanilla FollowOwnerGoal: walks to the owner when more than startDistance away, teleports beyond 12 blocks. */
	private final class FollowOwnerGoal extends Goal {
		private final double speed;
		private final float startDistance;
		private final float stopDistance;
		private LivingEntity owner;
		private int timeToRecalcPath;
		private float oldWaterCost;

		FollowOwnerGoal(double speed, float startDistance, float stopDistance) {
			this.speed = speed;
			this.startDistance = startDistance;
			this.stopDistance = stopDistance;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!isCollared()) {
				return false;
			}
			LivingEntity owner = getOwner();
			if (!canMoveToOwner(owner) || distanceToSqr(owner) < this.startDistance * this.startDistance) {
				return false;
			}
			this.owner = owner;
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			if (this.owner == null || !this.owner.isAlive() || !canMoveToOwner(this.owner)) {
				return false;
			}
			return !getNavigation().isDone() && distanceToSqr(this.owner) > this.stopDistance * this.stopDistance;
		}

		@Override
		public void start() {
			this.timeToRecalcPath = 0;
			this.oldWaterCost = getPathfindingMalus(BlockPathTypes.WATER);
			setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
		}

		@Override
		public void stop() {
			this.owner = null;
			getNavigation().stop();
			setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
		}

		@Override
		public void tick() {
			if (this.owner == null) {
				return;
			}
			boolean farAway = distanceToSqr(this.owner) >= TELEPORT_DISTANCE_SQR;
			if (!farAway) {
				getLookControl().setLookAt(this.owner, 10.0F, getMaxHeadXRot());
			}
			if (--this.timeToRecalcPath <= 0) {
				this.timeToRecalcPath = this.adjustedTickDelay(10);
				if (farAway) {
					tryToTeleportToOwner();
				} else {
					getNavigation().moveTo(this.owner, this.speed);
				}
			}
		}
	}

	/** Attacks whatever hurt the owner (vanilla OwnerHurtByTargetGoal). */
	private final class OwnerHurtByTargetGoal extends TargetGoal {
		private LivingEntity ownerLastHurtBy;
		private int timestamp;

		OwnerHurtByTargetGoal() {
			super(HostileDino.this, false);
			this.setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			if (!isCollared() || isOrderedToSit()) {
				return false;
			}
			LivingEntity owner = getOwner();
			if (owner == null) {
				return false;
			}
			this.ownerLastHurtBy = owner.getLastHurtByMob();
			return owner.getLastHurtByMobTimestamp() != this.timestamp && this.ownerLastHurtBy != null
					&& this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT) && wantsToAttack(this.ownerLastHurtBy, owner);
		}

		@Override
		public void start() {
			this.mob.setTarget(this.ownerLastHurtBy);
			LivingEntity owner = getOwner();
			if (owner != null) {
				this.timestamp = owner.getLastHurtByMobTimestamp();
			}
			super.start();
		}
	}

	/** Attacks whatever the owner attacks (vanilla OwnerHurtTargetGoal). */
	private final class OwnerHurtTargetGoal extends TargetGoal {
		private LivingEntity ownerLastHurt;
		private int timestamp;

		OwnerHurtTargetGoal() {
			super(HostileDino.this, false);
			this.setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			if (!isCollared() || isOrderedToSit()) {
				return false;
			}
			LivingEntity owner = getOwner();
			if (owner == null) {
				return false;
			}
			this.ownerLastHurt = owner.getLastHurtMob();
			return owner.getLastHurtMobTimestamp() != this.timestamp && this.ownerLastHurt != null
					&& this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT) && wantsToAttack(this.ownerLastHurt, owner);
		}

		@Override
		public void start() {
			this.mob.setTarget(this.ownerLastHurt);
			LivingEntity owner = getOwner();
			if (owner != null) {
				this.timestamp = owner.getLastHurtMobTimestamp();
			}
			super.start();
		}
	}

	@Override
	protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) {
		return ModEntities.eyeHeight(this.getType(), dimensions.height * 0.85F);
	}
}
