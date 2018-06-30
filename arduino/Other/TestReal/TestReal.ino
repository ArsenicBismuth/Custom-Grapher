#include "math.h"

#define ORDER 5

// Var
int buffer_input[ORDER] = {0};
int buffer_output[ORDER] = {0};
int temp = 0;
int i = 0, j = 0;

// Filter Coefficients (from MATLAB):
int a[ORDER] = {536870912, -1453348081, 1574796169, -838799089, 194152261};
int b[ORDER] = {45951035, 0, -91902070, 0, 45951035};

void setup() {
    Serial.begin(9600);
    pinMode(5, OUTPUT);
}

void loop() {
    // Shifting data
    for(i = ORDER-1; i > 0; i--) {
        buffer_input[i] = buffer_input[i - 1];
        buffer_output[i] = buffer_output[i - 1];
    }
    // Get the new first data, so current input
    buffer_input[0] = analogRead(A5);
    
    // Convert to real numbers then calculate the convolution from koefs
    temp = 0;
    for(i = 0; i < ORDER; i++) {
        temp += (buffer_input[i] >> 15) * (b[i] >> 16);
    }
    for(i = 1; i < ORDER; i++) {
        temp -= (buffer_output[i] >> 15) * (a[i] >> 16);
    }
    
    // Convert to 64-bit from 16-bit
    temp = temp * 4;
    
    // Output
    buffer_output[0] = temp;
    
}
