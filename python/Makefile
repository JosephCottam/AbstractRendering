
CC=clang
CFLAGS=-O3 -Wall -march=native -std=c++11 -fno-rtti -fno-exceptions -fPIC -lstdc++ -dynamiclib

all: libtransform.dylib libtransform_libdispatch.dylib

libtransform.dylib: transform.cpp
	$(CC) $(CFLAGS) -o $@ $<

libtransform_libdispatch.dylib: transform_libdispatch.cpp
	$(CC) $(CFLAGS) -o $@ $<

clean:
	rm -rf libtransform.dylib libtransform_libdispatch.dylib
