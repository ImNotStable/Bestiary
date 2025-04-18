package me.jeremiah.bestiary.hooks;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.AuraSkillsBukkit;
import lombok.Getter;
import me.jeremiah.bestiary.BestiaryPlatform;

public class AuraSkillsHook {

  private static AuraSkillsHook hook;

  public static void load(BestiaryPlatform platform) {
    hook = new AuraSkillsHook(platform);
  }

  public static AuraSkillsHook get() {
    return hook;
  }

  private final BestiaryPlatform platform;
  @Getter
  private final AuraSkillsApi api;
  @Getter
  private final AuraSkillsBukkit bukkitApi;

  private AuraSkillsHook(BestiaryPlatform platform) {
    this.platform = platform;
    this.api = AuraSkillsApi.get();
    this.bukkitApi = AuraSkillsBukkit.get();
  }

}
