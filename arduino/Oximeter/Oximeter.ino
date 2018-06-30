#include <Wire.h>
#include <Adafruit_MCP4725.h>
#include "Filter.h"

#define FS 9000
#define FSC 1500    // Data rate at the time for modulation

// Carriers
#define FCA 200
#define FCB 500

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
#define US3 20
#define MAXINDEX (DS0 * 4L * DS2 * (FS/FCA) * (FS/FCB))     // Used to reset the iterator, avoiding overflow

// Var
int input;
int inA;
int inB;
int inOff;
float chA = 0;
float chB = 0;
float filA = 0;
float filB = 0;
float interA = 0;
float interB = 0;
float distA = 0;
float distB = 0;
int output = 0;
long n = 1;     // Global index
unsigned long t0, t1, t2, t3, t4, t5;

void setup() {
    Serial.begin(115200);
    pinMode(5, OUTPUT);
    dac.begin(0x62);
}

void loop() {
    t0 = micros();
    input = analogRead(A2);
    
    t1 = micros();

    // Data mapping & downsampling, add data only every x indexes
    // TODO electrical settings on switching
    if (n % DS0 == 0) {
        if (n % 4 == 0) {
            // HbO2
            inA = input;
            filNoiseA.addVal(input);
        } else if (n % 4 == 1) {
            // Off
            inOff = input;
        } else if (n % 4 == 2) {
            // Hb
            inB = input;
            filNoiseB.addVal(input);
        } else if (n % 4 == 3) {
            // Off
            inOff = input;
        }
    }

    t2 = micros();

    // Filtering
    if (n % (DS0 * DS2 * 4) == 0) {
        filNormA.addVal(filNoiseA.getVal());
        filNormB.addVal(filNoiseB.getVal());

        // Set interpolation start as the prev result
        interA = filA;
        interB = filB;
        
        filA = filNormA.getVal();
        filB = filNormB.getVal();

        distA = filA - interA;
        distB = filB - interB;
    }

    t3 = micros();

    // Linear interpolation
    if (n % (DS0 * DS2 * 4 / US3) == 0) {
        interA += distA / US3;
        interB += distB / US3;
    }

    t4 = micros();

    // Modulation
    chA = interA * cos(2 * PI * FCA * n / FS);
    chB = interB * cos(2 * PI * FCB * n / FS);

    t5 = micros();

    output = chA + chB;
//    dac.setVoltage(output, false);

    if (n <= MAXINDEX) {
        n++;
    } else {
        n = 1;
    }

//    Serial.println((String)(-n/200.0f) + "\t" + (t1 - t0) + "\t" + (t2 - t1) + "\t" + (t3 - t2) + "\t" + (t4 - t3) + "\t" + (t5 - t4) + "\t" + (t5 - t0));
    Serial.println((String)(-n/200.0f) + " " + inA + " " + inB + " " + inOff + " " + filA + " " + filB + " " + interA + " " + interB + " " + chA + " " + chB + " " + output);
}
