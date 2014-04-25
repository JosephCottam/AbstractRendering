
CC=clang
CFLAGS=-O3 -Wall -march=native -std=c++11 -fno-rtti -fno-exceptions -fPIC -lstdc++ -dynamiclib

all: transform.so transform_libdispatch.so

transform.so: transform.cpp
	$(CC) $(CFLAGS) -o $@ $<

transform_libdispatch.so: transform_libdispatch.cpp
	$(CC) $(CFLAGS) -o $@ $<

clean:
	rm -rf transform.so transform_libdispatch.so
