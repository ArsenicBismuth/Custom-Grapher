//Libraries
#include <SoftwareSerial.h>

//Hardware specs
SoftwareSerial bt(11,12);          //RX, TX; thus the opposite for the BT module

String s;

void setup() {
  Serial.begin(9600); //For native USB port only
  bt.begin(9600);
  bt.println("Connected");
  
  pinMode(13,OUTPUT); //Indicator light
  delay(10);
}

void loop() {
  // Extreme testing, a lot of invalid cases for one valid data
  
  while(bt.available()){
    char c = bt.read(); //Store chars
    Serial.println(c);  // Check inputs
    //s.concat(c);      //Combine chars received
    
    switch(c) {
      case '!': 
        delay(1000);  // Simulate recording, 1 sec

        // Before
        bt.print("29999/8888~");  // Simulate invalid data, no initializer
        delay(500);
        bt.print("#39999/8888");  // Simulate invalid data, no ending
        delay(500);
        bt.print("49838");  // Simulate invalid data, random
        delay(500);

        // Wanted
        bt.print("#1111/2222~");  // Simulate valid data
        delay(500);

        // After
        bt.print("29999/8888~");  // Simulate invalid data, no initializer
        delay(500);
        bt.print("#39999/8888");  // Simulate invalid data, no ending
        delay(500);
        bt.print("49838");  // Simulate invalid data, random
        delay(500);
        break;
    }
  }
  bt.print("#19999/8888~"); // // Simulate invalid data, giving without being asked
  delay(1000);
}
