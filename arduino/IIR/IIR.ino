#include "math.h"

#define ORDER 32

// Var
float buffer_input[ORDER] = {0};
float buffer_output[ORDER] = {0};
float temp = 0;
int i = 0, j = 0;

// Filter Coefficients (from MATLAB):
float a[ORDER] = {1, -4.98940999368654, 9.95769602720355, -9.93662793621857, 4.95780776594274, -0.989465863240812};
float b[ORDER] = {-0.00126123490547738,-0.00184849330106461,-0.00277122455078149,-0.00389561547918654,-0.00474014009013690,-0.00450658595875769,-0.00221711222564580,0.00306513721280641,0.0119792920419832,0.0246356833512128,0.0404716459636847,0.0582391630524604,0.0761407757778390,0.0920959490127426,0.104088464123935,0.110524295974387,0.110524295974387,0.104088464123935,0.0920959490127426,0.0761407757778390,0.0582391630524604,0.0404716459636847,0.0246356833512128,0.0119792920419832,0.00306513721280641,-0.00221711222564580,-0.00450658595875769,-0.00474014009013690,-0.00389561547918654,-0.00277122455078149,-0.00184849330106461,-0.00126123490547738};

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
    
    // Calculate the convolution from koefs
    temp = 0;
    for(i = 0; i < ORDER; i++) {
        temp += buffer_input[i] * b[i];
    }
    for(i = 1; i < ORDER; i++) {
        temp -= buffer_output[i] * a[i];
    }
    
    // Output
    buffer_output[0] = temp;
    Serial.print(buffer_input[0]);
    Serial.print(" ");
    Serial.println(buffer_output[0]);
}
