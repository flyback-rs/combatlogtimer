package com.curvelabs.combatlogout;

import java.awt.*;
import java.text.DecimalFormat;

class CombatLogoutUtil {

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.#");

  /**
   * Converts game ticks to milliseconds
   * @param ticks Number of game ticks
   * @return Equivalent milliseconds
   */
  public static int ticksToMillis(int ticks) {
    return ticks * 600;
  }

  /**
   * Converts game ticks to seconds with one decimal place
   * @param ticks Number of game ticks
   * @return Formatted string representing seconds
   */
  public static String ticksToSeconds(int ticks) {
    return DECIMAL_FORMAT.format(ticks * 0.6);
  }

  /**
   * Converts seconds to game ticks
   * @param seconds Number of seconds
   * @return Equivalent game ticks (rounded)
   */
  public static int secondsToTicks(int seconds) {
    return (int) Math.round(seconds * (50.0 / 30.0));
  }

  /**
   * Formats a time value according to the specified format
   * @param ticks Current tick count
   * @param format Desired time format
   * @return Formatted time string
   */
  public static String formatTime(
    int ticks,
    CombatLogoutConfig.TimeFormat format
  ) {
    switch (format) {
      case TICKS:
        return ticks + " ticks";
      case SECONDS:
        return ticksToSeconds(ticks) + "s";
      case MILLISECONDS:
        return ticksToMillis(ticks) + "ms";
      default:
        return String.valueOf(ticks);
    }
  }
}

/**
 * Exception class for combat timer specific errors
 */
class CombatLogoutException extends RuntimeException {

  public CombatLogoutException(String message) {
    super(message);
  }

  public CombatLogoutException(String message, Throwable cause) {
    super(message, cause);
  }
}

public class Utils {

  // Color stops in HSB - Hue values are in 0-1 range (divide degrees by 360)
  private static final float[][] COLOR_STOPS = {
    // Hue, Saturation, Brightness
    { 0f, 0.9f, 0.9f }, // Red
    { 30f / 360f, 0.9f, 0.9f }, // Orange
    { 60f / 360f, 0.9f, 0.9f }, // Yellow
    { 90f / 360f, 0.9f, 0.9f }, // Yellow-Green
    { 120f / 360f, 0.9f, 1f }, // Bright Green
  };

  /**
   * Gets color for progress value using HSB color space
   * @param progress Value from 0.0 (start) to 1.0 (end)
   * @return Color for the current progress
   */
  public static Color getProgressColor(float progress) {
    progress = Math.min(1, Math.max(0, progress));

    float segments = COLOR_STOPS.length - 1;
    float scaledProgress = progress * segments;
    int index = (int) scaledProgress;
    float remainder = scaledProgress - index;

    if (index >= COLOR_STOPS.length - 1) {
      float[] lastStop = COLOR_STOPS[COLOR_STOPS.length - 1];
      return Color.getHSBColor(lastStop[0], lastStop[1], lastStop[2]);
    }

    float[] start = COLOR_STOPS[index];
    float[] end = COLOR_STOPS[index + 1];

    float hue = interpolateHue(start[0], end[0], remainder);
    float saturation = start[1] + (end[1] - start[1]) * remainder;
    float brightness = start[2] + (end[2] - start[2]) * remainder;

    return Color.getHSBColor(hue, saturation, brightness);
  }

  /**
   * Interpolates between two hue values around the color wheel
   */
  private static float interpolateHue(float h1, float h2, float progress) {
    float diff = h2 - h1;

    if (diff > 0.5f) {
      h2 -= 1.0f;
    } else if (diff < -0.5f) {
      h2 += 1.0f;
    }

    float h = h1 + (h2 - h1) * progress;

    if (h < 0) {
      h += 1.0f;
    } else if (h > 1) {
      h -= 1.0f;
    }

    return h;
  }
}

/**
 * Constants used throughout the plugin
 */
final class CombatLogoutConstants {

  private CombatLogoutConstants() {} // Prevent instantiation

  public static final int COMBAT_TIMEOUT_TICKS = 16;
  public static final int MILLIS_PER_TICK = 600;
  public static final int DEFAULT_GRACE_PERIOD_SECONDS = 3;
  public static final int DEFAULT_BAR_WIDTH = 30;
  public static final int DEFAULT_BAR_HEIGHT = 4;
  public static final int HEAD_BAR_OFFSET = 20;

  public static final Color DEFAULT_DANGER_COLOR = new Color(255, 0, 0);
  public static final Color DEFAULT_SAFE_COLOR = new Color(0, 255, 0);
  public static final Color BAR_BACKGROUND_COLOR = new Color(0, 0, 0, 150);

  public static final String CONFIG_GROUP = "combatlogout";
  public static final String CONFIG_KEY_PVP_ONLY = "pvpOnly";
  public static final String CONFIG_KEY_SHOW_OVERLAY = "showOverlay";
  public static final String CONFIG_KEY_SHOW_HEAD_BAR = "showHeadBar";
}
