package org.rainbowstudio.patterns;

import static processing.core.PConstants.P2D;

import com.google.gson.JsonObject;
import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;
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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.rainbowstudio.PathUtils;
import org.rainbowstudio.RainbowStudio;
import processing.core.PConstants;
import processing.core.PGraphics;

@LXCategory(LXCategory.FORM)
public class ShaderToy extends PGPixelPerfect implements CustomDeviceUI {
  public final StringParameter shaderFileKnob = new StringParameter("frag", "sparkles");
  public final CompoundParameter knob1 =
    new CompoundParameter("K1", 0, 1).setDescription("Mapped to iMouse.x");
  public final CompoundParameter knob2 =
    new CompoundParameter("K2", 0, 1).setDescription("Mapped to iMouse.y");
  public final CompoundParameter knob3 =
    new CompoundParameter("K3", 0, 1).setDescription("Mapped to iMouse.z");
  public final CompoundParameter knob4 =
    new CompoundParameter("K4", 0, 1).setDescription("Mapped to iMouse.w");

  List<FileItem> fileItems = new ArrayList<FileItem>();
  UIItemList.ScrollList fileItemList;
  List<String> shaderFiles;

  DwPixelFlow context;
  DwShadertoy toy;
  DwGLTexture tex0 = new DwGLTexture();
  PGraphics toyGraphics;
  private static final int CONTROLS_MIN_WIDTH = 200;

  private static final String SHADER_DIR = "";

  public ShaderToy(LX lx) {
    super(lx, "");
    fpsKnob.setValue(60);
    addParameter(knob1);
    addParameter(knob2);
    addParameter(knob3);
    addParameter(knob4);
    addParameter(shaderFileKnob);
    toyGraphics = RainbowStudio.pApplet.createGraphics(imageWidth, imageHeight, P2D);
    loadShader(shaderFileKnob.getString());
    // context initialized in loadShader, print the GL hardware once when loading
    // the pattern.  left in for now while testing performance on different
    // graphics hardware.
    context.print();
    context.printGL();

    shaderFiles = PathUtils.findDataFiles(SHADER_DIR, ".frag");
    for (String filename : shaderFiles) {
      // Use a name that's suitable for the knob
      int index = filename.lastIndexOf('/');
      if (index >= 0) {
        filename = filename.substring(index + 1);
      }
      index = filename.lastIndexOf('.');
      if (index >= 0) {
        filename = filename.substring(0, index);
      }
      fileItems.add(new FileItem(filename));
    }

    // TODO(tracy):  This is Voronoi-specific data.  ShaderToy shaders
    // that rely on inputs might need custom implemented patterns.
    // Some inputs are standard like Audio data
    // so that can be enabled with a toggle.  Actually, each Channel0..3
    // should have a dropdown to select the input as on shadertoy.com.

    // create noise texture.
    int wh = 256;
    byte[] bdata = new byte[wh * wh * 4];
    ByteBuffer bbuffer = ByteBuffer.wrap(bdata);
    for (int i = 0; i < bdata.length; ) {
      bdata[i++] = (byte) RainbowStudio.pApplet.random(0, 255);
      bdata[i++] = (byte) RainbowStudio.pApplet.random(0, 255);
      bdata[i++] = (byte) RainbowStudio.pApplet.random(0, 255);
      bdata[i++] = (byte) 255;
    }
    // Noise data texture passsed as a texture.
    tex0.resize(context, GL2.GL_RGBA8, wh, wh, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 4, 1, bbuffer);
  }

  @Override
  public void load(LX lx, JsonObject obj) {
    super.load(lx, obj);
    loadShader(shaderFileKnob.getString());
  }

  protected void loadShader(String shaderFile) {
    if (toy != null) toy.release();  // release existing shader texture
    if (context != null) context.release();
    context = new DwPixelFlow(RainbowStudio.pApplet);
    // TODO(tracy): Handle file not found issue.
    toy = new DwShadertoy(context, "data/" + SHADER_DIR + shaderFile + ".frag");
  }

  public void draw(double drawDeltaMs) {
    pg.background(0);
    toy.set_iChannel(0, tex0);
    toy.set_iMouse(knob1.getValuef(), knob2.getValuef(), knob3.getValuef(), knob4.getValuef());
    toy.apply(toyGraphics);
    toyGraphics.loadPixels();
    toyGraphics.updatePixels();
    pg.image(toyGraphics, 0, 0);
    pg.loadPixels();
  }

  protected InputStream getFile() {
    return RainbowStudio.pApplet.createInput(this.shaderFileKnob.getString() + ".frag");
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
    knobsContainer.setPadding(0, 0, 0, 0);
    new UIKnob(fpsKnob).addToContainer(knobsContainer);
    new UIKnob(knob1).addToContainer(knobsContainer);
    new UIKnob(knob2).addToContainer(knobsContainer);
    new UIKnob(knob3).addToContainer(knobsContainer);
    new UIKnob(knob4).addToContainer(knobsContainer);
    knobsContainer.addToContainer(device);

    UI2dContainer filenameEntry = new UI2dContainer(0, 0, device.getWidth(), 30);
    filenameEntry.setLayout(UI2dContainer.Layout.HORIZONTAL);

    fileItemList =  new UIItemList.ScrollList(ui, 0, 5, CONTROLS_MIN_WIDTH, 80);
    new UITextBox(0, 0, device.getContentWidth() - 22, 20)
      .setParameter(shaderFileKnob)
      .setTextAlignment(PConstants.LEFT)
      .addToContainer(filenameEntry);


    // Button for reloading shader.
    new UIButton(device.getContentWidth() - 20, 0, 20, 20) {
      @Override
        public void onToggle(boolean on) {
        if (on) {
          loadShader(shaderFileKnob.getString());
        }
      }
    }
    .setLabel("\u21BA").setMomentary(true).addToContainer(filenameEntry);
    filenameEntry.addToContainer(device);

    // Button for editing a file.
    new UIButton(0, 24, device.getContentWidth(), 16) {
      @Override
        public void onToggle(boolean on) {
        if (on) {
          try (InputStream in = getFile()) {
            // TODO: Implement this
//            if (in == null) {
//              // For new files, copy the template in.
//              java.nio.file.Files.copy(new File(RainbowStudio.pApplet.dataPath("basic.frag")).toPath(),
//                shaderFile.toPath(),
//                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
//            }
//            java.awt.Desktop.getDesktop().edit(shaderFile);
          } catch (Throwable t) {
            System.err.println(t.getLocalizedMessage());
          }
        }
      }
    }
    .setLabel("Edit").setMomentary(true).addToContainer(device);

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
      shaderFileKnob.setValue(filename);
      loadShader(filename);
    }
  }
}
