# CustomGrapher
### Full Scheme
**MCU** (Fs = ~9000Hz)
Obtain HbO2 and Hb signal from sensor and transmit them both through a single signal.
Sampling may be done through interrupt for more accurate rate. Another way would be
doing it directly, and design filters which with cutoff deviation in mind.

Everything with "Test" tag shows parameter very likely to change in current implementation.
Current implementation specified as:

   - Single signal HbO2
   - No diode switching, but data input switching implemented

Downsample specs are written respective to previous condition.

1. Read analog data for Hb and HbO2 alternately
   - 4 states for each cycle: R, off, IR, off
   - The off state can be used to inspect offset
   - Due to low order of the analog system, effect from
	 previous states would be short but still exist
2. Test: Downsample by 8x => 1125Hz (Simulating switching)
   - Since there're 4 input states, total downsample will be 2x ==> 4500Hz
3. Noise filtering
   - FIR, 12 \ ?, -30dB, 33nd order => 12 \ 30
4. Test: Downsample by 15x => 75Hz
4. Offset filtering
   - FIR, 8 \ 12, -30dB => 24th order
5. Test: Amplify 2x for better SNR
   - Signal Vpp must be less than 1/2 audio jack range for modulation purpose
6. Upsample to 1500Hz & interpolate => Test: 20x prev or 1/6x total
   - Interpolation receives at min 2 data, and values as small as
     the interpolation rate to be created
   - Test: No interpolation
6. Modulate each using DSBAM
   - HbO2, ch1 (200Hz)
   - Hb, ch2 (500Hz)
   - Test: Modulation index 100% (signal zero at 1/2 modulated signal peak)
7. Decrease Xx so that each has amplitude < half the full range
8. Mix ch1 & ch2 and transmit
   
**Phone** (Fs = 44100Hz)
Receive a single signal containing two channels through the audio jack.
1. Downsample by 21x   => 2100Hz
2. Separate channels
   - Separate HbO2, ch1 (200Hz): FIR, 210 \ 300 (Hz) => 51st order
   - Separate Hb, ch2 (400Hz):   FIR, 300 / 390 (Hz) => 51st order
3. Demodulate each
   - Rectify (reqires the sampling rate to be more than 2x[2x] of all the carriers)
   - FIR, 30 \ 190 (Hz)   => 20th order2
4. Downsample by 30x   => 70Hz
5. Noise & offset filtering
   - IIR, 0.3 / 0.8 5 \ 12 => (X) Unable to be converted to TF in Matlab
   - Stage 1: IIR, 0.3 / 0.6, Chebyshev I => 8th order
   - Stage 2: IIR,   5 \ 12  => 8th order
   - All IIR filter initially not normalized at 0dB: (switch b,a to sosMatrix for sos)
        [h,w] = freqz(b,a);    % frequency response of the filter H = b/a
        scale = 1/max(abs(h)); % scaling factor
        b = b*scale;           % scale*H, yes ONLY the b. Scaling a too will make it revert back.
   - Tried to cascade the tf, turns out the HPF side ruins everything. Using below methods;
        cFinal_Hbp_casc = filt(cFinal1_Hhp_b,cFinal1_Hhp_a) * filt(cFinal2_Hlp_b,cFinal2_Hlp_a);
        fvtool(cFinal_Hbp_casc.Numerator{1,1}, cFinal_Hbp_casc.Denominator{1,1},'Fs',210,'FrequencyScale','log','FrequencyRange','Specify freq. vector','FrequencyVector',[1e-3:0.1:1e3]);
   - Alternatives:
     - FIR, 0.2 / 0.4 8 \ 12, -60dB => 698th order => (X)
     - FIR, 0.3 / 0.5 8 \ 12, -60dB => 713th order => (X)
     - FIR, 0.1 / 0.6 8 \ 12, -60dB => 283th order => (X)
     - FIR, 0.1 / 0.6 8 \ 12, -30dB => 165th order
	 - FIR, 0.1 / 0.6 8 \ 12, -10dB => 85th order  => (X) Very big offset