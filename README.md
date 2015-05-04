# Pump_Control
Michigan State University ECE 480 senior design project source code for Android app. The app communicates with an RFduino via bluetooth to control a pump motor.

- The project goal is to design a surgical tool for use in unsanitary conditions. The ultimate goal is to prevent infections after surgery that occur from dirty surgery sites. The tool will be fed a solution with an antimicrobial that will run though microchannels and be released in a small amount on the tools surface. This will keep any potential infections from forming on the tool when it is set down on an unsanitized surface but will not cause any issues with preforming the surgery.

# What It Does:
- Takes an input of the estimated surgery time and outputs the time remaining
- When started the app will take a senor input from the bluetooth communication channel and compute an output pump power level to be sent back to the pump motor
- More capabilities may be added if the need is there

# Additional Information
- Utilizes Bluetooth LE
- RFduino is a Bluetooth enabled arduino prototyping microcontroller
- RFduino code will be linked eventually
- More details on the hardware will be linked and added at a later time
