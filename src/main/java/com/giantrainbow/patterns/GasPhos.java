/*
 * Created by shawn on 8/7/18 5:32 PM.
 */
package com.giantrainbow.patterns;

import static com.giantrainbow.RainbowStudio.GLOBAL_FRAME_RATE;
import static processing.core.PApplet.mag;
import static processing.core.PApplet.round;
import static processing.core.PApplet.sin;
import static processing.core.PConstants.HSB;
import static processing.core.PConstants.P2D;
import static processing.core.PConstants.PI;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Name: Gasoline phosphene.
 */
@LXCategory(LXCategory.FORM)
public class GasPhos extends PGPixelPerfect {
  private static final int MASK_SIZE = 7;
  private static final int DIFFUSION_KERNEL_SIZE = 3;

  private int setACount;
  private int setBCount;

  private float satMin;
  private float satDecay;
  private float satKickSize;
  private float satBoost;

  private float briMinDecay;
  private float briDecay;

  // State
  int screenSize = pg.width * pg.height;
  private float[] hue = new float[screenSize];
  private float[] sat = new float[screenSize];
  private float[] bri = new float[screenSize];
  private boolean[] particleMask = new boolean[MASK_SIZE * MASK_SIZE];
  private int[] particleMaskX = new int[particleMask.length];
  private int[] particleMaskY = new int[particleMask.length];
  private float[] diffusionKernel;
  private List<float[]> collisionKernels = new ArrayList<>(3);
  private List<Integer> collisionKernelSizes = new ArrayList<>(3);
  private List<ParticleSet> particleSets = new ArrayList<>(2);
  private float sigmaDecay;
  private float minSigmaI;
  private float sigmaIBoost;
  private float minSigmaJ;
  private float sigmaJBoost;

  // Working arrays
  private float[] workingHue = new float[screenSize];
  private float[] workingSat = new float[screenSize];
  private float[] workingBri = new float[screenSize];
  private float[] workingCell = new float[screenSize];
  private float[] workingScreen = new float[screenSize];

  private int frameCount;

  public GasPhos(LX lx) {
    super(lx, P2D);
  }

  @Override
  public void onActive() {
    fpsKnob.setValue(GLOBAL_FRAME_RATE);
    frameCount = 0;

    // Initial setup

    setACount = 30;
    setBCount = 30;

    satMin = 0.85f;
    satDecay = 0.95f;
    satKickSize = 1.0f - (0.75f*satMin);
    satBoost = 0.0f;

    briMinDecay = 0.990f;
    briDecay = 0.0f;

    // State

    sigmaDecay = 0.85f;

    minSigmaI = 0.5f;
    sigmaIBoost = 0;

    minSigmaJ = 0.5f;
    sigmaJBoost = 0;

    initState();
    cleanUp();

    pg.beginDraw();
    pg.colorMode(HSB, 1.0f);
    pg.endDraw();
  }

  @Override
  protected void draw(double deltaDrawMs) {
    update();
    boolean hasCollision = getStatesCell();

    briDecay *= 0.75f;
    satBoost *= satDecay;
    sigmaIBoost *= sigmaDecay;  // Code uses sigmaJBoost
    sigmaJBoost *= sigmaDecay;
    if (hasCollision) {
      briDecay += 0.060f;
      satBoost += satKickSize;
      sigmaIBoost += 10;
      sigmaJBoost += 2;
    }

    Arrays.fill(sat, satMin + satBoost);

    float targetHue = 0.5f + sin(PI * frameCount/200)/0.5f;
    for (int i = 0; i < screenSize; i++) {
      if (workingSat[i] != 0.0f) {
        hue[i] = workingHue[i];
        sat[i] = workingSat[i];
      } else {
        hue[i] += 0.015f*(targetHue - hue[i]);
      }
    }

    System.arraycopy(convolve(hue, pg.width, diffusionKernel, DIFFUSION_KERNEL_SIZE), 0,
        hue, 0, screenSize);
    System.arraycopy(convolve(sat, pg.width, diffusionKernel, DIFFUSION_KERNEL_SIZE), 0,
        sat, 0, screenSize);

    convolve(bri, pg.width, diffusionKernel, DIFFUSION_KERNEL_SIZE);
    for (int i = 0; i < screenSize; i++) {
      bri[i] = workingBri[i] + (briMinDecay + briDecay)*workingScreen[i];
    }
    if (hasCollision) {
      int k = random.nextInt(collisionKernels.size());
      convolve(workingCell, pg.width, collisionKernels.get(k), collisionKernelSizes.get(k));
      for (int i = 0; i < screenSize; i++) {
        bri[i] += workingScreen[i];
      }
    }

    pg.loadPixels();
    for (int i = 0; i < screenSize; i++) {
      pg.pixels[i] = pg.color(hue[i], sat[i], bri[i]);
    }
    pg.updatePixels();
  }

