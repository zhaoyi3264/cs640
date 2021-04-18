FILE=TCPState.java
MTU=8

all: TCPend

TCPend: TCPend.java
	javac *.java

clean:
	rm *.class

send: TCPend
	clear
	java TCPend -p 5000 -s 127.0.0.1 -a 8000 -f $(FILE) -m $(MTU) -c 4

receive: TCPend
	clear
	java TCPend -p 8000 -m $(MTU) -c 4 -f receive

test: TCPend Test.java
	clear
	javac Test.java
	java Test