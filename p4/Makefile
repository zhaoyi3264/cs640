all: TCPend

TCPend: TCPend.java
	javac *.java

clean:
	rm *.class

send: TCPend
	clear
	java TCPend -p 5000 -s 127.0.0.1 -a 8000 -f Makefile -m 8 -c 32

receive: TCPend
	clear
	java TCPend -p 8000 -m 8 -c 32 -f receive

test: TCPend Test.java
	clear
	javac Test.java
	java Test