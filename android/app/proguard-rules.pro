# The installed engine uses an independent classloader; its dependencies never enter host DEX.
-keep class top.lhlnb.easykiosk.EmbeddedChromiumComponentFactory { *; }