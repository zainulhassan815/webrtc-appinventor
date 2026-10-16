package dev.dreamerslab.webrtc.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum ConnectionState implements OptionList<String> {
  /** No connection attempt has started yet. */
  New("New"),
  /** The peers are finding a network path to each other. */
  Connecting("Connecting"),
  /** Media and data flow between the peers. */
  Connected("Connected"),
  /** The network path was lost. WebRTC tries to recover it. */
  Disconnected("Disconnected"),
  /** The connection couldn't be established or recovered. */
  Failed("Failed"),
  /** The connection was closed. */
  Closed("Closed");

  private static final Map<String, ConnectionState> lookup = new HashMap<>();

  static {
    for (ConnectionState option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  ConnectionState(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static ConnectionState fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
