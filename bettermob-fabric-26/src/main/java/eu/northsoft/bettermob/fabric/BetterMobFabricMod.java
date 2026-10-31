package eu.northsoft.bettermob.fabric;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import eu.northsoft.bettermob.drop.DropTable;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.skill.core.CoreSkillEngine;
import eu.northsoft.bettermob.util.ReloadDiff;
import eu.northsoft.bettermob.util.ReloadReport;
import eu.northsoft.bettermob.util.ReloadReportFormatter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public final class BetterMobFabricMod implements ModInitializer {
    public static final String MOD_ID = "bettermob";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static BetterMobFabricMod instance;
    private final FabricPlatform platform = new FabricPlatform();
    private final FabricPackScanner packScanner = new FabricPackScanner(platform.dataFolder());
    private final FabricItemRegistry itemRegistry = new FabricItemRegistry(packScanner);
    private final FabricDropRegistry dropRegistry = new FabricDropRegistry(packScanner, itemRegistry);
    private final FabricMobRegistry mobRegistry = new FabricMobRegistry(packScanner);
    private final CoreSkillEngine skillEngine = new CoreSkillEngine();
    private final FabricMobManager mobManager = new FabricMobManager(this, skillEngine);
    private final FabricSpawnerManager spawnerManager = new FabricSpawnerManager(this, platform.dataFolder().resolve("spawners.yml"));

    @Override
    public void onInitialize() {
        instance = this;
        this.itemRegistry.load();
        this.dropRegistry.load();
        this.mobRegistry.load();
        this.spawnerManager.load();
        registerMechanics();
        registerCommands();
        this.mobManager.registerEvents();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(platform::setServer);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> platform.setServer(null));
        LOGGER.info("BetterMob (Fabric) initialized for Minecraft {}", platform.minecraftVersion());
    }

    private void registerMechanics() {
        skillEngine.setWarningHandler(LOGGER::warn);

        skillEngine.registerMechanic("model", (ctx, target, params) -> {
            String modelId = params.getOrDefault("mid", params.get("m"));
            if (modelId != null && target.entity() != null && platform.model("bettermodel").isPresent()) {
                platform.model("bettermodel").get().attach(target.entity(), modelId);
            }
        });

        skillEngine.registerMechanic("damage", (ctx, target, params) -> {
            if (target.entity() instanceof eu.northsoft.bettermob.platform.PlatformLivingEntity living) {
                double amount = eu.northsoft.bettermob.skill.Params.parseNumber(params.getOrDefault("amount", params.get("a")), 1.0);
                living.damage(amount, ctx.caster());
            }
        });

        skillEngine.registerMechanic("heal", (ctx, target, params) -> {
            if (target.entity() instanceof eu.northsoft.bettermob.platform.PlatformLivingEntity living) {
                double amount = eu.northsoft.bettermob.skill.Params.parseNumber(params.getOrDefault("amount", params.get("a")), 1.0);
                double current = living.health();
                double max = living.maxHealth();
                if (current < max && amount > 0) {
                    double newHealth = Math.min(max, current + amount);
                    living.health(newHealth);
                    if (living.handle() instanceof LivingEntity nmsLiving) {
                        mobManager.onHeal(nmsLiving, (float) (newHealth - current));
                    }
                }
            }
        });

        skillEngine.registerMechanic("loot", (ctx, target, params) -> {
            String tableId = params.getOrDefault("table", params.get("droptable"));
            if (tableId == null) return;
            DropTable table = dropRegistry.get(tableId);
            if (table == null) return;
            var result = dropRegistry.roll(table);
            Position pos = target.position();
            if (pos == null && ctx.caster() != null) pos = ctx.caster().position();
            if (pos != null && ctx.caster() != null && ctx.caster().world() != null) {
                for (var drop : result.items()) {
                    ctx.caster().world().dropItem(pos, drop.itemKey(), drop.amount());
                }
                if (result.exp() > 0) {
                    ctx.caster().world().dropExperience(pos, result.exp());
                }
            }
        });
    }

    private LiteralArgumentBuilder<CommandSourceStack> createCommandRoot(String name) {
        return Commands.literal(name)
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("reload")
                .executes(context -> handleReload(context.getSource(), false))
                .then(Commands.literal("verbose")
                    .executes(context -> handleReload(context.getSource(), true))))
            .then(Commands.literal("packs")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    List<String> packs = packScanner.listPacks();
                    if (packs.isEmpty()) {
                        source.sendSuccess(() -> Component.literal("No packs found in " + packScanner.packsFolder().getPath()), false);
                    } else {
                        source.sendSuccess(() -> Component.literal("Loaded packs (" + packs.size() + "):"), false);
                        for (String p : packs) {
                            source.sendSuccess(() -> Component.literal(" - " + p), false);
                        }
                    }
                    return 1;
                }))
            .then(Commands.literal("spawn")
                .then(Commands.argument("mob", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(mobRegistry.all().keySet(), builder))
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        String mobId = StringArgumentType.getString(context, "mob");
                        MobDefinition def = mobRegistry.get(mobId);
                        if (def == null) {
                            source.sendFailure(Component.literal("Unknown BetterMob: " + mobId));
                            return 0;
                        }
                        Vec3 pos = source.getPosition();
                        ServerLevel level = source.getLevel();
                        Position p = new Position(level.dimension().identifier().toString(),
                                pos.x, pos.y, pos.z, source.getEntity() != null ? source.getEntity().getYRot() : 0, 0);
                        var spawned = new FabricWorld(level).spawn(def.type, p);
                        if (spawned.isPresent()) {
                            var entity = spawned.get();
                            if (def.displayName != null && !def.displayName.isBlank()) {
                                entity.customName(def.displayName.replace('&', '§'));
                                entity.customNameVisible(true);
                            }
                            if (entity instanceof eu.northsoft.bettermob.platform.PlatformLivingEntity living) {
                                if (def.health > 0) {
                                    living.maxHealth(def.health);
                                    living.health(def.health);
                                }
                                if (platform.model("bettermodel").isPresent() && def.modelId != null) {
                                    platform.model("bettermodel").get().attach(entity, def.modelId);
                                }
                                mobManager.register(entity.uuid(), def, living);
                            }
                            source.sendSuccess(() -> Component.literal("Spawned BetterMob: " + def.id + " (model: " + def.modelId + ")"), true);
                            return 1;
                        }
                        source.sendFailure(Component.literal("Failed to spawn BetterMob: " + mobId + " (unknown entity type: " + def.type + ")"));
                        return 0;
                    })));
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(createCommandRoot("bettermob"));
            dispatcher.register(createCommandRoot("bm"));
        });
    }

    private int handleReload(CommandSourceStack source, boolean verbose) {
        ReloadReport report = reloadAll();
        for (String line : ReloadReportFormatter.format(report, verbose)) {
            source.sendSuccess(() -> Component.literal(line), true);
        }
        return 1;
    }

    public ReloadReport reloadAll() {
        var beforeMobs = mobRegistry.fingerprints();
        var beforeItems = itemRegistry.fingerprints();
        var beforeDrops = dropRegistry.fingerprints();
        var beforeSpawners = spawnerManager.fingerprints();

        itemRegistry.load();
        dropRegistry.load();
        mobRegistry.load();
        spawnerManager.load();

        Map<String, ReloadDiff> diffs = new java.util.LinkedHashMap<>();
        ReloadDiff mobDiff = ReloadDiff.of(beforeMobs, mobRegistry.fingerprints());
        diffs.put("mobs", mobDiff);
        diffs.put("skills", new ReloadDiff(List.of(), List.of(), List.of()));
        diffs.put("items", ReloadDiff.of(beforeItems, itemRegistry.fingerprints()));
        diffs.put("droptables", ReloadDiff.of(beforeDrops, dropRegistry.fingerprints()));
        diffs.put("spawners", ReloadDiff.of(beforeSpawners, spawnerManager.fingerprints()));

        int affected = mobManager.livingOf(new java.util.HashSet<>(mobDiff.changed()));
        int updated = mobManager.reloadLiving(mobRegistry);

        return new ReloadReport(updated, diffs, affected);
    }

    public static BetterMobFabricMod instance() {
        return instance;
    }

    public FabricPlatform platform() {
        return platform;
    }

    public FabricPackScanner packScanner() {
        return packScanner;
    }

    public FabricItemRegistry itemRegistry() {
        return itemRegistry;
    }

    public FabricDropRegistry dropRegistry() {
        return dropRegistry;
    }

    public FabricMobRegistry mobRegistry() {
        return mobRegistry;
    }

    public FabricMobManager mobManager() {
        return mobManager;
    }

    public FabricSpawnerManager spawnerManager() {
        return spawnerManager;
    }
}
