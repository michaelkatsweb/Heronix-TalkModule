Heronix TalkModule - Notification Sounds
=========================================

This directory contains sound files for alert notifications.

Required Files:
---------------
1. emergency.wav - Critical emergency alert sound (loud, attention-grabbing)
2. urgent.wav    - Urgent alert sound (moderately attention-grabbing)
3. notification.wav - Standard notification sound (subtle)

Sound Specifications:
---------------------
- Format: WAV (PCM, uncompressed)
- Sample Rate: 44100 Hz (recommended)
- Bit Depth: 16-bit
- Channels: Stereo or Mono
- Duration: 1-3 seconds recommended

Suggested Sources:
------------------
- https://freesound.org/ (Creative Commons sounds)
- https://mixkit.co/free-sound-effects/ (Free for commercial use)
- Windows System Sounds (C:\Windows\Media\)

Installation:
-------------
1. Download or create appropriate WAV files
2. Rename them to: emergency.wav, urgent.wav, notification.wav
3. Place them in this directory
4. Restart the application

The application will function without these files, but no sounds will play
for alerts. A warning will be logged if sounds are missing.
