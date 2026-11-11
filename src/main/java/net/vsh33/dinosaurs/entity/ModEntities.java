package net.vsh33.dinosaurs.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.vsh33.dinosaurs.Dinosaurs;
import net.vsh33.dinosaurs.config.DinoConfig;

public final class ModEntities {
	public static final List<EntityType<? extends Mob>> ALL = new ArrayList<>();
	private static final Map<EntityType<?>, DinoProfile> PROFILES = new HashMap<>();

	// ---- Carnivores (hostile)
	private static final DinoProfile T_REX_PROFILE = new DinoProfile(SoundEvents.RAVAGER_ROAR, SoundEvents.RAVAGER_HURT, SoundEvents.RAVAGER_DEATH, SoundEvents.RAVAGER_STEP, 0.7F, 2.0F, false, false, 25);
	private static final DinoProfile RAPTOR_PROFILE = new DinoProfile(SoundEvents.FOX_SCREECH, SoundEvents.FOX_HURT, SoundEvents.FOX_DEATH, null, 0.8F, 1.0F, false, true, 7);
	private static final DinoProfile SPINO_PROFILE = new DinoProfile(SoundEvents.RAVAGER_AMBIENT, SoundEvents.RAVAGER_HURT, SoundEvents.RAVAGER_DEATH, SoundEvents.RAVAGER_STEP, 0.85F, 1.8F, false, false, 20);
	// ---- Herbivores
	private static final DinoProfile TRIKE_PROFILE = new DinoProfile(SoundEvents.HOGLIN_AMBIENT, SoundEvents.HOGLIN_HURT, SoundEvents.HOGLIN_DEATH, null, 0.7F, 1.4F, true, false, 8);
	private static final DinoProfile STEGO_PROFILE = new DinoProfile(SoundEvents.SNIFFER_IDLE, SoundEvents.SNIFFER_HURT, SoundEvents.SNIFFER_DEATH, SoundEvents.SNIFFER_STEP, 0.8F, 1.4F, true, false, 8);
	private static final DinoProfile ANKY_PROFILE = new DinoProfile(SoundEvents.ARMADILLO_AMBIENT, SoundEvents.ARMADILLO_HURT, SoundEvents.ARMADILLO_DEATH, null, 0.5F, 1.4F, true, false, 8);
	private static final DinoProfile BRACHIO_PROFILE = new DinoProfile(SoundEvents.CAMEL_AMBIENT, SoundEvents.CAMEL_HURT, SoundEvents.CAMEL_DEATH, SoundEvents.RAVAGER_STEP, 0.5F, 2.0F, false, false, 10);
	private static final DinoProfile PARA_PROFILE = new DinoProfile(SoundEvents.GOAT_AMBIENT, SoundEvents.GOAT_HURT, SoundEvents.GOAT_DEATH, null, 0.6F, 1.3F, false, false, 5);

	public static final EntityType<HostileDino> T_REX = register("t_rex",
			EntityType.Builder.<HostileDino>of(HostileDino::new, MobCategory.MONSTER)
					.sized(1.8F, 3.4F).eyeHeight(3.0F).clientTrackingRange(10).notInPeaceful());
	public static final EntityType<HostileDino> VELOCIRAPTOR = register("velociraptor",
			EntityType.Builder.<HostileDino>of(HostileDino::new, MobCategory.MONSTER)
					.sized(0.7F, 1.0F).eyeHeight(0.85F).clientTrackingRange(8).notInPeaceful());
	public static final EntityType<HostileDino> SPINOSAURUS = register("spinosaurus",
			EntityType.Builder.<HostileDino>of(HostileDino::new, MobCategory.MONSTER)
					.sized(1.8F, 3.2F).eyeHeight(2.6F).clientTrackingRange(10).notInPeaceful());
	public static final EntityType<Triceratops> TRICERATOPS = register("triceratops",
			EntityType.Builder.<Triceratops>of(Triceratops::new, MobCategory.CREATURE)
					.sized(1.7F, 2.1F).eyeHeight(1.5F).passengerAttachments(1.75F).clientTrackingRange(10));
	public static final EntityType<HerbivoreDino> STEGOSAURUS = register("stegosaurus",
			EntityType.Builder.<HerbivoreDino>of(HerbivoreDino::new, MobCategory.CREATURE)
					.sized(1.6F, 2.2F).eyeHeight(1.2F).clientTrackingRange(10));
	public static final EntityType<HerbivoreDino> ANKYLOSAURUS = register("ankylosaurus",
			EntityType.Builder.<HerbivoreDino>of(HerbivoreDino::new, MobCategory.CREATURE)
					.sized(1.7F, 1.3F).eyeHeight(0.9F).clientTrackingRange(10));
	public static final EntityType<HerbivoreDino> BRACHIOSAURUS = register("brachiosaurus",
			EntityType.Builder.<HerbivoreDino>of(HerbivoreDino::new, MobCategory.CREATURE)
					.sized(2.0F, 3.0F).eyeHeight(2.9F).clientTrackingRange(12));
	public static final EntityType<HerbivoreDino> PARASAUROLOPHUS = register("parasaurolophus",
			EntityType.Builder.<HerbivoreDino>of(HerbivoreDino::new, MobCategory.CREATURE)
					.sized(1.2F, 2.4F).eyeHeight(2.0F).clientTrackingRange(10));
	public static final EntityType<Megalodon> MEGALODON = register("megalodon",
			EntityType.Builder.<Megalodon>of(Megalodon::new, MobCategory.WATER_CREATURE)
					.sized(2.5F, 1.8F).eyeHeight(1.0F).clientTrackingRange(10).notInPeaceful());
	public static final EntityType<Pteranodon> PTERANODON = register("pteranodon",
			EntityType.Builder.<Pteranodon>of(Pteranodon::new, MobCategory.CREATURE)
					.sized(1.0F, 0.9F).eyeHeight(0.6F).clientTrackingRange(10));

