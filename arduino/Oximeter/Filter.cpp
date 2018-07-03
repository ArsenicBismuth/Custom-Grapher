#include "Arduino.h"
#include "Filter.h"
using namespace std;

// Usage must addVal then getVal immediately since there's no buffer

// IIR if a is given
Filter::Filter(int _order, float *_b, float *_a) {
    // Using "_" to distinguish member and not and avoiding using "_".
    order = _order;
    a = _a;
    b = _b;
    
    int aSize = sizeof(a) / sizeof(float);
    int bSize = sizeof(b) / sizeof(float);
    
    if (aSize != 0) iir = true;
    init();
}

// FIR
Filter::Filter(int _order, float *_b) {
    order = _order;
    b = _b;
    iir = false;

    int bSize = sizeof(b) / sizeof(float);
    
    init();
}

void Filter::init() {
    // The last two brackets isn't necessary, just used to zero them
    if (iir) outputs = new float[1]();
    else outputs = new float[order]();
    inputs = new float[order]();
}

int st = 0; // The array index for x[n] in circular buffer

// Add value and simultanteously compute the result
void Filter::addVal(int value) {
    
    // Shifting the start of circular buffer FORWARD, since
    // the prev x(n) is x(n-1) now.
    st = modulo((st + 1), order);

    // Get input and store to the new start, x[n]
    inputs[st] = value;

    // Calculate the convolution from koefs
    outputs[st] = 0;
    
    for (int i = 0; i < order; i++) {
        int j = modulo((order + st - i), order); // Decrement, x[n-i] * b[i]
        
        outputs[st] += inputs[j] * b[i];
        
        if ((iir) && (i != 0)) {
            outputs[st] -= outputs[j] * a[i];
        }
    }
    
}

// Get individual output
float Filter::getVal() {
    return outputs[st];
}

// Alternate modulo function
// Good enough if dividend is sure to be not far bigger than divisor
int Filter::modulo(int dividend, int divisor) {
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
