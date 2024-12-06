package com.curvelabs.combatlogout;

import com.google.inject.Provides;
import java.awt.*;
import java.text.DecimalFormat;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.*;

@Slf4j
@PluginDescriptor(
  name = "Combat Logout Timer",
  description = "Shows countdown until you can safely logout after combat",
  tags = { "combat", "logout", "timer", "pvp", "pking" }
)
public class CombatLogoutPlugin extends Plugin {

  private static final int COMBAT_TIMEOUT_TICKS = 16;
  private static final String CONFIG_GROUP = "combatlogout";

  @Inject
  private Client client;

  @Inject
  private EventBus eventBus;

  @Inject
  private CombatLogoutConfig config;

  @Inject
  private OverlayManager overlayManager;

  @Inject
  private CombatLogoutOverlay overlay;

  @Inject
  private CombatLogoutOverheadBar headBarOverlay;

  @Getter
  private int logoutTicks = 0;

  @Getter
  private int graceTicks = 0;

  private boolean subscribed = false;
  private IsInWildy inWilderness = IsInWildy.NO_IS_NOT_IN_WILDERNESS;
  private boolean isPvp = false;

  @Override
  protected void startUp() {
    log.info("Combat Logout Timer starting up");
    initializeDisplays();
    updateSubscriptionState();
  }

  @Override
  protected void shutDown() {
    log.info("Combat Logout Timer shutting down");
    overlayManager.remove(overlay);
    overlayManager.remove(headBarOverlay);
    unsubscribeFromEvents();
    resetState();
  }

  /**
   * Initializes display elements based on configuration
   */
  private void initializeDisplays() {
    if (config.showOverlay()) {
      log.debug("Showing overlay");
      overlayManager.add(overlay);
    }
    if (config.showHeadBar()) {
      log.debug("Showing progress bar overlay");
      overlayManager.add(headBarOverlay);
    }
  }

  /**
   * Manages event subscriptions based on configuration and game state
   */
  private void updateSubscriptionState() {
    boolean shouldBeActive = !config.pvpOnly() || isPvpContext();

    if (shouldBeActive && !subscribed) {
      log.debug(
        "Activating combat timer in {} contexts",
        config.pvpOnly() ? "PvP" : "all"
      );
      subscribeToEvents();
    } else if (!shouldBeActive && subscribed) {
      log.debug("Deactivating combat timer due to non-PvP context");
      unsubscribeFromEvents();
      resetState();
    }
  }

  /**
   * Is the player in a PvP situation (PvP world or wilderness)
   */
  private boolean isPvpContext() {
    //        log.debug("Pvp context: World? {} Varbit? {}", client.getWorldType(), client.getVarbitValue(Varbits.IN_WILDERNESS));
    //        return WorldType.isPvpWorld(client.getWorldType()) || IsInWildy.fromValue(client.getVarbitValue(Varbits.IN_WILDERNESS)) == IsInWildy.HOLY_SHIT_YES_THEY_WENT_WILDY;
    boolean ret = false;

    if (isPvp) {
      log.debug("In PVP world");
      ret = true;
    } else if (inWilderness == IsInWildy.HOLY_SHIT_YES_THEY_WENT_WILDY) {
      log.debug("In wildy");
      ret = true;
    } else {
      log.debug("not in pvp world/wildy");
    }
    return ret;
  }

  /*
   * we are not idiots so we stop listening to events when we don't need to :-)
   */
  private void subscribeToEvents() {
    if (!subscribed) {
      eventBus.register(HitsplatApplied.class);
      eventBus.register(GameTick.class);
      subscribed = true;
    }
  }

  private void unsubscribeFromEvents() {
    if (subscribed) {
      eventBus.unregister(HitsplatApplied.class);
      eventBus.unregister(GameTick.class);
      subscribed = false;
    }
  }

  private void resetState() {
    logoutTicks = 0;
    graceTicks = 0;
  }

  @Subscribe
  public void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
    Actor target = hitsplatApplied.getActor();
    Hitsplat hitsplat = hitsplatApplied.getHitsplat();
    log.debug(
      "Hitsplat: Mine? {}, Type: {}, Amount: {}, Target Name: {}, Is Player? {}",
      hitsplat.isMine(),
      hitsplat.getHitsplatType(),
      hitsplat.getAmount(),
      target.getName(),
      target == client.getLocalPlayer()
    );

