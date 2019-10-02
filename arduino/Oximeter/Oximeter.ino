#include <Wire.h>
#include <Adafruit_MCP4725.h>
#include "Filter.h"
#include <TimerOne.h>

Adafruit_MCP4725 dac;
#define SERIAL_BUFFER_SIZE 32

/* Due to SRAM limitation, combined order all filters of more than ~2x60 will induce some array to be filled with zeroes.
 * Meanwhile combined order of more than ~2x70 will make the MCU not even working.
 * Alternative would be using PROGMEM, which is the flash memory.
 * SRAM: 2kB, thus for every order in a filter, space needed for even
 * the array = 4 byte * 2 (ch) * 2 (coefs & inputs) * 2 (if iir).
 */

// Filter Coefficients (from MATLAB
// LPF
//float bNoise[11] = {0.0104163064372188, 0.0565664602816431, -0.0480254229384230, -0.0853677562092125, 0.294078704296918, 0.602164544048249, 0.294078704296918, -0.0853677562092125, -0.0480254229384230, 0.0565664602816431, 0.0104163064372188};
// HPF
float bNorm[51] = {0.00412053114641298, -0.0312793191356909, -0.0115236109767417, -0.0100213992087269, -0.0108385904761499, -0.0119734717946732, -0.0131685337169099, -0.0143842189071186, -0.0156078845365078, -0.0168299462147875, -0.0180409785596737, -0.0192314427835679, -0.0203917450600907, -0.0215123386063274, -0.0225838323470475, -0.0235970999596655, -0.0245433871199924, -0.0254144153854427, -0.0262024812734020, -0.0269005491468212, -0.0275023365897874, -0.0280023910540817, -0.0283961566788859, -0.0286800303246559, -0.0288514060155109, 0.971091292850008, -0.0288514060155109, -0.0286800303246559, -0.0283961566788859, -0.0280023910540817, -0.0275023365897874, -0.0269005491468212, -0.0262024812734020, -0.0254144153854427, -0.0245433871199924, -0.0235970999596655, -0.0225838323470475, -0.0215123386063274, -0.0203917450600907, -0.0192314427835679, -0.0180409785596737, -0.0168299462147875, -0.0156078845365078, -0.0143842189071186, -0.0131685337169099, -0.0119734717946732, -0.0108385904761499, -0.0100213992087269, -0.0115236109767417, -0.0312793191356909, 0.00412053114641298};
// Difference
//float bNorm[] = {0.5, -0.5};

// Down/upsampling rate, specified relative to previous process
#define DS0 1   // Downsampling rate FS0 = FS / DS1, for input
#define DS1 4   // Downsampling due to switching of 4 states

// Remember b's passed by reference
//Filter filNoiseA(sizeof(bNoise)/sizeof(float), bNoise);
//Filter filNoiseB(sizeof(bNoise)/sizeof(float), bNoise);

#define DS2 1   // Downsampling for precise offset-suppression
Filter filNormA(sizeof(bNorm)/sizeof(float), bNorm);
Filter filNormB(sizeof(bNorm)/sizeof(float), bNorm);

#define US3 4   // Upsampling for interpolation

#define MAXINDEX (DS0 * DS1 * DS2)       // Used to reset the global iterator, avoiding overflow

/*  Both FS and USO can be focused to obtain:
 *  - FS:  More detailed data, including the carrier, but less room for interpolation
 *  - USO: Less blocky output, but will reduce loop speed FS and thus output triangluar data rather than sinusoidal
 */
#define FS  990 // Loop rate (Hz)
#define FSI 1000 // Interrupt rate (Hz), somehow multiple of carrier freq produce a big low-freq noise
#define FSC (FS / DS0 / DS1 / DS2 * US3) // Data rate at the time for modulation

// Carriers
#define FCA 100
#define FCB 200

// Value of square carrier iterator until they're switched
#define FLIPA (FSI / FCA)
#define FLIPB (FSI / FCB)

// Negative limit for modulation
#define NEG 100

// Bottom limit at output
#define BOTTOM 10

// Var
int input;
int inA;
int inB;
int inOff;
float dsA = 0;
float dsB = 0;
float filA = 0;
float filB = 0;
float interA = 0;
float interB = 0;
float distA = 0;
float distB = 0;
float chA = 0;
float chB = 0;
short carrA = 1;
short carrB = 1;
int output = 0;
int n = 1;      // Global index
int mA = 1;     // Index used for cosine
int mB = 1;     // Index used for cosine
unsigned long t0, t1, t2, t3, t4, t5, t6;

