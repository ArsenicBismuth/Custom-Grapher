# CustomGrapher
### Full Scheme
**MCU** [Fs = ~200Hz]
Obtain HbO2 and Hb signal from sensor and transmit them both through a single signal.
Sampling may be done through interrupt for more accurate rate. Another way would be
doing it directly, and design filters which with cutoff deviation in mind.

Everything with "Test" tag shows parameter very likely to change in current implementation.
Current implementation specified as:

   - Single signal HbO2
   - No diode switching, but data input switching implemented
   
Downsample specs are written respective to previous condition.

[TODO] Test 2 hi-freq sinusoids, one is half amplitude the other

1. Read analog data for Hb and HbO2 alternately
   - 4 states for each cycle: R, off, IR, off
   - The off state can be used to inspect offset
   - Due to low order of the analog system, effect from
	 previous states would be short but still exist
2. Downsample by 4x => 50Hz (Simulating switching)
   - Since there're 4 input states, input downsampled 1x ==> 200Hz
3. [Removed] Noise filtering (Too many memory)
   - FIR, 12 \ ?, -30dB, 11th order => 12 \ 18
4. Downsample by 1x => 50Hz
4. Offset filtering
   - FIR, ? / 0.6, -30dB, 51st order => 0.11 / 0.6  @25Hz (Offset to 0.22 / 1.2 @50Hz)
5. Amplify 1x for better SNR
   - Signal Vpp must be less than 1/2 audio jack range for modulation purpose
6. Upsample to 100Hz & interpolate => Test: 4x prev or 1x total
   - Interpolation receives at min 2 data, and values as small as
     the interpolation rate to be created
6. Modulate each using DSBAM square carrier
   - HbO2, ch1 (70Hz)
   - Hb,   ch2 (100Hz)
   - Modulation index 100% (signal zero at 1/2 modulated signal peak)
7. Decrease 8x so that each has amplitude < half the full range
8. Mix ch1 & ch2 and transmit

Testing rate specs for the scheme abovet:
   
   - Common single loop speed (15/16) = ~2.86kHz - 6.6kHz
   - Slowest single loop speed (1/16) = ~300Hz - 370Hz
   - Average loop speed = ~2.7kHz - 6.2kHz
   - New data sampling rate = ~29Hz - 50Hz
   
**Phone** (Fs = 44100Hz)
Receive a single signal containing two channels through the audio jack.
1. Anti-aliasing filter, multi stage
   - FIR, -6dB @ 700Hz, 111th order => -0.3dB @ 210Hz
   - Downsample by 30 => 1470Hz
   - FIR, -6dB @ 240Hz, 61st order => -0.3dB @ 210Hz
1. Downsample by 3x => 490Hz
2. Separate channels
   - Separate HbO2, ch1 (100Hz): FIR, 110 \ 150, -60dB => 26th order
   - Separate Hb,   ch2 (200Hz): FIR, 150 / 190, -60dB => 28th order
3. Downsample by 2x => 245Hz
4. Demodulate each
   - Rectify (actually reqires the sampling rate to be more than 2x[2x] of all the carriers)
   - FIR, 5 \ 10, -40dB => 83th order
5. Downsample by 5x => 49Hz
6. Noise & offset filtering
   - Alternatives:
     - FIR, 0.2 / 0.4 8 \ 12, -60dB => 698th order => (X)
     - FIR, 0.1 / 0.6 8 \ 12, -30dB => 165th order
	 - FIR, 0.1 / 0.6 8 \ 12, -10dB => 85th order  => (X) Very big offset
   - Newer:
     - Offset: Copy Arduino Filter
7. Downsample by 7x => 7Hz
8. [Cancelled] BPM Calculation
   - FIR, ? \ 2, -30dB, 30th orrder => 1 \ 2
   - Check for rising zero-crossing, averaged at min every 4 waves
	 
**Extras**
Some very important information used during testings, some are just a cut out from above sections
thus might contain stuffs mentioned without context.
- A lot of them available either through commit descriptions or some notes scattered around the repo.
- Infinite Impulse Response Filter (IIR):
   - IIR, 0.3 / 0.8 5 \ 12 => (X) Unable to be converted to TF in Matlab
   - All IIR filter initially not normalized at 0dB: (switch b,a to sosMatrix for sos)
        [h,w] = freqz(b,a);    % frequency response of the filter H = b/a
        scale = 1/max(abs(h)); % scaling factor
        b = b*scale;           % scale*H, yes ONLY the b. Scaling a too will make it revert back.
   - Tried to cascade the tf, turns out the HPF side ruins everything. Using below methods;
        cFinal_Hbp_casc = filt(cFinal1_Hhp_b,cFinal1_Hhp_a) * filt(cFinal2_Hlp_b,cFinal2_Hlp_a);
        fvtool(cFinal_Hbp_casc.Numerator{1,1}, cFinal_Hbp_casc.Denominator{1,1},'Fs',210,'FrequencyScale',...
		'log','FrequencyRange','Specify freq. vector','FrequencyVector',[1e-3:0.1:1e3]);
- Adding new settings for the app:
   1. Add a new menu to a settings section in pref_generals.xml (for example) as in usual procedure
   2. Change the defaultValue
   3. For input-able settings (edit text, list, dialog, etc), bind the values inside SettingsActivity.java
   4. Pass the setting values to their respective variables to apply changes using sharedPref
- Adding new setting section for the app:
   1. Copy existing pref xml such as pref_general.xml and modify
   2. Add a new header inside pref_headers.xml
   3. Inside SettingsActivity.java, add similar code as its neighbor inside "isValidFragment" method
   4. Add another PreferenceFragment static class, again similar to its neighbor
- Signals characteristics
   - Square = ~10Hz components around specified freq & aliasing at x3, x5, etc
   - Square no-Alias = Similar with previous but the stepping adds 2 additional sample (@44.1kHz)
   - Sawtooth = ~10Hz components around specified freq & aliasing at x2, x3, etc
   - Sine = No aliasing