    if (target == client.getLocalPlayer()) {
      //            if ( hitsplat.getHitsplatType() == HitsplatID.DAMAGE_ME ||  // Regular damage
      //                    hitsplat.getHitsplatType() == HitsplatID.DAMAGE_OTHER ) { // Other damage types

      log.debug(
        "Combat timer reset - Received damage: {} from hitsplat type: {}",
        hitsplat.getAmount(),
        hitsplat.getHitsplatType()
      );

      resetCombatTimer();
    }
  }

  @Subscribe
  public void onGameTick(GameTick tick) {
    updateTimers();
  }

  @Subscribe
  public void onVarbitChanged(VarbitChanged event) {
    if (event.getVarbitId() == Varbits.IN_WILDERNESS) {
      log.debug("varbit changed: {} {}", event.getVarbitId(), event.getValue());
      inWilderness = IsInWildy.fromValue(event.getValue());
      log.debug(
        "Wilderness state changed: {}",
        inWilderness.toBoolean() ? "entered" : "exited"
      );
      updateSubscriptionState();
    }
  }

  @Subscribe
  public void onWorldChanged(WorldChanged event) {
    isPvp = WorldType.isPvpWorld(client.getWorldType());
    log.debug("World changed - PvP world? {}", isPvp);
    updateSubscriptionState();
  }

  @Subscribe
  public void onConfigChanged(ConfigChanged event) {
    if (!event.getGroup().equals(CONFIG_GROUP)) {
      return;
    }

    String key = event.getKey();
    String newval = event.getNewValue();
    String oldcrap = event.getOldValue();

    log.debug("Config '{}' changed to '{}' (was: {})", key, newval, oldcrap);

    switch (event.getKey()) {
      case "pvpOnly":
        updateSubscriptionState();
        break;
      case "showOverlay":
        if (Boolean.parseBoolean(event.getNewValue())) {
          overlayManager.add(overlay);
        } else {
          overlayManager.remove(overlay);
        }
        break;
      case "showHeadBar":
        if (Boolean.parseBoolean(event.getNewValue())) {
          overlayManager.add(headBarOverlay);
        } else {
          overlayManager.remove(headBarOverlay);
        }
        break;
    }
  }

  private void resetCombatTimer() {
    logoutTicks = COMBAT_TIMEOUT_TICKS;
    graceTicks = 0;
  }

  private void updateTimers() {
    if (logoutTicks > 0) {
      logoutTicks--;
      if (logoutTicks == 0) {
        log.debug("Combat timer expired, entering grace period");
        graceTicks = (config.gracePeriod() * 50) / 30;
        if (config.soundAlert()) {
          client.playSoundEffect(
            SoundEffectID.PRAYER_ACTIVATE_CLARITY_OF_THOUGHT
          );
        }
      }
    } else if (graceTicks > 0) {
      graceTicks--;
    }
  }

  @Provides
  CombatLogoutConfig provideConfig(ConfigManager configManager) {
    return configManager.getConfig(CombatLogoutConfig.class);
  }
}

class CombatLogoutOverlay extends Overlay {

  public static final DecimalFormat TIME_FORMAT = new DecimalFormat("#.#");
  private final CombatLogoutPlugin plugin;
  private final CombatLogoutConfig config;
  private final PanelComponent panelComponent = new PanelComponent();

  @Inject
  private CombatLogoutOverlay(
    CombatLogoutPlugin plugin,
    CombatLogoutConfig config
  ) {
    this.plugin = plugin;
    this.config = config;
    setLayer(OverlayLayer.ABOVE_SCENE);
    setPosition(OverlayPosition.TOP_LEFT);
    setPriority(OverlayPriority.HIGHEST); // probably
  }

