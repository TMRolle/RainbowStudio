package org.rainbowstudio.patterns;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXPattern;
import heronarts.lx.color.LXColor;
import heronarts.lx.midi.MidiNote;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import org.rainbowstudio.model.RainbowBaseModel;

@LXCategory(LXCategory.FORM)
public class KeyboardMidiPP extends LXPattern {
  public final CompoundParameter brightnessKnob =
    new CompoundParameter("bright", 1.0, 100.0)
    .setDescription("Brightness");

  public final CompoundParameter keysKnob =
    new CompoundParameter("bars", 25, 88)
    .setDescription("Musical Keys");

  final int MIDDLEC = 60;
  heronarts.lx.midi.LXMidiOutput midiThroughOutput;
  heronarts.lx.midi.LXMidiInput midiThroughInput;

  Queue<Integer> keysPlayed = new LinkedList<Integer>();
  ArrayList<Integer> litColumns = new ArrayList<Integer>();

  public KeyboardMidiPP(LX lx) {
    super(lx);
    // Find target output for passing MIDI through
    heronarts.lx.midi.LXMidiEngine midi = lx.engine.midi;

    for (heronarts.lx.midi.LXMidiOutput output : midi.outputs) {
      System.out.println(output.getName() + ": " + output.getDescription());
      if (output.getName().equalsIgnoreCase("rainbowStudioOut")) {
        midiThroughOutput = output;
        midiThroughOutput.open();
      }
    }

    brightnessKnob.setValue(30);
    keysKnob.setValue(25);
    addParameter(brightnessKnob);
    addParameter(keysKnob);
  }

  public void run(double deltaMs) {

    int numCol = ((RainbowBaseModel)lx.model).pointsWide;
    int centerRainbow;
    int centerkeyboard;

    // Find center based on parity (odd/even)
    // To align cente of keyboard and rainbow
    // Note: Even centers are aligned to the left

    // Center of Rainbow
    if (numCol %2 == 0) {
      centerRainbow = (numCol/2);
    } else {
      centerRainbow = (numCol/2)+1;
    }

    // Center of Keyboard
    int numMidiKeys= (int)keysKnob.getValue();
    if (numMidiKeys %2 == 0) {
      centerkeyboard = (int)(numMidiKeys/2);
    } else {
      centerkeyboard = (int)(numMidiKeys/2)+1;
    }

    // Padding needed to fill up the rainbow
    int padding = 0;

    if (numMidiKeys < numCol) {
      padding = numCol/numMidiKeys;
    }

    // Find out which keys are displayed on the rainbow
    // And add any padding necessary
    for (int note : keysPlayed) {
      int litKeys;
      if (padding != 0) {
        litKeys = ((note - MIDDLEC)+centerkeyboard)*padding;
      } else {
        litKeys = (note - MIDDLEC)+centerRainbow;
      }
      litColumns.add(litKeys);
      for (int i = 0; i < padding; i++) {
        litColumns.add(litKeys + i);
      }
    }

    // Scan and light up points
    int pointNumber = 0;
    for (LXPoint p : model.points) {
      int colNumber = pointNumber % numCol;

      // Check for bad values
      if (numMidiKeys < 1) numMidiKeys = 1;

      // Light it up!
      if (litColumns.contains(colNumber)) {
        colors[p.index] = LXColor.hsb(colNumber, 100, 100);
      } else {
        colors[p.index] = LXColor.gray(brightnessKnob.getValue());
      }
      ++pointNumber;
    }

    // Refresh Columns to Light up for next round
    litColumns.clear();
  }

  // Map a note to a hue
  public void noteOnReceived(MidiNoteOn note) {

    // Collect all the Midi notes played
    int midiNote = note.getPitch();
    if ( midiNote >= 0 && midiNote <= 127) {
      keysPlayed.offer(midiNote);
    }
    if (midiThroughOutput != null) {
      midiThroughOutput.send(note);
    }
  }

  public void noteOffReceived(MidiNote note) {
    // Releasing any note will turn it off.  Multiple notes can be
    // on at once and to turn off when all notes are released we need
    // to track the notes on and only go black once we have received
    // note-off for all notes.

    // Remove all the Midi notes played
    int midiNote = note.getPitch();
    if ( midiNote >= 0 && midiNote <= 127) {
      try {
        keysPlayed.remove(midiNote);
      }
      catch (Exception e) {
        // Do nothing, keep operation going
      }
    }
    // Forward MIDI notes
    if (midiThroughOutput != null) {
      midiThroughOutput.send(note);
    }
  }
}