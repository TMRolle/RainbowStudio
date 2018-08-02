package org.rainbowstudio.patterns;

import gifAnimation.Gif;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.StringParameter;
import heronarts.p3lx.ui.CustomDeviceUI;
import heronarts.p3lx.ui.UI;
import heronarts.p3lx.ui.UI2dContainer;
import heronarts.p3lx.ui.component.UIButton;
import heronarts.p3lx.ui.component.UIItemList;
import heronarts.p3lx.ui.component.UIKnob;
import heronarts.p3lx.ui.component.UITextBox;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.rainbowstudio.RainbowStudio;
import processing.core.PConstants;
import processing.core.PImage;

@LXCategory(LXCategory.FORM)
public class AnimatedSpritePP extends PGPixelPerfect implements CustomDeviceUI {
  public final StringParameter spriteFileKnob = new StringParameter("sprite", "smallcat");
  public final CompoundParameter xSpeed =
    new CompoundParameter("XSpd", 1, 20)
    .setDescription("X speed in pixels per frame");

  List<FileItem> fileItems = new ArrayList<FileItem>();
  UIItemList.ScrollList fileItemList;
  List<String> spriteFiles;
  private static final int CONTROLS_MIN_WIDTH = 120;
  public String filename = "smallcat.gif";

  private PImage[] images;
  protected int currentPos = 0;

  public AnimatedSpritePP(LX lx) {
    super(lx, "");
    addParameter(xSpeed);
    xSpeed.setValue(5);
    loadSprite(spriteFileKnob.getString());
    spriteFiles = getSpriteFiles();
    for (String filename : spriteFiles) {
      fileItems.add(new FileItem(filename));
    }
  }

  public void draw(double deltaMs) {
    pg.background(0);
    try {
      PImage frameImg = images[((int)currentFrame)%images.length];
      if (currentPos < 0 - frameImg.width) {
        currentPos = imageWidth + frameImg.width + 1;
      }
      pg.image(frameImg, currentPos, 0);
      currentPos -= xSpeed.getValue();
    }
    catch (ArrayIndexOutOfBoundsException ex) {
      // handle race condition when reloading images.
    }
  }

  protected void loadSprite(String spritename) {
    String filename = RainbowStudio.pApplet.dataPath("./spritepp/" + spritename + ".gif");
    PImage[] newImages = Gif.getPImages(RainbowStudio.pApplet, filename);
    for (int i = 0; i < newImages.length; i++) {
      newImages[i].loadPixels();
    }
    images = newImages;
    // Start off the screen to the right.
    currentPos = imageWidth + images[0].width + 1;
  }

  protected File getFile() {
    return new File(RainbowStudio.pApplet.dataPath("./spritepp/" + this.spriteFileKnob.getString() + ".gif"));
  }

  protected List<String> getSpriteFiles() {
    List<String> results = new ArrayList<String>();

    File[] files = new File(RainbowStudio.pApplet.dataPath("./spritepp/")).listFiles();
    //If this pathname does not denote a directory, then listFiles() returns null.
    for (File file : files) {
      if (file.isFile()) {
        if (file.getName().endsWith(".gif")) {
          results.add(ImgUtil.stripExtension(file.getName()));
        }
      }
    }
    return results;
  }

  //
  // Custom UI to allow for the selection of the shader file
  //
  @Override
    public void buildDeviceUI(UI ui, final UI2dContainer device) {
    device.setContentWidth(CONTROLS_MIN_WIDTH);
    device.setLayout(UI2dContainer.Layout.VERTICAL);
    device.setPadding(3, 3, 3, 3);

    UI2dContainer knobsContainer = new UI2dContainer(0, 30, device.getWidth(), 45);
    knobsContainer.setLayout(UI2dContainer.Layout.HORIZONTAL);
    knobsContainer.setPadding(3, 3, 3, 3);
    new UIKnob(xSpeed).addToContainer(knobsContainer);
    new UIKnob(fpsKnob).addToContainer(knobsContainer);
    knobsContainer.addToContainer(device);

    UI2dContainer filenameEntry = new UI2dContainer(0, 0, device.getWidth(), 30);
    filenameEntry.setLayout(UI2dContainer.Layout.HORIZONTAL);

    fileItemList =  new UIItemList.ScrollList(ui, 0, 5, CONTROLS_MIN_WIDTH, 80);
    new UITextBox(0, 0, device.getContentWidth() - 22, 20)
      .setParameter(spriteFileKnob)
      .setTextAlignment(PConstants.LEFT)
      .addToContainer(filenameEntry);


    // Button for reloading shader.
    new UIButton(device.getContentWidth() - 20, 0, 20, 20) {
      @Override
        public void onToggle(boolean on) {
        if (on) {
          loadSprite(spriteFileKnob.getString());
        }
      }
    }
    .setLabel("\u21BA").setMomentary(true).addToContainer(filenameEntry);
    filenameEntry.addToContainer(device);

    fileItemList =  new UIItemList.ScrollList(ui, 0, 5, CONTROLS_MIN_WIDTH, 80);
    fileItemList.setShowCheckboxes(false);
    fileItemList.setItems(fileItems);
    fileItemList.addToContainer(device);
  }

  public class FileItem extends FileItemBase {
    public FileItem(String filename) {
      super(filename);
    }
    public void onActivate() {
      spriteFileKnob.setValue(filename);
      loadSprite(filename);
    }
  }
}
