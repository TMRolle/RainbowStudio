/*
 * By using LX Studio, you agree to the terms of the LX Studio Software
 * License and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 */

// ---------------------------------------------------------------------------
//
// Welcome to LX Studio! Getting started is easy...
//
// (1) Quickly scan this file
// (2) Look at "Model" to define your model
// (3) Move on to "Patterns" to write your animations
//
// ---------------------------------------------------------------------------

package org.rainbowstudio;

import com.google.common.reflect.ClassPath;
import com.google.gson.JsonObject;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXEffect;
import heronarts.lx.LXPattern;
import heronarts.lx.model.LXModel;
import heronarts.lx.studio.LXStudio;
import heronarts.p3lx.ui.UI3dContext;
import heronarts.p3lx.ui.UIEventHandler;
import heronarts.p3lx.ui.component.UIGLPointCloud;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rainbowstudio.model.RainbowBaseModel;
import org.rainbowstudio.model.RainbowModel3D;
import org.rainbowstudio.model.SimplePanel;
import org.rainbowstudio.ui.UIAudioMonitorLevels;
import org.rainbowstudio.ui.UIGammaSelector;
import org.rainbowstudio.ui.UIModeSelector;
import org.rainbowstudio.ui.UIPixliteConfig;
import processing.core.PApplet;
import processing.event.KeyEvent;

public class RainbowStudio extends PApplet {
  static {
    System.setProperty(
        "java.util.logging.SimpleFormatter.format",
        "%3$s: %1$tc [%4$s] %5$s%6$s%n");
  }

  private static final Logger logger = Logger.getLogger(RainbowStudio.class.getName());

  public static void main(String[] args) {
    PApplet.main(RainbowStudio.class.getName(), args);
  }

  // Reference to top-level LX instance
  heronarts.lx.studio.LXStudio lx;

  public static final int GLOBAL_FRAME_RATE = 60;
  public static final boolean enableArtNet = false;
  public static final int ARTNET_PORT = 6454;
  public static final String LED_CONTROLLER_IP = "192.168.2.134";

  public static final int FULL_RAINBOW = 0;
  public static final int SRIKANTH_PANEL = 1;
  public static final int RAINBOW_PANEL = 2;
  public static final int LARGE_PANEL = 3;
  public static final int RAINBOW_PANEL_4 = 4;
  public static final int RAINBOW_PANEL_2 = 5;

  // Used for PixelFlow.  Needs a reference to pApplet for setting up
  // OpenGL Context.
  public static PApplet pApplet;

  public static boolean fullscreenMode = false;
  static UI3dContext fullscreenContext;
  static UIGammaSelector gammaControls;
  static UIModeSelector modeSelector;
  static UIAudioMonitorLevels audioMonitorLevels;
  static UIPixliteConfig pixliteConfig;

  @Override
  public void settings() {
    size(800, 720, P3D);
  }

  /**
   * Registers all patterns and effects that LX doesn't already have registered.
   * This check is important because LX just adds to a list.
   *
   * @param lx the LX environment
   */
  private void registerAll(LXStudio lx) {
    List<Class<? extends LXPattern>> patterns = lx.getRegisteredPatterns();
    List<Class<? extends LXEffect>> effects = lx.getRegisteredEffects();
    final String parentPackage = getClass().getPackage().getName();

    try {
      ClassPath classPath = ClassPath.from(getClass().getClassLoader());
      for (ClassPath.ClassInfo classInfo : classPath.getAllClasses()) {
        // Limit to this package and sub-packages
        if (!classInfo.getPackageName().startsWith(parentPackage)) {
          continue;
        }
        Class<?> c = classInfo.load();
        if (Modifier.isAbstract(c.getModifiers())) {
          continue;
        }
        if (LXPattern.class.isAssignableFrom(c)) {
          Class<? extends LXPattern> p = c.asSubclass(LXPattern.class);
          if (!patterns.contains(p)) {
            lx.registerPattern(p);
            logger.info("Added pattern: " + p);
          }
        } else if (LXEffect.class.isAssignableFrom(c)) {
          Class<? extends LXEffect> e = c.asSubclass(LXEffect.class);
          if (!effects.contains(e)) {
            lx.registerEffect(e);
            logger.info("Added effect: " + e);
          }
        }
      }
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Error finding pattern and effect classes", ex);
    }
  }