void result() {
    sei();  // To enable I2C inside ISR
    
    // Modulation
    carrA = (mA <= FLIPA / 2) ? 1 : -1;
    carrB = (mB <= FLIPB / 2) ? 1 : -1;
    
    // Currently MESSAGE is [-100, 100], converted to [0, 200]
    // Thus modulated at [-200, 200]
    chA = ((interA > -NEG) ? (NEG + interA) : (0)) * carrA;
    chB = ((interB > -NEG) ? (NEG + interB) : (0)) * carrB;

    // Combine, creating wave with range of [-200, 200]
//    output = (chA + chB) / 2;
    output = chA;

    // Final output handling    
    // Amplify & offset to maximize range & avoid negatives
    /* Data about the range:
     *  - For a full range wave [ -1, 1] in Audacity, phone data is good at ~50% vol
     *  - For a half-range wave [-.5,.5] in Audacity, phone data is good at ~80-90% vol
     *  - DAC sine with amp of 512 (4096 / 8, or 0,625 Vp) will result in a very little clipping
     *  - Currently combined output is [0, 400] for dual ch
     *  - Audiacity full-range wave at 10% vol playback equals to a full-range wave at recording
     *  - Optimal conclusion: 50% Audacity playback => 5% Audacity record => 25.6 Arduino DAC (3mVp)
     */
    int temp = (output + 2*NEG) / 8 + BOTTOM;      // [-200, 200] => [0, 400]
    
    if (temp > BOTTOM) {
        dac.setVoltage(temp, false);
    } else {
        dac.setVoltage(BOTTOM, false);
    }

    // Carrier iterator
    if (mA < FLIPA) {
        mA++;
    } else {
        mA = 1;
    }

    if (mB < FLIPB) {
        mB++;
    } else {
        mB = 1;
    }

    // Only max 1 or 2 serial prints may be activated
//    Serial.print(inA); Serial.print(" ");
//    Serial.print(filA); Serial.print(" ");
    Serial.print(interA); Serial.print(" ");
//    Serial.print(interO);  Serial.print(" ");
    Serial.println();
}

void setup() {
    Serial.begin(250000);
    pinMode(5, OUTPUT);
    dac.begin(0x62);

    Timer1.initialize(1000000 / FSI);   // Time in us
    Timer1.attachInterrupt(result);     // Do modulation & final output at 1kHZ
}

void loop() {
    t0 = micros();
    input = analogRead(A2);
    
    t1 = micros();

    // Data mapping & downsampling, add data only every x indexes
    // TODO electrical settings on switching
    if (modulo(n, DS0) == 0) {
        if (modulo(n, DS0 * DS1) == 0) {
            // HbO2
            inA = input;
//            filNoiseA.addVal(inA);
        } else if (modulo(n, DS0 * DS1) == 1) {
            // Off
            inOff = input;
        } else if (modulo(n, DS0 * DS1) == 2) {
            // Hb
            inB = input;
//            filNoiseB.addVal(inB);
        } else if (modulo(n, DS0 * DS1) == 3) {
            // Off
            inOff = input;
        }
    }

    t2 = micros();

    // Offset suppression
    if (modulo(n, (DS0 * DS1 * DS2)) == 0) {
//        dsA = filNoiseA.getVal();
//        dsB = filNoiseB.getVal();
        
        dsA = inA;
        dsB = inB;
        
        filNormA.addVal(dsA);
        filNormB.addVal(dsB);

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
    if (n % (DS0 * DS1 * DS2 / US3) == 0) {
        interA += distA / US3;
        interB += distB / US3;
    }

    t4 = micros();

    // Global iterator
    if (n < MAXINDEX) {
        n++;
    } else {
        n = 1;
    }

    t6 = micros();

//    Serial.print(-n/100.0f); Serial.print("\t");
//    Serial.print(t1 - t0); Serial.print("\t");
//    Serial.print(t2 - t1); Serial.print("\t");
//    Serial.print(t3 - t2); Serial.print("\t");
//    Serial.print(t4 - t3); Serial.print("\t");
//    Serial.print(t5 - t4); Serial.print("\t");
//    Serial.print(t6 - t5); Serial.print("\t");
//    Serial.print(t6 - t0); Serial.print("\t");
//    Serial.print(micros() - t6); Serial.print("\t");
//    
//    Serial.print(-n/1.0f); Serial.print(" ");
//    Serial.print(input); Serial.print(" ");
//    Serial.print(inA); Serial.print(" ");
//    
//    Serial.print(inB); Serial.print(" ");
//    Serial.print(inOff); Serial.print(" ");
//    Serial.print(dsA); Serial.print(" ");
//    Serial.print(dsB); Serial.print(" ");
//    Serial.print(filA); Serial.print(" ");
//    Serial.print(filB); Serial.print(" ");
//
//    Serial.print(interA, 16); Serial.print(" ");
//    Serial.print(interB); Serial.print(" ");
//    
//    Serial.print(chA); Serial.print(" ");
//    Serial.print(chB); Serial.print(" ");
//    
//    Serial.print(output); Serial.print(" ");
//    Serial.print(interO); Serial.print(" ");
//    
//    Serial.println();
//    Serial.println(1);
}

int modulo(int dividend, int divisor) {
    if (dividend == 1) {
        return divisor > 1;
    } else if (divisor == 1) {
        return 0;
    } else if (divisor <= 4) {
        return dividend % divisor;
    } else {
        int result = dividend;
        for (result; result >= divisor; result -= divisor);
        return result;
    }
}
