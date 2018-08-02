package org.rainbowstudio.patterns;

import static processing.core.PConstants.P2D;
import static processing.core.PConstants.P3D;

import heronarts.lx.LX;
import heronarts.lx.LXPattern;
import heronarts.lx.parameter.CompoundParameter;
import org.rainbowstudio.RainbowStudio;
import processing.core.PGraphics;

/*
 * Abstract base class for all Processing PGraphics drawing and mapping
 * to the Rainbow.
 */
abstract class PGBase extends LXPattern {
  public final CompoundParameter fpsKnob =
    new CompoundParameter("Fps", 1.0, 60.0)
    .setDescription("Controls the frames per second.");

  protected double currentFrame = 0.0;
  protected PGraphics pg;
  protected int imageWidth = 0;
  protected int imageHeight = 0;
  protected int previousFrame = -1;
  protected double deltaDrawMs = 0.0;

  // For P3D, we need to be on the UI/GL Thread.  We should always be on the GL thread
  // during initialization because we start with Multithreading off.  If somebody enables
  // the Engine thread in the UI we don't want to crash so we will keep track of the GL
  // thread and if the current thread in our run() method doesn't match glThread we will just
  // skip our GL render (image will freeze).
  protected Thread glThread;

  public PGBase(LX lx, int width, int height, String drawMode) {
    super(lx);
    imageWidth = width;
    imageHeight = height;
    if (P3D.equals(drawMode) || P2D.equals(drawMode)) {
      glThread = Thread.currentThread();
      pg = RainbowStudio.pApplet.createGraphics(imageWidth, imageHeight, drawMode);
    } else {
      pg = RainbowStudio.pApplet.createGraphics(imageWidth, imageHeight);
    }
    addParameter(fpsKnob);
  }

  public void run(double deltaMs) {
    double fps = fpsKnob.getValue();
    currentFrame += (deltaMs/1000.0) * fps;
    // We don't call draw() every frame so track the accumulated deltaMs for them.
    deltaDrawMs += deltaMs;
    if ((int)currentFrame > previousFrame) {
      // if glThread == null this is the default Processing renderer so it is always
      // okay to draw.  If it is not-null, we need to make sure the pattern is
      // executing on the glThread or else Processing will crash.
      if (glThread == null || Thread.currentThread() == glThread) {
        // Time for new frame.  Draw
        pg.beginDraw();
        draw(deltaDrawMs);
        pg.endDraw();
        pg.loadPixels();
      }
      previousFrame = (int)currentFrame;
      deltaDrawMs = 0.0;
    }
    // Don't let current frame increment forever.  Otherwise float will
    // begin to lose precision and things get wonky.
    if (currentFrame > 10000.0) {
      currentFrame = 0.0;
      previousFrame = -1;
    }
    imageToPoints();
  }

  // Responsible for calling RenderImageUtil.imageToPointsSemiCircle to
  // RenderImageUtil.imageToPointsPixelPerfect.
  abstract protected void imageToPoints();

  // Implement PGGraphics drawing code here.  PGTexture handles beginDraw()/endDraw();
  abstract protected void draw(double deltaDrawMs);
}