  @Override
  public void setup() {
    // Processing setup, constructs the window and the LX instance
    pApplet = this;
    frameRate(GLOBAL_FRAME_RATE);

    int modelType = FULL_RAINBOW; // RAINBOW_PANEL, RAINBOW_PANEL_4 or FULL_RAINBOW

    LXModel model = buildModel(modelType);
    /* MULTITHREADED disabled for P3D, GL, Hardware Acceleration */
    boolean multithreaded = false;
    lx = new heronarts.lx.studio.LXStudio(this, model, multithreaded);

    // Register any patterns and effects LX doesn't recognize
    registerAll(lx);

    lx.ui.setResizable(RESIZABLE);

    modeSelector = (UIModeSelector) new UIModeSelector(lx.ui, lx).setExpanded(true).addToContainer(lx.ui.leftPane.global);
    gammaControls = (UIGammaSelector) new UIGammaSelector(lx.ui)
        .setExpanded(false).addToContainer(lx.ui.leftPane.global);
    audioMonitorLevels = (UIAudioMonitorLevels) new UIAudioMonitorLevels(lx.ui).setExpanded(false).addToContainer(lx.ui.leftPane.global);
    pixliteConfig = (UIPixliteConfig) new UIPixliteConfig(lx.ui).setExpanded(false).addToContainer(lx.ui.leftPane.global);

    if (modelType == RAINBOW_PANEL) {
      // Manually force the camera settings for a single panel.  A single panel is
      // way at the top of the world space and it is difficult to zoom in on it.
      float cameraY = RainbowBaseModel.innerRadius +
          (RainbowBaseModel.outerRadius - RainbowBaseModel.innerRadius) / 2.0f;
      lx.ui.preview.setCenter(0.0f, cameraY, 0.0f);
      lx.ui.preview.setRadius(8.0f);
    }

    // Output the model bounding box for reference.
    System.out.println("minx, miny: " + model.xMin + "," + model.yMin);
    System.out.println("maxx, maxy: " + model.xMax + "," + model.yMax);
    System.out.println("bounds size: " + (model.xMax - model.xMin) + "," +
        (model.yMax - model.yMin));

    int texturePixelsWide = ceil(((RainbowBaseModel) model).outerRadius *
        ((RainbowBaseModel) model).pixelsPerFoot) * 2;
    int texturePixelsHigh = ceil(((RainbowBaseModel) model).outerRadius *
        ((RainbowBaseModel) model).pixelsPerFoot);
    System.out.println("texture image size: " + texturePixelsWide + "x" +
        texturePixelsHigh);

    int innerRadiusPixels = floor(RainbowBaseModel.innerRadius * RainbowBaseModel.pixelsPerFoot);
    int outerRadiusPixels = ceil(RainbowBaseModel.outerRadius * RainbowBaseModel.pixelsPerFoot);
    System.out.println("innerRadiusPixels = " + innerRadiusPixels);
    System.out.println("outerRadiusPixels = " + outerRadiusPixels);

    // FULL_RAINBOW is
    // rectangle bounds size: 86.52052, 37.74478
    // Roughly, 87, 38 feet with led's per 2 inch (highest density) = 87*6, 38*6 = 522x228
    // 86.52052 * 6 = 519.12312
    // 37.74478 * 6 = 226.46868
    // NOTE(tracy): Using images at larger sizes reduces aliasing artifacts
    // when not resorting to averaging neighbors in the pattern code.

    if (enableArtNet) {
      if (modelType == FULL_RAINBOW) {
        SimplePanel.configureOutputMultiPanel(lx, pixliteConfig);
      } else if (modelType == SRIKANTH_PANEL) {
        SimplePanel.configureOutputSrikanthPanel(lx);
      } else if (modelType == RAINBOW_PANEL) {
        SimplePanel.configureOutputRainbowPanel(lx);
      } else if (modelType == RAINBOW_PANEL_4) {
        SimplePanel.configureOutputMultiPanel(lx, pixliteConfig);
      } else if (modelType == RAINBOW_PANEL_2) {
        SimplePanel.configureOutputMultiPanel(lx, pixliteConfig);
      }
    }

    // Check for data/PLAYASIDE
    if (new File(dataPath("PLAYASIDE")).exists()) {
      System.out.println("PLAYASIDE");
      modeSelector.autoAudioModeP.setValue(true);
    } else {
      System.out.println(dataPath("PLAYASIDE") + " does not exist.");
      modeSelector.autoAudioModeP.setValue(false);
    }

    // Dump our MIDI device names for reference.
    heronarts.lx.midi.LXMidiEngine midi = lx.engine.midi;
    for (heronarts.lx.midi.LXMidiOutput output : midi.outputs) {
      System.out.println(output.getName() + ": " + output.getDescription());
    }

    // Support Fullscreen Mode.  We create a second UIGLPointCloud and
    // add it to a LXStudio.UI layer.  When entering fullscreen mode,
    // toggleFullscreen() will set the
    // standard UI components visibility to false and the larger
    // fullscreenContext visibility to true.
    UIGLPointCloud fullScreenPointCloud = new UIGLPointCloud(lx);
    fullscreenContext = new UI3dContext(lx.ui);
    fullscreenContext.addComponent(fullScreenPointCloud);
    lx.ui.addLayer(fullscreenContext);
    fullscreenContext.setVisible(false);

    lx.ui.setTopLevelKeyEventHandler(new TopLevelKeyEventHandler());
    lx.ui.setBackgroundColor(0);

    /*
    Locale currentLocale = Locale.getDefault();

  System.out.println(currentLocale.getDisplayLanguage());
  System.out.println(currentLocale.getDisplayCountry());

  System.out.println(currentLocale.getLanguage());
  System.out.println(currentLocale.getCountry());

  System.out.println(System.getProperty("user.country"));
  System.out.println(System.getProperty("user.language"));
  */
  }

