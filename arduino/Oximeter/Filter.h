#ifndef Filter_h
#define Filter_h
#include "Arduino.h"

class Filter
{
    private:
        int _order = 32;
        bool _iir = false;
        
        // Pointers are used because we can't declare anything yet.
        // It's only here to be recognized as a class member.
        float* _inputs;
        float* _outputs;
        float* _a;
        float* _b;

        void init();
        
    public:
        Filter(int order, float *b, float *a);
        Filter(int order, float *b);
        void addVal(int value);  // Add value and compute
        float getVal();
};

#endif
