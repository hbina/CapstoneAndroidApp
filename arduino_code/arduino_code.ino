/*
   All the resources for this project: https://www.hackster.io/Aritro
   Modified by Aritro Mukherjee
*/
#include <SoftwareSerial.h>
#include <SPI.h>
#include <MFRC522.h>
SoftwareSerial BTserial(0, 1); // RX | TX

#define SS_PIN 10
#define RST_PIN 9
MFRC522 mfrc522(SS_PIN, RST_PIN);   // Create MFRC522 instance.
int counter = 1;
byte key[5] = {100, 100, 100, 100, 100};
void setup()
{
  pinMode(3, OUTPUT);
  Serial.begin(9600);   // Initiate a serial communication
  SPI.begin();      // Initiate  SPI bus
  mfrc522.PCD_Init();   // Initiate MFRC522
  mfrc522.PCD_SetRegisterBitMask(mfrc522.RFCfgReg, (0x07 << 4));
  BTserial.begin(9600);

}

boolean reached = false;
void loop()
{
  // Look for new cards
  if ( ! mfrc522.PICC_IsNewCardPresent())
  {
    return;
  } else {
    // Select one of the cards
    if ( ! mfrc522.PICC_ReadCardSerial())
    {
      return;
    } else {
      String content = "";
      byte letter;
      boolean someBool = false;
      for (byte i = 1; i < mfrc522.uid.size + 1; i++)
      {
        if (key[i] != mfrc522.uid.uidByte[i - 1]) {
          someBool = someBool || true;
        }
      }
      if (!someBool) {
        return;
      } else {

        Serial.print("\n");
        for (byte i = 0; i < mfrc522.uid.size + 1; i++)
        {
          key[i] = mfrc522.uid.uidByte[i - 1];
          Serial.print(key[i], HEX);
          BTserial.print(key[i], HEX);
        }


        for (byte i = 0; i < 6; i++)
        {
          digitalWrite(3, HIGH);   // turn the LED on (HIGH is the voltage level)
          delay(50);               // wait for a second
          digitalWrite(3, LOW);    // turn the LED off by making the voltage LOW
          delay(50);
        }
      }
    }
  }
}