	private static <T extends Mob> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Dinosaurs.id(name));
		EntityType<T> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
		ALL.add(type);
		return type;
	}

	private static AttributeSupplier.Builder attrs(double health, double speed, double damage, double armor, double knockbackRes, double follow) {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, health)
				.add(Attributes.MOVEMENT_SPEED, speed)
				.add(Attributes.ATTACK_DAMAGE, damage)
				.add(Attributes.ARMOR, armor)
				.add(Attributes.KNOCKBACK_RESISTANCE, knockbackRes)
				.add(Attributes.FOLLOW_RANGE, follow);
	}

	public static DinoProfile profile(EntityType<?> type) {
		return PROFILES.get(type);
	}

	public static void initialize() {
		PROFILES.putAll(Map.of(T_REX, T_REX_PROFILE, VELOCIRAPTOR, RAPTOR_PROFILE, SPINOSAURUS, SPINO_PROFILE, TRICERATOPS, TRIKE_PROFILE, STEGOSAURUS, STEGO_PROFILE, ANKYLOSAURUS, ANKY_PROFILE, BRACHIOSAURUS, BRACHIO_PROFILE, PARASAUROLOPHUS, PARA_PROFILE));
		// Attributes
		FabricDefaultAttributeRegistry.register(T_REX, attrs(90, 0.3, 14, 6, 0.9, 32).add(Attributes.STEP_HEIGHT, 1.1).add(Attributes.ATTACK_KNOCKBACK, 1.0));
		FabricDefaultAttributeRegistry.register(VELOCIRAPTOR, attrs(16, 0.38, 4, 0, 0.0, 24));
		FabricDefaultAttributeRegistry.register(SPINOSAURUS, attrs(75, 0.28, 11, 4, 0.8, 28).add(Attributes.STEP_HEIGHT, 1.1));
		FabricDefaultAttributeRegistry.register(TRICERATOPS, attrs(50, 0.24, 9, 6, 0.7, 16).add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.ATTACK_KNOCKBACK, 1.5));
		FabricDefaultAttributeRegistry.register(STEGOSAURUS, attrs(45, 0.22, 8, 4, 0.6, 16).add(Attributes.STEP_HEIGHT, 1.0));
		FabricDefaultAttributeRegistry.register(ANKYLOSAURUS, attrs(55, 0.2, 8, 14, 0.8, 16).add(Attributes.ATTACK_KNOCKBACK, 1.2));
		FabricDefaultAttributeRegistry.register(BRACHIOSAURUS, attrs(110, 0.2, 0, 4, 1.0, 16).add(Attributes.STEP_HEIGHT, 1.1));
		FabricDefaultAttributeRegistry.register(PARASAUROLOPHUS, attrs(30, 0.27, 0, 0, 0.3, 16).add(Attributes.STEP_HEIGHT, 1.0));
		FabricDefaultAttributeRegistry.register(MEGALODON, Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 100).add(Attributes.MOVEMENT_SPEED, 1.3).add(Attributes.ATTACK_DAMAGE, 12)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FOLLOW_RANGE, 32));
		FabricDefaultAttributeRegistry.register(PTERANODON, Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 16).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FLYING_SPEED, 0.6));

		// Spawn placement rules (every rule first checks the config: per-species toggle and the daylight option)
		placement(T_REX, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, true, (t, l, r, p, rand) -> checkPredatorSpawn(t, l, r, p, rand, 1, 4));
		placement(VELOCIRAPTOR, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, true, (t, l, r, p, rand) -> checkPredatorSpawn(t, l, r, p, rand, 6, 2));
		placement(SPINOSAURUS, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, true, (t, l, r, p, rand) -> checkPredatorSpawn(t, l, r, p, rand, 1, 4));
		placement(TRICERATOPS, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, false, ModEntities::checkHerbivoreSpawn);
		placement(STEGOSAURUS, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, false, ModEntities::checkHerbivoreSpawn);
		placement(ANKYLOSAURUS, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, false, ModEntities::checkHerbivoreSpawn);
		placement(BRACHIOSAURUS, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, false, ModEntities::checkHerbivoreSpawn);
		placement(PARASAUROLOPHUS, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, false, ModEntities::checkHerbivoreSpawn);
		placement(MEGALODON, SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, true, ModEntities::checkMegalodonSpawn);
		placement(PTERANODON, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING, false, ModEntities::checkPteranodonSpawn);

		// Biomes: every species has its own home so they are spread over the world.
		addSpawn(MobCategory.MONSTER, T_REX, 4, 1, 1, Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST, Biomes.DARK_FOREST);
		addSpawn(MobCategory.MONSTER, VELOCIRAPTOR, 14, 2, 4, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA, Biomes.BADLANDS, Biomes.WOODED_BADLANDS, Biomes.ERODED_BADLANDS);
		addSpawn(MobCategory.MONSTER, SPINOSAURUS, 6, 1, 1, Biomes.MANGROVE_SWAMP, Biomes.BEACH);
		addSpawn(MobCategory.CREATURE, TRICERATOPS, 6, 2, 3, Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW);
		addSpawn(MobCategory.CREATURE, STEGOSAURUS, 6, 1, 3, Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST);
		addSpawn(MobCategory.CREATURE, ANKYLOSAURUS, 5, 1, 2, Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_GRAVELLY_HILLS);
		addSpawn(MobCategory.CREATURE, BRACHIOSAURUS, 5, 1, 2, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE);
		addSpawn(MobCategory.CREATURE, PARASAUROLOPHUS, 8, 2, 4, Biomes.SWAMP, Biomes.RIVER);
		addSpawn(MobCategory.WATER_CREATURE, MEGALODON, 1, 1, 1, Biomes.DEEP_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.DEEP_FROZEN_OCEAN);
		addSpawn(MobCategory.CREATURE, PTERANODON, 8, 1, 3, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.STONY_PEAKS, Biomes.JAGGED_PEAKS);
	}

	@SafeVarargs
	private static void addSpawn(MobCategory category, EntityType<?> type, int weight, int min, int max, ResourceKey<Biome>... biomes) {
		BiomeModifications.addSpawn(BiomeSelectors.includeByKey(biomes), category, type, weight, min, max);
	}

	/** Hostile species (T. rex, Velociraptor, Spinosaurus, Megalodon) - used by the "no hostile spawns in daylight" option. */
	public static boolean isHostile(EntityType<?> type) {
		return type == T_REX || type == VELOCIRAPTOR || type == SPINOSAURUS || type == MEGALODON;
	}

	private static <T extends Mob> void placement(EntityType<T> type, SpawnPlacementType placementType, Heightmap.Types heightmap, boolean hostile, SpawnPlacements.SpawnPredicate<T> rule) {
		SpawnPlacements.register(type, placementType, heightmap,
				(t, level, reason, pos, random) -> configAllows(t, level, reason, pos, hostile) && rule.test(t, level, reason, pos, random));
	}

	/**
	 * Config checks for natural spawning (world generation and the regular mob spawner).
	 * Spawn eggs, spawners and /summon are never blocked.
	 */
	private static boolean configAllows(EntityType<?> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, boolean hostile) {
		if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
			return true;
		}
		DinoConfig config = DinoConfig.get();
		if (!config.isSpawnEnabled(type)) {
			return false;
		}
		if (hostile && !config.hostileSpawnInDaylight) {
			// Only at night and only where it is dark (like vanilla monsters: light level 7 or less).
			return level.getLevel().isDarkOutside() && level.getMaxLocalRawBrightness(pos) <= 7;
		}
		return true;
	}

	private static int countNearby(ServerLevelAccessor level, EntityType<?> type, BlockPos pos, double radius) {
		return level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(radius), e -> e.getType() == type).size();
	}

	/** Predators hunt in daylight too, but are rare and limited per area so they never flood a biome. */
	private static <T extends Mob> boolean checkPredatorSpawn(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random, int maxNearby, int chance) {
		if (level.getDifficulty() == Difficulty.PEACEFUL || !Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		if (EntitySpawnReason.isSpawner(reason)) {
			return true;
		}
		return level.canSeeSky(pos) && random.nextInt(chance) == 0 && countNearby(level, type, pos, 64) < maxNearby;
	}

	private static <T extends Mob> boolean checkHerbivoreSpawn(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		boolean bright = EntitySpawnReason.ignoresLightRequirements(reason) || level.getRawBrightness(pos, 0) > 8;
		return bright && level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON);
	}

	private static <T extends Mob> boolean checkMegalodonSpawn(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			return false;
		}
		if (!level.getFluidState(pos).is(FluidTags.WATER) || !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
			return false;
		}
		if (EntitySpawnReason.isSpawner(reason)) {
			return true;
		}
		return pos.getY() <= level.getSeaLevel() - 10 && random.nextInt(3) == 0 && countNearby(level, type, pos, 96) < 1;
	}

	private static <T extends Mob> boolean checkPteranodonSpawn(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		boolean bright = EntitySpawnReason.ignoresLightRequirements(reason) || level.getRawBrightness(pos, 0) > 8;
		return bright && level.canSeeSky(pos) && Mob.checkMobSpawnRules(type, level, reason, pos, random)
				&& (EntitySpawnReason.isSpawner(reason) || countNearby(level, type, pos, 48) < 6);
	}

	private ModEntities() {
	}
}