  // https://www.mathworks.com/help/matlab/ref/conv2.html?s_tid=doc_ta#bvgtfv6
  // workingScreen is expected to be the same size as a.
  private float[] convolve(float[] a, int aWidth, float[] b, int bWidth) {
    Arrays.fill(workingScreen, 0.0f);
    float[] c = workingScreen;

    int aHeight = a.length / aWidth;
    int bHeight = b.length / bWidth;
    int cIndex = 0;
    for (int j = 0; j < aHeight; j++) {
      for (int k = 0; k < aWidth; k++) {
        // Convolve this element
        float sum = 0.0f;
        int aIndex = 0;
        for (int p = 0; p < aHeight; p++) {
          for (int q = 0; q < aWidth; q++) {
            int by = j - p + 1;
            int bx = k - q + 1;
            if (0 <= by && by < bHeight && 0 <= bx && bx < bWidth) {
              sum += a[aIndex++] * b[(j - p + 1)*bWidth + (k - q + 1)];
            }
          }
        }
        c[cIndex++] = sum;
      }
    }
    return c;
  }

  private void initState() {
    Arrays.fill(hue, 0);
    Arrays.fill(sat, 0);
    Arrays.fill(bri, 0);

    int index = 0;
    int valY = -MASK_SIZE/2;
    for (int row = 0; row < 7; row++) {
      int valX = -MASK_SIZE/2;
      for (int col = 0; col < 7; col++) {
        particleMaskX[index] = valX;
        particleMaskY[index] = valY;
        particleMask[index] = mag(valX, valY) < 3.0f;
        valX++;
        index++;
      }
      valY++;
    }

    // Normalize the diffusion kernel
    diffusionKernel = new float[] {
        0.0f, 0.2f, 0.1f,
        0.1f, 1.0f, 0.0f,
        0.2f, 0.0f, 0.1f,
    };
    float sum = 0.0f;
    for (float f : diffusionKernel) {
      sum += f;
    }
    for (int i = 0; i < diffusionKernel.length; i++) {
      diffusionKernel[i] /= sum;
    }

    // Collision kernels
    collisionKernels.clear();
    collisionKernelSizes.clear();
    collisionKernels.add(new float[] {
        1, 0, 0, 0, 0, 1,
        0, 1, 0, 0, 1, 0,
        0, 0, 1, 1, 0, 0,
        0, 0, 1, 1, 0, 0,
        0, 1, 0, 0, 1, 0,
        1, 0, 0, 0, 0, 1,
    });
    collisionKernelSizes.add(6);
    collisionKernels.add(new float[] {
        1, 0, 0, 0, 0, 0, 0, 1,
        0, 1, 0, 0, 0, 0, 1, 0,
        0, 0, 1, 1, 1, 1, 0, 0,
        0, 0, 1, 1, 1, 1, 0, 0,
        0, 0, 1, 1, 1, 1, 0, 0,
        0, 0, 1, 1, 1, 1, 0, 0,
        0, 1, 0, 0, 0, 0, 1, 0,
        1, 0, 0, 0, 0, 0, 0, 1,
    });
    collisionKernelSizes.add(8);
    collisionKernels.add(new float[] {
        1, 0, 0, 0, 0, 0, 0, 0, 0, 1,
        0, 1, 0, 0, 0, 0, 0, 0, 1, 0,
        0, 0, 1, 1, 1, 1, 1, 1, 0, 0,
        0, 0, 1, 1, 1, 1, 1, 1, 0, 0,
        0, 0, 1, 1, 1, 1, 1, 1, 0, 0,
        0, 0, 1, 1, 1, 1, 1, 1, 0, 0,
        0, 0, 1, 1, 1, 1, 1, 1, 0, 0,
        0, 0, 1, 1, 1, 1, 1, 1, 0, 0,
        0, 1, 0, 0, 0, 0, 0, 0, 1, 0,
        1, 0, 0, 0, 0, 0, 0, 0, 0, 1,
    });
    collisionKernelSizes.add(10);

    // Particle sets
    particleSets.clear();
    particleSets.add(new ParticleSet(setACount, -50.0f, 0.0f, 1.0f, 2.0f));
    particleSets.add(new ParticleSet(setBCount, 50.0f, pg.width, -1.0f, -2.0f));
  }

