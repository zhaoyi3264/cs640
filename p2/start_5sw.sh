#/bin/bash
java -jar VirtualNetwork.jar -v s1 &> /dev/null &
java -jar VirtualNetwork.jar -v s2 &> /dev/null &
java -jar VirtualNetwork.jar -v s3 &> /dev/null &
java -jar VirtualNetwork.jar -v s4 &> /dev/null &
java -jar VirtualNetwork.jar -v s5 &> /dev/null &
echo 5 switches started