  public class TopLevelKeyEventHandler extends UIEventHandler {
    public TopLevelKeyEventHandler() {
      super();
    }

    protected void onKeyPressed(KeyEvent keyEvent, char keyChar, int keyCode) {
      super.onKeyPressed(keyEvent, keyChar, keyCode);
      if (keyCode == 70) {
        toggleFullscreen();
      }
    }
  }

  void toggleFullscreen() {
    if (fullscreenMode == false) {
      lx.ui.leftPane.setVisible(false);
      lx.ui.rightPane.setVisible(false);
      lx.ui.helpBar.setVisible(false);
      lx.ui.bottomTray.setVisible(false);
      lx.ui.toolBar.setVisible(false);
      lx.ui.preview.setVisible(false);

      fullscreenContext.setVisible(true);
      fullscreenMode = true;
    } else {
      fullscreenContext.setVisible(false);

      lx.ui.leftPane.setVisible(true);
      lx.ui.rightPane.setVisible(true);
      lx.ui.helpBar.setVisible(true);
      lx.ui.bottomTray.setVisible(true);
      lx.ui.toolBar.setVisible(true);
      lx.ui.preview.setVisible(true);
      fullscreenMode = false;
    }
  }

  private class Settings extends LXComponent {

    private final LXStudio.UI ui;

    private Settings(LX lx, LXStudio.UI ui) {
      super(lx);
      this.ui = ui;
    }

    private static final String KEY_GAMMA_RED = "gammaRed";
    private static final String KEY_GAMMA_GREEN = "gammaGreen";
    private static final String KEY_GAMMA_BLUE = "gammaBlue";

    private static final String KEY_PIXLITE1_IP = "pixlite1Ip";
    private static final String KEY_PIXLITE1_PORT = "pixlite1Port";
    private static final String KEY_PIXLITE2_IP = "pixlite2Ip";
    private static final String KEY_PIXLITE2_PORT = "pixlite2Port";

