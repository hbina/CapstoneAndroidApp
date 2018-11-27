# ~November 23rd 2018

Today we have made some work on:

1. JSON parser
2. Accessibility support
3. Refactoring
4. Arduino code

The first order of business is the JSON parser. 
We need this to parse the JSONObject returned by the API.
Added a lot more accessibility support.
Specifically, we have added a voice output when the device does not support Bluetooth. 
This was originally only treated as an internal debugging tool because Android Debugging Bridge (adb) does not support Bluetooth.
However, we feel that it might be necessary to notice the user their device does not support Bluetooth.
Despite this being extremely rare, who knows what Apple will remove next.

Secondly, we have also refactored the logging for developers and the speech for the user.
Previously, we always made 2 seperate calls to logging and generating the voice output.
With today's patch, this has been made into a single function.
The default TEXT_TO_SPEECH_MODE for this is QUEUE_ADD because everything seems to be working too fast for the voice to make out words properly.
There is however, an overloaded method of this that have an option custom parameter for TEXT_TO_SPEECH_MODE.
The only time this was used was for QUEUE_FLUSH whenever the user have arrived and detected the RFIDs.

There are some places where this is not possible. For instance, when text-to-speech fails to work!

We have also fixed an issue with Arduino code where the first character of the RFID tag is left behind.
We think that this issue was caused by the Android's inputStream receiving the first character and the rest of the characters as 2 different byte arrays.
Specifically, the RFID tag is defined an array of 4 integers that will be interpreted as HEX values to get their ASCII equivalent.
For whatever reason, the first integer will be sent and the Android's Bluetooth socket will receive this.
The thread responsible for handling incoming message will then process this single int.
Then, the 3 other integers will be sent in a single message to be received by the Bluetooth socket.
The thread will then process this and disregard the previous integer.
We have no way of confirming this.
However, we have a simply workaround for this.
Simply, we introduce a dummy int that will act as the bait instead.
So instead of having an array of size 4, we have an array of size 5 with the first integer acting as a dummy integer.

# ~November 16th 2018

In today's issue, we have:
 
1. Introduced text-to-speech functionality to the Android device.
2. Migrated to AndroidX (Updated API level to 26)
3. Fixed a crash on devices that does not suppor Bluetooth
4. Made a boolean responsible for stopping the thread handling Bluetooth's inputStream as volatile!
5. Deciding whether to use local database to map RFID tags to location.

Since we are working with people with vision problems, it is of course necessary to have a voice output to help navigate and inform user of the state of the application.
We have already supported accessibility extensions (like content descriptions) before.
Today, we are improving the user experience further!

We have also migrated to AndroidX.
This is to ensure that our applications are future-proof.
Mobile development is notorious for being too volatile with too many changes happening each year making application versioning a nightmare to deal with.
AndroidX attempts to help with that issue.
Unfortunately however, this means that the application only support devices with API level larger than 25.
Furthermore, Google Play have also set the same API level lower bound.
Therefore, we find that it is necessary to have this migration to ensure that our application will suddenly not work when its needed the most.
And to make sure that our application is as consistent as possible.


Next, we have an issue where devices that does not support Bluetooth will immediately crash.
We had this problem because we failed to note that some devices (for some reason) does not support Bluetooth.
When this happen, our Bluetooth object would be null and everything crashes afterwards.
To fix this, we simply add a check and will provide user with a notification saying that this device does not support Bluetooth.
The application will then exit.

Multithreading is an art in itself. When we first introduced a thread in the application we didn't think much of it.
The application seemeed to work fine and we didn't notice any issue with the thread object itself.
However, we have discovered that Java supports the volatile keyword and upon further research we have found that indeed, any critical variables that is used by thread need to be set as volatile.
The keyword volatile can be regarded as a flag to prevent the hardware from optimizing the retrieval of this variable.
In particular, any read and write requested to this variable will have to be made through the memory.
This means that any copy stored in the cache is made dirty.

We are also considering using Room Database to handle the mapping between RFID tags detected by the Bluetooth and the actual name of the place.
We might have to move this particular development to next semester because there are more essential section of the application that need fixing.

We still haven't fixed the problem with Bluetooth reader missing the first character.


# ~November 5th 2018

Today, we introduce a voice input capability to the device.
Since our target user is blind, it is necessary that user perform as little touch action as possible.
Google provides a ready to use voice recognition Intent.
After parsing the voice input, it will return an array of strings that it think it recognized.
For instance, if one says "Hello World", it will (possibly) returns "Hello World" and "Hello world".
In some rare cases, it might even return "LOL".
It is our responsible to select which string is the most appropriate for our purposes.

Upon further testing, we noted that the first strings are mostly correct most of the time.
Therefore, we simply pick the first string from the array.

In future builds, we hope to introduce a method to pick the most legible string.
Currently, we think of 2 possible solution:

1. ## Let the user pick the right word

Using this method, after the array of possible strings are received, we let the user decide which to use.
With this method, we can remove critical errors like interpreting "Hello world" as "LOL".
However, it can only prevent edge cases.
This method cannot help user distinguish two words that are very similar to one another.
For example, consider "Hello world" and "Hello World".

2. ## Approximation test

In this method, we try to automatically figure out what the user want based on the device location.
For example, consider a situation is located near "Hello World".
The user then says that he/she wants to go to "Hello World".
Then, we received "Hello World", "Lol" and "Hell World" as possible strings.
We will automatically assume that the user wants to go to "Hello World".
Before proceeding with this, we will prompt the user for confirmation.


# ~October 31st 2018

Reworked how Bluetooth connection was made.
To do this, we introduce a thread that will continuously detect any incoming message from the Bluetooth device.
There is a problem where the inputStream is missing a character.


# ~October 15th 2018

Created a way to connect to Bluetooth.
We have not tested if it works.