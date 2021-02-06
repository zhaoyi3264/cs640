all: Iperfer

Iperfer: Iperfer.java
	clear
	javac Iperfer.java
	
run-client: Iperfer
	java Iperfer -c -h 127.0.0.1 -p 8080 -t 3
	
run-server: Iperfer
	java Iperfer -s -p 8080
	
clean:
	rm -rf Iperfer