    @Override
    public void save(LX lx, JsonObject obj) {
      obj.addProperty(KEY_GAMMA_RED, gammaControls.redGamma.getValue());
      obj.addProperty(KEY_GAMMA_GREEN, gammaControls.greenGamma.getValue());
      obj.addProperty(KEY_GAMMA_BLUE, gammaControls.blueGamma.getValue());
      obj.addProperty(KEY_PIXLITE1_IP, pixliteConfig.pixlite1IpP.getString());
      obj.addProperty(KEY_PIXLITE1_PORT, pixliteConfig.pixlite1PortP.getString());
      obj.addProperty(KEY_PIXLITE2_IP, pixliteConfig.pixlite2IpP.getString());
      obj.addProperty(KEY_PIXLITE2_PORT, pixliteConfig.pixlite2PortP.getString());
    }

    @Override
    public void load(LX lx, JsonObject obj) {
      System.out.println("Loading settings....");
      if (obj.has(KEY_GAMMA_RED)) {
        gammaControls.redGamma.setValue(obj.get(KEY_GAMMA_RED).getAsDouble());
      }
      if (obj.has(KEY_GAMMA_GREEN)) {
        gammaControls.greenGamma.setValue(obj.get(KEY_GAMMA_GREEN).getAsDouble());
      }
      if (obj.has(KEY_GAMMA_BLUE)) {
        gammaControls.blueGamma.setValue(obj.get(KEY_GAMMA_BLUE).getAsDouble());
      }
      if (obj.has(KEY_PIXLITE1_IP)) {
        pixliteConfig.pixlite1IpP.setValue(obj.get(KEY_PIXLITE1_IP).getAsString());
      }
      if (obj.has(KEY_PIXLITE1_PORT)) {
        pixliteConfig.pixlite1PortP.setValue(obj.get(KEY_PIXLITE1_PORT).getAsString());
      }
      if (obj.has(KEY_PIXLITE2_IP)) {
        pixliteConfig.pixlite2IpP.setValue(obj.get(KEY_PIXLITE2_IP).getAsString());
      }
      if (obj.has(KEY_PIXLITE2_PORT)) {
        pixliteConfig.pixlite2PortP.setValue(obj.get(KEY_PIXLITE2_PORT).getAsString());
      }
    }
  }


  void initialize(final heronarts.lx.studio.LXStudio lx, heronarts.lx.studio.LXStudio.UI ui) {
    // Add custom components or output drivers here
    // Register settings
    lx.engine.registerComponent("rainbowSettings", new Settings(lx, ui));
  }

  void onUIReady(heronarts.lx.studio.LXStudio lx, heronarts.lx.studio.LXStudio.UI ui) {
    // Add custom UI components here
  }

  public void draw() {
    // All is handled by LX Studio
  }

  // Configuration flags
  final static boolean MULTITHREADED = true;
  final static boolean RESIZABLE = true;

  // Helpful global constants
  final static float INCHES = 1.0f / 12.0f;
  final static float IN = INCHES;
  final static float FEET = 1.0f;
  final static float FT = FEET;
  final static float CM = IN / 2.54f;
  final static float MM = CM * .1f;
  final static float M = CM * 100;
  final static float METER = M;

  public static final int LEDS_PER_UNIVERSE = 170;

  LXModel buildModel(int modelType) {
    // A three-dimensional grid model
    // return new GridModel3D();
    if (modelType == FULL_RAINBOW) {
      return new RainbowModel3D();
    } else if (modelType == SRIKANTH_PANEL) {
      return new SimplePanel();
    } else if (modelType == RAINBOW_PANEL) {
      return new RainbowModel3D(1);
    } else if (modelType == LARGE_PANEL) {
      return new SimplePanel(100, 50);
    } else if (modelType == RAINBOW_PANEL_4) {
      return new RainbowModel3D(4);
    } else if (modelType == RAINBOW_PANEL_2) {
      return new RainbowModel3D(2);
    } else {
      return null;
    }
  }
}
