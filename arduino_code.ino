#include <SoftwareSerial.h>

SoftwareSerial BTserial(10, 11); // RX | TX

int sensorPin = A0;

int sensorValue = 0;

void setup() {

  BTserial.begin(9600);
}

void loop() {

  sensorValue = analogRead(sensorPin);
  BTserial.print("gaf");
  delay(20);

}