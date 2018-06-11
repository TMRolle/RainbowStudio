Rainbow Studio
==
Based on LX Studio, Prototype for [Rainbow Bridge](http://giantrainbow.com/)
==

**BY DOWNLOADING OR USING THE LX STUDIO SOFTWARE OR ANY PART THEREOF, YOU AGREE TO THE TERMS AND CONDITIONS OF THE [LX STUDIO SOFTWARE LICENSE AND DISTRIBUTION AGREEMENT](http://lx.studio/license).**

Please note that LX Studio is not open-source software. The license grants permission to use this software freely in non-commercial applications. Commercial use is subject to a total annual revenue limit of $25K on any and all projects associated with the software. If this licensing is obstructive to your needs or you are unclear as to whether your desired use case is compliant, contact me to discuss proprietary licensing: mark@heronarts.com

---

![Rainbow Studio](https://raw.github.com/tracyscott/RainbowStudio/master/assets/rainbowstudio.jpg)

[LX Studio](http://lx.studio/) is a digital lighting workstation, bringing concepts from digital audio workstations and modular synthesis into the realm of LED lighting control. Generative patterns, interactive inputs, and flexible parameter-driven modulation — a rich environment for lighting composition and performance.

### Getting Started ###

Rainbow Studio runs on top of Processing. [Download and install Processing &rarr;](https://processing.org/download/)

Next, clone this project and open the RainbowStudio.pde project in Processing:
```
$ cd ~/Documents/Processing
$ git clone https://github.com/tracyscott/RainbowStudio.git
```

The Animated GIF Pattern requires the Animated GIF Processing Library. You need to install it into your Processing libraries folder. You might need to mkdir ~/Documents/Processing/libraries.
```
$ cd ~/Documents/Processing/libraries
$ git clone https://github.com/01010101/GifAnimation.git
```

Also, the code is currently set up to render the rainbow points.  In order to test animated GIFs, you will need to change it to modelType = LARGE_PANEL in RainbowStudio.pde.  You should also load the AnimatedGifLife.lxp file once Rainbow Studio is running.  Animated GIFs are currently only supported for 2D grid models.

Consult the [LX Studio API reference &rarr;](http://lx.studio/api/)

More and better documentation is coming soon!

### Contact and Collaboration ###

Building a big cool project? I'm probably interested in hearing about it! Want to solicit some help, request new framework features, or just ask a random question? Open an issue on the project or drop me a line: mark@heronarts.com

---

HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE, WITH RESPECT TO THE SOFTWARE.
