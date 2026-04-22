#!/bin/zsh
# Guardian Watch build environment — source this (or use `. envrc.sh`) to get JDK/SDK/Gradle on PATH.
# Safe to source multiple times.
export JAVA_HOME="$HOME/.local/opt/jdk-17.0.19+10"
export ANDROID_SDK_ROOT="$HOME/.local/opt/android-sdk"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export GRADLE_BOOTSTRAP="$HOME/.local/opt/gradle-8.10.2"
case ":$PATH:" in
  *":$JAVA_HOME/bin:"*) ;;
  *) export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$GRADLE_BOOTSTRAP/bin:$PATH" ;;
esac
