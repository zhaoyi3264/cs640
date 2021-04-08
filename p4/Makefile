all: TCPend

TCPend: src/com/cs640/tcp/*.java
	javac src/com/cs640/tcp/*.java

clean:
	rm src/com/cs640/tcp/*.class

send: TCPend
	clear
	java -classpath src com.cs640.tcp.TCPend -p 5000 -s 127.0.0.1 -a 8000 -f Makefile -m 8 -c 32

receive: TCPend
	clear
	java -classpath src com.cs640.tcp.TCPend -p 8000 -m 8 -c 32 -f receive
