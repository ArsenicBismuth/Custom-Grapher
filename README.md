# CustomGrapher
### Full Scheme
**MCU** (Fs = ~9000Hz)
Obtain HbO2 and Hb signal from sensor and transmit them both through a single signal.
1. Read analog data for Hb and HbO2 alternately
2. Noise & offset filtering
   - *yet*
4. Amplify XdB for better SNR
5. Modulate each using DSBAM
   - HbO2, ch1 (200Hz)
   - Hb, ch2 (500Hz)
6. Amplify XdB so that each has amplitude < half the full range
7. Mix ch1 & ch2 and transmit
   
**Phone** (Fs = 44100Hz)
Receive a single signal containing two channels through the audio jack.
1. Downsample by 21x   => 2100Hz
2. Separate channels
   - Separate HbO2, ch1 (200Hz): FIR, 210 \ 300 (Hz)   => 51st order
   - Separate Hb, ch2 (400Hz): FIR, 300 / 390 (Hz)   => 51st order
3. Demodulate each
   - Rectify (reqires the sampling rate to be more than 2x[2x] of all the carriers)
   - FIR, 30 \ 190 (Hz)   => 20th order
4. Downsample by 10x   => 210Hz
5. Noise & offset filtering
   - IIR, 0.01 / 0.05 9 \ 15 => 31st order