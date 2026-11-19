# The installed engine uses an independent classloader; its dependencies never enter host DEX.
-keep class top.lhlnb.easykiosk.EmbeddedChromiumComponentFactory { *; }
# The public-key instrumentation fixture exercises the same auth code as production.
-keep class top.lhlnb.easykiosk.AuthPolicy { *; }
-keep class top.lhlnb.easykiosk.Totp { *; }
-keep class top.lhlnb.easykiosk.NetworkTime { *; }
