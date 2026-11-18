# The installed engine uses an independent classloader; its dependencies never enter host DEX.
-keep class com.freekiosk.EmbeddedChromiumComponentFactory { *; }