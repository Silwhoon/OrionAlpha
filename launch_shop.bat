@echo off
title OrionAlpha - Shop
set CLASSPATH=.;bin\target\OrionAlpha.jar
java -Xmx200m -Dwzpath=data/ -Dio.netty.leakDetection.level=advanced -XX:NewRatio=50 -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:+HeapDumpOnOutOfMemoryError -XX:-DisableExplicitGC -XX:+AggressiveOpts -Xnoclassgc -XX:+UseNUMA -XX:+UseFastAccessorMethods -XX:ReservedCodeCacheSize=48m -XX:MaxGCPauseMillis=300 -XX:GCPauseIntervalMillis=400 -XX:+UseTLAB -XX:+AlwaysPreTouch shop.ShopApp
pause