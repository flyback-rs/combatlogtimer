package com.curvelabs.combatlogout;

import java.awt.*;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("combatlogout")
public interface CombatLogoutConfig extends Config {
  @ConfigItem(
    keyName = "pvpOnly",
    name = "PvP Only",
    description = "Only activate the plugin in PvP (Wilderness/PvP worlds)",
    position = 1
  )
  default boolean pvpOnly() {
    return true;
  }

  public enum TimeFormat {
    TICKS("Game ticks"),
    SECONDS("Seconds"),
    MILLISECONDS("Milliseconds");

    private final String description;

    TimeFormat(String description) {
      this.description = description;
    }

    @Override
    public String toString() {
      return description;
    }
  }

  @ConfigItem(
    keyName = "showOverlay",
    name = "Show Overlay",
    description = "Show the timer overlay",
    position = 2
  )
  default boolean showOverlay() {
    return true;
  }

  @ConfigItem(
    keyName = "showHeadBar",
    name = "Show Head Bar",
    description = "Show a timer bar above your character",
    position = 3
  )
  default boolean showHeadBar() {
    return true;
  }

  @ConfigItem(
    keyName = "gracePeriod",
    name = "Grace Period",
    description = "Seconds to continue showing timer after the timer has expired",
    position = 4
  )
  default int gracePeriod() {
    return 3;
  }

  @ConfigItem(
    keyName = "timeFormat",
    name = "Time Format",
    description = "How to display the remaining time",
    position = 5
  )
  default TimeFormat timeFormat() {
    return TimeFormat.SECONDS;
  }

  @ConfigItem(
    keyName = "soundAlert",
    name = "Sound Alert",
    description = "Play sound when safe to logout",
    position = 6
  )
  default boolean soundAlert() {
    return false;
  }

  @ConfigItem(
    keyName = "dangerColor",
    name = "Danger Color",
    description = "Color that the timer starts with",
    position = 7
  )
  default Color dangerColor() {
    return new Color(255, 0, 0);
  }

  @ConfigItem(
    keyName = "safeColor",
    name = "Safe Color",
    description = "Color that the timer shifts to as it decays",
    position = 8
  )
  default Color safeColor() {
    return new Color(0, 255, 0);
  }
}
