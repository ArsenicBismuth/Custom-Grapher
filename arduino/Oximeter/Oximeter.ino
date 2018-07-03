#include <Wire.h>
#include <Adafruit_MCP4725.h>
#include "Filter.h"

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

// Down/upsampling rate, specified relative to previous process
#define DS0 1   // Downsampling rate FS0 = FS / DS1, for input
#define DS1 4                       // Downsampling due to switching of 4 states

// Remember b's passed by reference
//Filter filNoiseA(sizeof(bNoise)/sizeof(float), bNoise);
//Filter filNoiseB(sizeof(bNoise)/sizeof(float), bNoise);

#define DS2 1   // Downsampling for precise offset-suppression
Filter filNormA(sizeof(bNorm)/sizeof(float), bNorm);
Filter filNormB(sizeof(bNorm)/sizeof(float), bNorm);

#define US3 4   // Upsampling for interpolation
#define MAXINDEX (DS0 * DS1 * DS2)       // Used to reset the global iterator, avoiding overflow

#define FS 200                  
#define FSC (FS / DS0 / DS1 / DS2 * US3) // Data rate at the time for modulation

// Carriers
#define FCA 60
#define FCB 100

#define MAXINDEX2 ((FS/FCA) * (FS/FCB)) // Used to separate carrier index from global iterator to keep MAXINDEX low

// Var
int input;
int inA;
int inB;
int inOff;
float chA = 0;
float chB = 0;
float dsA = 0;
float dsB = 0;
float filA = 0;
float filB = 0;
float interA = 0;
float interB = 0;
float distA = 0;
float distB = 0;
int output = 0;
int n = 1;     // Global index
long m = 1;     // Index used for cosine
unsigned long t0, t1, t2, t3, t4, t5, t6;

void setup() {
    Serial.begin(250000);
    pinMode(5, OUTPUT);
    dac.begin(0x62);
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

    // Modulation
    // Currently unmodulated at [-100, 100], converted to [0, 800]
    chA = (100 + interA) * cos(2 * PI * FCA / FS * m) * 8;
    chB = (100 + interB) * cos(2 * PI * FCB / FS * m) * 8;

    t5 = micros();

    // Halved from max range and combined
    output = chA + chB;
    dac.setVoltage(output, false);

    if (n < MAXINDEX) {
        n++;
    } else {
        n = 1;
    }

    if (m < MAXINDEX2) {
        m++;
    } else {
        m = 1;
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
    
    Serial.print(-n/1.0f); Serial.print(" ");
    Serial.print(input); Serial.print(" ");
    Serial.print(inA); Serial.print(" ");
    Serial.print(inB); Serial.print(" ");
    Serial.print(inOff); Serial.print(" ");
    Serial.print(dsA); Serial.print(" ");
    Serial.print(dsB); Serial.print(" ");
    Serial.print(filA); Serial.print(" ");
    Serial.print(filB); Serial.print(" ");
    Serial.print(interA); Serial.print(" ");
    Serial.print(interB); Serial.print(" ");
    Serial.print(chA); Serial.print(" ");
    Serial.print(chB); Serial.print(" ");
    Serial.print(output);

    Serial.println();
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
