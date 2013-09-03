UNAME = $(shell uname)
ifeq ($(UNAME), Linux)
	PLATFORM = linux
endif
ifeq ($(UNAME), Darwin)
    PLATFORM = darwin
endif

TARGET = libpaxos.jnilib

CC = gcc
CFLAGS += -shared -fPIC -g -O2 -W -Wall -Wno-unused-parameter -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(PLATFORM) -I/usr/java/jdk1.7.0_10/include/ -I/usr/java/jdk1.7.0_10/include/$(PLATFORM)

all: $(TARGET)

header: ../../target/classes/ch/usi/da/paxos/storage/CyclicArray.class
	javah -jni -classpath ../../target/classes/ ch.usi.da.paxos.storage.CyclicArray

%.o: %.c
	$(CC) $(CFLAGS) -o $@ -c $<

$(TARGET): paxos.c
	$(CC) $(CFLAGS) -o $@ $^ $(LDFLAGS)	

clean:
	find . -name "*~" -exec rm -f {} ';'
	find . -name "*.o" -exec rm -f {} ';'
	rm -f $(TARGET)

