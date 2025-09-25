#!/bin/bash

# Android SDK Setup for RideLink Development
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools

echo "Starting Android Emulator for RideLink..."
echo "Available AVDs:"
$ANDROID_HOME/emulator/emulator -list-avds

echo "\nStarting Medium_Phone_API_35..."
$ANDROID_HOME/emulator/emulator -avd Medium_Phone_API_35 -no-snapshot-load &

echo "Emulator started in background. You can now build and install the RideLink app."
echo "To install the app, run: ./gradlew installDebug"