  @Override
  public Dimension render(Graphics2D graphics) {
    if (!config.showOverlay()) {
      return null;
    }

    if (
      plugin.getLogoutTicks() <= 0 &&
      plugin.getGraceTicks() <= 0 &&
      config.gracePeriod() > 0
    ) {
      return null;
    }

    panelComponent.getChildren().clear();

    Color textColor;
    String text;

    int ticks = plugin.getLogoutTicks();
    if (ticks > 0) {
      float progress = 1 - (ticks / (float) 16); // Invert progress so red->green
      textColor = Utils.getProgressColor(progress);
      text = formatTime(ticks);
    } else if (plugin.getGraceTicks() > 0) {
      textColor = config.safeColor();
      text = String.format("OK (%d)", ((plugin.getGraceTicks() * 30) / 50));
    } else {
      textColor = config.safeColor();
      text = "OK";
    }

    panelComponent
      .getChildren()
      .add(
        LineComponent.builder()
          .left("Logout:")
          .right(text)
          .rightColor(textColor)
          .build()
      );

    return panelComponent.render(graphics);
  }

  private String formatTime(int ticks) {
    switch (config.timeFormat()) {
      case TICKS:
        return ticks + " ticks";
      case SECONDS:
        return TIME_FORMAT.format(ticks * 0.6) + "s";
      case MILLISECONDS:
        return (ticks * 600) + "ms";
      default:
        return String.valueOf(ticks);
    }
  }
}

/**
 * The bar overhead overlay that shows a colored progress bar above the player, shockingly
 */
class CombatLogoutOverheadBar extends Overlay {

  private final Client client;
  private final CombatLogoutPlugin plugin;
  private final CombatLogoutConfig config;

  @Inject
  private CombatLogoutOverheadBar(
    Client client,
    CombatLogoutPlugin plugin,
    CombatLogoutConfig config
  ) {
    this.client = client;
    this.plugin = plugin;
    this.config = config;
    setLayer(OverlayLayer.ABOVE_SCENE);
    setPosition(OverlayPosition.DYNAMIC);
  }

  @Override
  public Dimension render(Graphics2D graphics) {
    if (!config.showHeadBar()) {
      return null;
    }

    if (
      plugin.getLogoutTicks() <= 0 &&
      plugin.getGraceTicks() <= 0 &&
      config.gracePeriod() > 0
    ) {
      return null;
    }

    Player player = client.getLocalPlayer();
    if (player == null) {
      return null;
    }

    int ticks = plugin.getLogoutTicks();
    if (ticks <= 0) {
      return null;
    }

    LocalPoint lp = player.getLocalLocation();
    if (lp == null) {
      return null;
    }

    Point point = Perspective.localToCanvas(
      client,
      lp,
      client.getPlane(),
      player.getLogicalHeight() + 60
    );
    if (point == null) {
      return null;
    }

    // Bar dimensions and position
    final int width = 50;
    final int height = 4;
    final int x = point.getX() - width / 2;
    final int y = point.getY();

    // Color interpolation
    float progress = 1 - (ticks / (float) 16);
    Color currentColor = Utils.getProgressColor(progress);

    // Draw bar background
    graphics.setColor(new Color(0, 0, 0, 150));
    graphics.fillRect(x, y, width, height);

    // Draw progress
    graphics.setColor(currentColor);
    int progressWidth = (int) (width * (1 - progress));
    graphics.fillRect(x, y, progressWidth, height);

    String text = "";
    if (ticks > 0) {
      // Format based on config
      switch (config.timeFormat()) {
        case TICKS:
          text = ticks + " t";
          break;
        case SECONDS:
          text = CombatLogoutOverlay.TIME_FORMAT.format(ticks * 0.6) + " s";
          break;
        case MILLISECONDS:
          text = (ticks * 600) + " ms";
          break;
        default:
          text = String.valueOf(ticks);
      }
    } else if (plugin.getGraceTicks() > 0) {
      text = "Safe";
    } else if (plugin.getGraceTicks() == 0) {
      text = "";
    }

    graphics.setFont(FontManager.getRunescapeSmallFont());

    FontMetrics fm = graphics.getFontMetrics();
    int textWidth = fm.stringWidth(text);
    int textX = x + (width - textWidth) / 2;
    int textY = y - 1;

    //shadow
    graphics.setColor(Color.BLACK);
    graphics.drawString(text, textX + 1, textY + 1);

    graphics.setColor(currentColor);
    graphics.drawString(text, textX, textY);

    return null;
  }
}
