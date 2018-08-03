package org.rainbowstudio.patterns;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import org.rainbowstudio.model.RainbowBaseModel;

/*
 * Pixel perfect animated GIFs.  Uses base class with directory data/gifpp, default file of life2.gif, and
 * no antialias toggle.
 */
@LXCategory(LXCategory.FORM)
public class RainbowGIFPP extends RainbowGIFBase {
  public RainbowGIFPP(LX lx) {
    super(lx, ((RainbowBaseModel)lx.model).pointsWide, ((RainbowBaseModel)lx.model).pointsHigh,
    "gifpp/",
    "life2",
    false);
  }

  protected void renderToPoints() {
    RenderImageUtil.imageToPointsPixelPerfect(lx, colors, images[(int)currentFrame]);
  }
}
