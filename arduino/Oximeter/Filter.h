#ifndef Filter_h
#define Filter_h
#include "Arduino.h"

class Filter
{
    private:
        int order = 32;
        bool iir = false;
        
        // Pointers are used because we can't declare anything yet.
        // It's only here to be recognized as a class member.
        float* inputs;
        float* outputs;
        float* a;
        float* b;

        void init();
        int modulo(int dividend, int divisor);
        
    public:
        Filter(int order, float *b, float *a);
        Filter(int order, float *b);
        void addVal(int value);  // Add value and compute
        float getVal();
};

#endif
