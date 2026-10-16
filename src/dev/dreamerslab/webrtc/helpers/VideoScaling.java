package dev.dreamerslab.webrtc.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum VideoScaling implements OptionList<String> {
  /** Fills the view, cropping the video's edges. */
  Fill("Fill"),
  /** Shows the whole video, with bars where its shape differs from the view's. */
  Fit("Fit");

  private static final Map<String, VideoScaling> lookup = new HashMap<>();

  static {
    for (VideoScaling option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  VideoScaling(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static VideoScaling fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
