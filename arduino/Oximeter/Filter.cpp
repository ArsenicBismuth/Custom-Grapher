#include "Arduino.h"
#include "Filter.h"
using namespace std;

// Usage must addVal then getVal immediately since there's no buffer

// IIR if a is given
Filter::Filter(int order, float *b, float *a) {
    // Using "_" to distinguish member and not and avoiding using "_".
    _order = order;
    _a = a;
    _b = b;
    
    int aSize = sizeof(a) / sizeof(float);
    int bSize = sizeof(b) / sizeof(float);
    
    if (aSize != 0) _iir = true;
    init();
}

// FIR
Filter::Filter(int order, float *b) {
    _order = order;
    _b = b;
    _iir = false;

    int bSize = sizeof(b) / sizeof(float);
    
    init();
}

void Filter::init() {
    // The last two brackets isn't necessary, just used to zero them
    if (_iir) _outputs = new float[1]();
    else _outputs = new float[_order]();
    _inputs = new float[_order]();
}

// Add value and simultanteously compute the result
void Filter::addVal(int value) {
    float temp;

    // Shifting data
    int i;
    for(i = _order - 1; i > 0; i--) {
        _inputs[i] = _inputs[i - 1];
        if (_iir) _outputs[i] = _outputs[i - 1];
    }

    // Get the new first data, so current input
    _inputs[0] = value;

    // Calculate the convolution from koefs
    temp = 0;
    for (i = 0; i < _order; i++)
        temp += _inputs[i] * _b[i];

    if (_iir) for (i = 1; i < _order; i++)
        temp -= _outputs[i] * _a[i];

    // Result
    _outputs[0] = temp;
}

// Get individual output
float Filter::getVal() { return _outputs[0]; }