  private void update() {
    float sigmaI = minSigmaI + sigmaIBoost;
    float sigmaJ = minSigmaJ + sigmaJBoost;

    for (ParticleSet p : particleSets) {
      p.update(sigmaI, sigmaJ);
    }
  }

  private boolean getStatesCell() {
    Arrays.fill(workingHue, 0.0f);
    Arrays.fill(workingSat, 0.0f);
    Arrays.fill(workingBri, 0.0f);
    Arrays.fill(workingCell, 0.0f);

    for (ParticleSet p : particleSets) {
      p.getStatesCell();
    }

    // Detect any collisions
    for (int i = 0; i < particleSets.size(); i++) {
      ParticleSet p1 = particleSets.get(i);
      for (int j = i + 1; j < particleSets.size(); j++) {
        ParticleSet p2 = particleSets.get(j);
        for (int a = 0; a < p1.count; a++) {
          for (int b = 0; b < p2.count; b++) {
            if (p1.coords[a*2] == p2.coords[b*2]
                && p1.coords[a*2 + 1] == p2.coords[b*2 + 1]) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private void cleanUp() {
    for (ParticleSet p : particleSets) {
      p.cleanUp();
    }
  }

  private final class ParticleSet {
    private final float initPosM;
    private final float initPosA;
    private final float initVelM;
    private final float initVelA;

    final int count;

    final float[] coords;
    final float[] vel;
    final float[] hue;
    final boolean[] isRenderable;
    final int[] timeOffScreen;
    final boolean[] killIdx;

    ParticleSet(int count, float initPosM, float initPosA, float initVelM, float initVelA) {
      this.count = count;
      coords = new float[2 * count];
      vel = new float[2 * count];
      hue = new float[count];
      isRenderable = new boolean[count];
      timeOffScreen = new int[count];
      killIdx = new boolean[count];

      this.initPosM = initPosM;
      this.initPosA = initPosA;
      this.initVelM = initVelM;
      this.initVelA = initVelA;
    }

    /**
     * Updates this particle set.
     */
    void update(float sigmaI, float sigmaJ) {
      int index = 0;
      for (int row = 0; row < count; row++) {
        coords[index] += vel[index] + (float) (sigmaI*random.nextGaussian());
        index++;
        coords[index] += vel[index] + (float) (sigmaJ*random.nextGaussian());
        index++;
      }

      for (int row = 0; row < count; row++) {
        int coordsRow = row * 2;
        boolean b =
            coords[coordsRow] >= pg.height
                || coords[coordsRow] < 0
                || coords[coordsRow + 1] >= pg.width
                || coords[coordsRow + 1] < 0;
        isRenderable[row] = !b;
        if (b) {
          timeOffScreen[row]++;
        }
        killIdx[row] |= timeOffScreen[row] > 100;
      }
    }

    void getStatesCell() {
      for (int coordsIndex = 0; coordsIndex < count; coordsIndex++) {
        if (!isRenderable[coordsIndex]) {
          continue;
        }
        int maskIndex = 0;
        for (int row = 0; row < MASK_SIZE; row++) {
          for (int col = 0; col < MASK_SIZE; col++, maskIndex++) {
            if (!particleMask[maskIndex]) {
              continue;
            }
            int i = round(coords[coordsIndex*2]) + particleMaskX[maskIndex];
            int j = round(coords[coordsIndex*2 + 1]) + particleMaskY[maskIndex];
            if (!(
                0 <= i && i < pg.height
                    && 0 <= j && j < pg.width
            )) {
              continue;
            }

            int screenIndex = i*pg.width + j;
            workingHue[screenIndex] = hue[coordsIndex];
            if (particleMaskX[maskIndex] == 0 && particleMaskY[maskIndex] == 0) {
              if (workingSat[screenIndex] > 0.0f) {
                workingCell[screenIndex] = 1.0f;
              }
            }
            workingSat[screenIndex] = 1.0f;
          }
        }
        workingBri[round(coords[coordsIndex*2])*pg.width + round(coords[coordsIndex*2 + 1])] = 1.0f;
      }
    }

    /**
     * Calculates the initial state wherever killIdx is true.
     */
    void cleanUp() {
      for (int i = 0; i < count; i++) {
        if (!killIdx[i]) {
          continue;
        }

        coords[i*2] = pg.height * random.nextFloat();
        coords[i*2 + 1] = initPosA + initPosM*random.nextFloat();

        isRenderable[i] = false;

        vel[i*2] = (float) (0.05 * random.nextGaussian());
        vel[i*2 + 1] = (float) (initVelA + initVelM*Math.abs(random.nextGaussian()));

        hue[i] = random.nextFloat();
        timeOffScreen[i] = 0;
        killIdx[i] = false;
      }
    }
  }
}


//function BurningManRainbowAnimations(FileName)
//FileName = 'test.gif';
//SaveFile = false;
//
//Height = 50;
//Width = 440;
//
//Dims = [Height,Width];
//
//SetACount = 30;
//SetBCount = 30;
//
//SMin = 0.85;
//SDecay = 0.95;
//SKickSize = 1-(0.75*SMin);
//ThisSBoost = 0;
//
//LMinDecay = 0.990;
//ThisLDecay = 0;
//
//StateObject = InitStateObject(Height,Width,SetACount,SetBCount);
//StateObject = CleanupParticles(StateObject);
//
//
//StateObject.SigmaDecay = 0.85;
//
//StateObject.MinSigmaI = .5;
//StateObject.SigmaIBoost = 0;
//
//StateObject.MinSigmaJ = 0.5;
//StateObject.SigmaJBoost = 0;
//
//while true;% StateObject.Frame <= 250
//
//    StateObject.Frame = StateObject.Frame + 1;
//    StateObject = UpdateParticles(StateObject);
//    ThisStatesCell = GetStatesCell(StateObject);
//    NewState = zeros(StateObject.Height,StateObject.Width,3);
//
//    [H,S,L,C,HasCollision] = GetStatesCell(StateObject);
//    ThisLDecay = 0.060 .* HasCollision + 0.75 .* ThisLDecay;
//
//    ThisSBoost = ThisSBoost * SDecay + SKickSize * HasCollision;
//    ThisS = SMin + ThisSBoost;
//
//    StateObject.SigmaIBoost = StateObject.SigmaDecay*StateObject.SigmaJBoost +10.*HasCollision;
//    StateObject.SigmaJBoost = StateObject.SigmaDecay*StateObject.SigmaJBoost + 2.*HasCollision;
//
//
//    TargetHue = 0.5 +sin(pi.*StateObject.Frame/200)/0.5;
//    HueDiff = TargetHue - StateObject.State.H;
//    StateObject.State.H = StateObject.State.H + HueDiff.*0.015;
//    StateObject.State.H(logical(S)) = H(logical(S));
//
//    StateObject.State.S(logical(S)) = S(logical(S));
//%      RGBFrame = hsl2rgb(cat(3, StateObject.State.H, ThisS.*ones(size(H)), 0.5.*ones(size(H))));
//%      imagesc(RGBFrame(10:40,10:430,:))
//%      drawnow
//
//
//
//
//    StateObject.State.H = conv2(StateObject.State.H,StateObject.DiffusionKernel,'same');
//    StateObject.State.S = conv2(StateObject.State.S,StateObject.DiffusionKernel,'same');
//
//    if HasCollision
//    	StateObject.State.L = L + ( LMinDecay  + ThisLDecay).* + conv2(StateObject.State.L,StateObject.DiffusionKernel,'same') + conv2(C,StateObject.CollisionKernel{datasample(numel(StateObject.CollisionKernel),1)},'same');
//    else
//    	StateObject.State.L = L + ( LMinDecay  + ThisLDecay).* + conv2(StateObject.State.L,StateObject.DiffusionKernel,'same');
//    end
//    RGBFrame = hsl2rgb(cat(3, StateObject.State.H, ThisS.*ones(size(H)), StateObject.State.L));
//    image(RGBFrame(10:40,10:430,:))
//    set(gca,'Position', [0,0,1,1])
//    drawnow
//%    imagesc(StateObject.State.H)
//%    drawnow
//    StateObject = CleanupParticles(StateObject);
//
//    if SaveFile
//      frame = getframe(gcf);
//      im = frame2im(frame);
//      [imind,cm] = rgb2ind(im,256);
//
//      if StateObject.Frame == 1
//          imwrite(imind,cm,FileName,'gif', 'Loopcount',inf,'DelayTime',1/12);
//      else
//          imwrite(imind,cm,FileName,'gif','WriteMode','append','DelayTime',1/12);
//      end
//    end
//end
//
//end
//
//function StateObject =  InitStateObject(Height,Width,SetACount,SetBCount)
//
//StateObject.Frame = 0;
//StateObject.Height = Height;
//StateObject.Width = Width;
//StateObject.State.H = zeros([StateObject.Height,StateObject.Width]);
//StateObject.State.S = zeros([StateObject.Height,StateObject.Width]);
//StateObject.State.L = zeros([StateObject.Height,StateObject.Width]);
//
//[ParticleMaskX,ParticleMaskY] = meshgrid(linspace(-3,3,7),linspace(-3,3,7));
//
//StateObject.ParticleMask = sqrt(ParticleMaskX.^2 + ParticleMaskY.^2) < 3;
//StateObject.ParticleMaskX = ParticleMaskX;
//StateObject.ParticleMaskY = ParticleMaskY;
//
//StateObject.DiffusionKernel = [0,0.2,0.1;...
//                               0.1,1,0;...
//                               0.2,0,0.1];
//
//StateObject.DiffusionKernel = StateObject.DiffusionKernel./sum(StateObject.DiffusionKernel(:));
//
//StateObject.CollisionKernel{1} =  eye(6) | flipud(eye(6));
//StateObject.CollisionKernel{1}(3:4,3:4)= 1;
//
//StateObject.CollisionKernel{2} =  eye(8) | flipud(eye(8));
//StateObject.CollisionKernel{2}(3:6,3:6)= 1;
//
//StateObject.CollisionKernel{3} =  eye(10) | flipud(eye(10));
//StateObject.CollisionKernel{3}(3:8,3:8)= 1;
//
//
//%%
//StateObject.ParticleSets(1) = struct('Params',{[]}','Coords',{zeros([SetACount,2])},'Vel',{zeros([SetACount,2])},'Hue',{zeros([SetACount,1])},'IsRenderable',{false([SetACount,1])},'TimeOffScreen',{zeros([SetACount,1])},'KillIDX',true([SetACount,1]),'GetInitPosition',@(x) cat(2,Height.*rand([x,1]),-50.*rand([x,1])),'GetInitVelocity',@(x) cat(2,normrnd(0,0.05,[x,1]),2+abs(normrnd(0,1,[x,1]))));
//StateObject.ParticleSets(2) = struct('Params',{[]}','Coords',{zeros([SetBCount,2])},'Vel',{zeros([SetBCount,2])},'Hue',{zeros([SetACount,1])},'IsRenderable',{false([SetACount,1])},'TimeOffScreen',{zeros([SetBCount,1])},'KillIDX',true([SetBCount,1]),'GetInitPosition',@(x) cat(2,Height.*rand([x,1]),Width+50.*rand([x,1])),'GetInitVelocity',@(x) cat(2,normrnd(0,0.05,[x,1]),-(2+abs(normrnd(0,1,[x,1])))));
//
//
//end
//
//
//
//function StateObject = UpdateParticles(StateObject)
//
//SigmaI = StateObject.MinSigmaI + StateObject.SigmaIBoost;
//Sigmaj = StateObject.MinSigmaJ + StateObject.SigmaJBoost;
//
//for SetIDX = 1:numel(StateObject.ParticleSets)
//
//    ThisParticleSet = StateObject.ParticleSets(SetIDX);
//    ThisParticleSet.Coords = ThisParticleSet.Coords + ...
//                             ThisParticleSet.Vel + ...
//                             [normrnd(0,SigmaI,[size(ThisParticleSet.Vel,1),1]),normrnd(0,Sigmaj,[size(ThisParticleSet.Vel,1),1])];
//
//    ThisInvalidPositionIDX = ThisParticleSet.Coords(:,1) > StateObject.Height | ...
//                             ThisParticleSet.Coords(:,1) < 0.5 | ...
//                             ThisParticleSet.Coords(:,2) > StateObject.Width | ...
//                             ThisParticleSet.Coords(:,2) < 0.5;
//
//    ThisParticleSet.IsRenderable = ~ThisInvalidPositionIDX;
//    ThisParticleSet.TimeOffScreen = ThisParticleSet.TimeOffScreen + ThisInvalidPositionIDX;
//
//    ThisParticleSet.KillIDX = ThisParticleSet.KillIDX | ThisParticleSet.TimeOffScreen > 100;
//
//    StateObject.ParticleSets(SetIDX) = ThisParticleSet;
//
//end
//end
//
//function StateObject = CleanupParticles(StateObject)
//
//for SetIDX = 1:numel(StateObject.ParticleSets)
//    ThisParticleSet = StateObject.ParticleSets(SetIDX);
//    ThisKillCounts = nnz(ThisParticleSet.KillIDX);
//    if ThisKillCounts > 0
//        ThisParticleSet.Coords(ThisParticleSet.KillIDX,:) = ThisParticleSet.GetInitPosition(ThisKillCounts);
//        ThisParticleSet.IsRenderable(ThisParticleSet.KillIDX) = false([ThisKillCounts,1]);
//        ThisParticleSet.Vel(ThisParticleSet.KillIDX,:) = ThisParticleSet.GetInitVelocity(ThisKillCounts);
//        ThisParticleSet.Hue(ThisParticleSet.KillIDX) = rand([ThisKillCounts,1]);
//        ThisParticleSet.TimeOffScreen(ThisParticleSet.KillIDX,:) = 0;
//        ThisParticleSet.KillIDX(ThisParticleSet.KillIDX,:) = false;
//        ThisParticleSet.KillIDX(ThisParticleSet.KillIDX,:) = false;
//    end
//
//    StateObject.ParticleSets(SetIDX) = ThisParticleSet;
//end
//end
//
//function [H,S,L,C,HasCollision] = GetStatesCell(StateObject)
//
//H = zeros(StateObject.Height,StateObject.Width);
//S = zeros(StateObject.Height,StateObject.Width);
//L = zeros(StateObject.Height,StateObject.Width);
//C = false(StateObject.Height,StateObject.Width);
//HasCollision = false;
//
//RoundCorrCell = {};
//
//for SetIDX = 1:numel(StateObject.ParticleSets)
//
//    ThisParticleSet = StateObject.ParticleSets(SetIDX);
//    ThisHuesVector = StateObject.ParticleSets(SetIDX).Hue;
//    ThisRoundCoords = round(ThisParticleSet.Coords);
//    RoundCorrCell{SetIDX} = ThisRoundCoords;
//
//    for CoordIDX = 1:size(ThisRoundCoords,1)
//        if ThisParticleSet.IsRenderable(CoordIDX)
//
//            for MaskI = 1:size(StateObject.ParticleMask,1)
//                for MaskJ = 1:size(StateObject.ParticleMask,1)
//                    if StateObject.ParticleMask(MaskI,MaskJ)
//
//                        ThisI = ThisRoundCoords(CoordIDX,1) + StateObject.ParticleMaskX(MaskI,MaskJ);
//                        ThisJ = ThisRoundCoords(CoordIDX,2) + StateObject.ParticleMaskY(MaskI,MaskJ);
//
//                        if ThisI > 0 && ...
//                           ThisI <= StateObject.Height && ...
//                           ThisJ > 0 && ...
//                           ThisJ <= StateObject.Width
//
//
//                            H(ThisI,ThisJ) = ThisHuesVector(CoordIDX);
//
//                            if StateObject.ParticleMaskX(MaskI,MaskJ) == 0 && ...
//                               StateObject.ParticleMaskY(MaskI,MaskJ) == 0
//                                if S(ThisI,ThisJ) == true
//                                    C(ThisI,ThisJ) = true;
//
//
//                                end
//                            end
//                            S(ThisI,ThisJ) = true;
//                        end
//                    end
//                end
//            end
//
//        	%H(ThisRoundCoords(CoordIDX,1),ThisRoundCoords(CoordIDX,2)) = ThisHuesVector(CoordIDX);
//            %S(ThisRoundCoords(CoordIDX,1),ThisRoundCoords(CoordIDX,2)) = true;
//            L(ThisRoundCoords(CoordIDX,1),ThisRoundCoords(CoordIDX,2)) = true;
//        end
//    end
//
//end
//HasCollision = any(ismember(RoundCorrCell{1},RoundCorrCell{2},'rows') );
//
//end