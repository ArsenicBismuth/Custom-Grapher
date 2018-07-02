
int j;
long t0, t1, t2, t3, t4;

void setup() {
    Serial.begin(250000);

    for (int k = 1; k <= 64; k++) {
        t0 = micros();
    
        for (int i = 0; i <= 300; i++) {
            j = modulo(i, k);
        }
        
        t1 = micros();
    
        for (int h = 0; h <= 300; h++) {
            j = h % k;
        }
    
        t2 = micros();

        Serial.print(k); Serial.print("\t");
        Serial.print(t1 - t0); Serial.print("\t");
        Serial.print(t2 - t1); Serial.print("\t");
        Serial.print((float)(t1 - t0) * 100 / (t2 - t1) - 100); Serial.print("\t");
        Serial.println();
    }
}

void loop() {
    
}

int modulo(int dividend, int divisor) {
    if (divisor <= 4) {
        return dividend % divisor;
    } else {
        int result;
        for (result = dividend; result >= divisor; result -= divisor);
        return result;
    }
}
