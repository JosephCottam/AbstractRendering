#!/bin/sh
taskset -c 0 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -p 1 >> kivaTimings.txt
taskset -c 0-1 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 2 >> kivaTimings.txt
taskset -c 0-2 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 3 >> kivaTimings.txt
taskset -c 0-3 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 4 >> kivaTimings.txt
taskset -c 0-4 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 5 >> kivaTimings.txt
taskset -c 0-5 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 6 >> kivaTimings.txt
taskset -c 0-6 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 7 >> kivaTimings.txt
taskset -c 0-7 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 8 >> kivaTimings.txt
taskset -c 0-8 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 9 >> kivaTimings.txt
taskset -c 0-9 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 10 >> kivaTimings.txt
taskset -c 0-10 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 11  >> kivaTimings.txt
taskset -c 0-11 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 12 >> kivaTimings.txt
taskset -c 0-12 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 13 >> kivaTimings.txt
taskset -c 0-13 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 14 >> kivaTimings.txt
taskset -c 0-14 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 15 >> kivaTimings.txt
taskset -c 0-15 java -cp ARApp.jar ar.RenderSpeedTest -data ../data/kiva-adjacency.hbin -header false -p 16 >> kivaTimings.txt

