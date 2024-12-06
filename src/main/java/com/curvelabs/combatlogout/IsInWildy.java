package com.curvelabs.combatlogout;

import lombok.Getter;

// jesus fuck
enum IsInWildy {
  NO_IS_NOT_IN_WILDERNESS(0, false),
  HOLY_SHIT_YES_THEY_WENT_WILDY(1, true);

  @Getter
  private final int value;

  private final boolean booleanValue;

  IsInWildy(int value, boolean booleanValue) {
    this.value = value;
    this.booleanValue = booleanValue;
  }

  public static IsInWildy fromValue(int value) {
    for (IsInWildy status : IsInWildy.values()) {
      if (status.getValue() == value) return status;
    }
    throw new IllegalArgumentException("you are a fucking idiot: " + value);
  }

  public static IsInWildy fromBoolean(boolean value) {
    return value ? HOLY_SHIT_YES_THEY_WENT_WILDY : NO_IS_NOT_IN_WILDERNESS;
  }

  public boolean toBoolean() {
    return booleanValue;
  }
}
