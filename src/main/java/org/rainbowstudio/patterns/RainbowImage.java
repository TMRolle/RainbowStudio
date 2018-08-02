package org.rainbowstudio.patterns;

import static processing.core.PApplet.ceil;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import org.rainbowstudio.model.RainbowBaseModel;

@LXCategory(LXCategory.FORM)
public class RainbowImage extends RainbowImageBase {
  public RainbowImage(LX lx) {
    super(lx, ceil(RainbowBaseModel.outerRadius * RainbowBaseModel.pixelsPerFoot * 2.0f),
          ceil(RainbowBaseModel.outerRadius * RainbowBaseModel.pixelsPerFoot),
      "./img/",
      "oregontex.jpg",
      true);
  }

  protected void renderToPoints() {
    RenderImageUtil.imageToPointsSemiCircle(lx, colors, image, antialiasKnob.isOn());
  }
}
