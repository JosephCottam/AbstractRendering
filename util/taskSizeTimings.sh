#!/bin/sh
java -cp ARApp.jar ar.RenderSpeedTest -config Kiva -task 100000000 >> taskTiming.txt 
java -cp ARApp.jar ar.RenderSpeedTest -config Kiva -task 10000000 >> taskTiming.txt 
java -cp ARApp.jar ar.RenderSpeedTest -config Kiva -task 1000000 >> taskTiming.txt 
java -cp ARApp.jar ar.RenderSpeedTest -config Kiva -task 100000 >> taskTiming.txt 
java -cp ARApp.jar ar.RenderSpeedTest -config Kiva -task 10000 >> taskTiming.txt 
java -cp ARApp.jar ar.RenderSpeedTest -config Kiva -task 1000 >> taskTiming.txt 
java -cp ARApp.jar ar.RenderSpeedTest -config Kiva -task 100 >> taskTiming.txt 
java -cp ARApp.jar ar.RenderSpeedTest -config Kiva -task 10 >> taskTiming.txt 
java -cp ARApp.jar ar.RenderSpeedTest -config Kiva -task 1 >> taskTiming.txt 
