#include <Wire.h>
#include <Adafruit_MCP4725.h>
#include "Filter.h"

#define FS 9000
#define FSC 1500    // Data rate at the time for modulation

// Carriers
#define FC1 = 200;
#define FC2 = 500;

Adafruit_MCP4725 dac;

// Filter Coefficients (from MATLAB
// LPF
float bNoise[33] = {-0.00403470692324842, 0.0182225145403899, 0.0131899782039394, 0.0149826746343488, 0.0183062083083345, 0.0221051451337311, 0.0261117305545169, 0.0301973612472578, 0.0342530062990987, 0.0381697736260240, 0.0418386379590894, 0.0451539032847044, 0.0480173236758640, 0.0503421327520257, 0.0520566797183028, 0.0531074708780736, 0.0534614599996321, 0.0531074708780736, 0.0520566797183028, 0.0503421327520257, 0.0480173236758640, 0.0451539032847044, 0.0418386379590894, 0.0381697736260240, 0.0342530062990987, 0.0301973612472578, 0.0261117305545169, 0.0221051451337311, 0.0183062083083345, 0.0149826746343488, 0.0131899782039394, 0.0182225145403899, -0.00403470692324842};
// HPF
float bNorm[24] = {0.00892989819282400, 0.0333839577844101, -0.00240756368983904, -0.0152616506593997, -0.0333156498206383, -0.0289015336720673, 0.00253344956193304, 0.0506090102439507, 0.0848875699648763, 0.0645789814297148, -0.0619687093364281, -0.579666988755414, 0.579666988755414, 0.0619687093364281, -0.0645789814297148, -0.0848875699648763, -0.0506090102439507, -0.00253344956193304, 0.0289015336720673, 0.0333156498206383, 0.0152616506593997, 0.00240756368983904, -0.0333839577844101, -0.00892989819282400};

// Downsampling rate are specified relative to previous process
// Upsampling rate are specified as absolute to FS or main loop rate
#define DS0 2                       // Downsampling rate FS0 = FS / DS1
Filter filNoiseA(32, bNoise);       // Remember b's passed by reference
Filter filNoiseB(32, bNoise);
Filter filNormA(32, bNorm);
Filter filNormB(32, bNorm);
#define DS2 15
#define US3 FS/FSC                  // Actually used for upsampling, but still < FS
#define MAXINDEX DS0 * DS2 * US3    // Used to reset the iterator, avoiding overflow

// Var
int input = 0;
int output = 0;
float ch1;
float ch2;
long n = 0;

void setup() {
    Serial.begin(115200);
    pinMode(5, OUTPUT);
    dac.begin(0x62);
}

void loop() {
    input = analogRead(A2);

    // Data mapping & downsampling, add data only every x indexes
    // TODO electrical settings on switching
    if (n % DS0 == 0) {
        if (n % 4 == 0) {
            // HbO2
            filNoiseA.addVal(input);
        } else if (n % 4 == 1) {
            // Off
        } else if (n % 4 == 2) {
            // Hb
            filNoiseA.addVal(input);
        } else if (n % 4 == 3) {
            // Off
        }
    }
    if (n % (DS0 * DS2) == 0) {
        filNormA.addVal(filNoiseA.getVal());
        filNormB.addVal(filNoiseB.getVal());
    }

    // Upsampling, value relative to FS or main loop rate
    if (n % US3 == 0) {
        // The rate at which modulation updated
        ch1 = filNormA.getVal();
        ch2 = filNormB.getVal();
    }

//    // Should be 10-bit ADC (1024) => 12-bit DAC (4096) for 5V => 5V
//    // But turns out 5V (4096) is too high for audio jack input
//    output = filNoiseA.getVal() * 2;
//                                                
//    Serial.print(input);
//    Serial.print(" ");
//    Serial.print(filNoiseA.getVal());
//
//    output *= cos(2 * PI * fc * n / FS) * 0.5;
//    output += 2048;                             // Offset to prevent negative
//    if (output <= 0) output = 0;
//
//    Serial.print(" ");
//    Serial.println(output);
//
//    dac.setVoltage(output, false);
    
    n++;
}
