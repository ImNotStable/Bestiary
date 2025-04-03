package me.jeremiah.bestiary;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import me.jeremiah.bestiary.configuration.Config;
import me.jeremiah.bestiary.configuration.DatabaseConfig;
import me.jeremiah.bestiary.data.BestiaryPlayer;
import me.jeremiah.bestiary.data.configuration.BestiaryCategory;
import me.jeremiah.bestiary.data.configuration.BestiaryEntry;
import me.jeremiah.bestiary.data.configuration.DatabaseInfo;
import me.jeremiah.bestiary.data.storage.AbstractDatabase;
import me.jeremiah.bestiary.data.storage.H2;
import me.jeremiah.bestiary.hooks.PlaceholderAPIHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.io.Closeable;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class BestiaryPlatform implements Listener, Closeable {

  @Getter
  private final BestiaryPlugin plugin;

  private final Config config;

  private final DatabaseConfig dbConfig;
  @Getter
  private DatabaseInfo databaseInfo;
  private AbstractDatabase database;

  @Getter
  private BestiaryCategory mainCategory;

  @SuppressWarnings("UnstableApiUsage")
  public BestiaryPlatform(BestiaryPlugin plugin) {
    this.plugin = plugin;
    this.config = new Config(plugin);
    this.dbConfig = new DatabaseConfig(plugin);
    plugin.getLifecycleManager().registerEventHandler(
      LifecycleEvents.COMMANDS,
      event -> event.registrar().register("bestiary", new BestiaryCommand(this))
    );
    Bukkit.getPluginManager().registerEvents(this, plugin);
    PlaceholderAPIHook.load(this);
  }

  public void load() {
    config.load();
    dbConfig.load();
    databaseInfo = dbConfig.getDatabaseInfo();
    database = switch (databaseInfo.getDatabaseType()) {
      case H2 -> new H2(this);
      default -> null;
    };

    mainCategory = config.getMainCategory();
  }

  public BestiaryPlayer getPlayer(UUID uniqueId) {
    return database.getBestiaryPlayer(uniqueId);
  }

  public BestiaryCategory getCategory(String id) {
    return config.getCategoriesMap().get(id);
  }

  public BestiaryEntry getEntry(String id) {
    return config.getEntryMap().get(id);
  }

  // TODO Performance could be improved by using a TreeMap where entries are sorted by total kills in reverse order
  public CompletableFuture<Optional<BestiaryEntry>> findApplicableEntry(LivingEntity entity) {
    return CompletableFuture.supplyAsync(() -> {
      for (BestiaryEntry entry : config.getEntryMap().values())
        if (entry.matchesFilter(entity)) {
          return Optional.of(entry);
        }
      return Optional.empty();
    });
  }

  @SuppressWarnings("UnstableApiUsage")
  @EventHandler
  public void onEntityDeath(EntityDeathEvent event) {
    Entity causingEntity = event.getDamageSource().getCausingEntity();
    if (!(causingEntity instanceof Player player))
      return;
    BestiaryPlayer bPlayer = database.getBestiaryPlayer(player);
    findApplicableEntry(event.getEntity()).thenAccept(entry -> entry.ifPresent(bPlayer::incrementEntry));
  }

  @Override
  public void close() {
    database.close();
  }

  public Logger getLogger() {
    return plugin.getLogger();
  }

}
