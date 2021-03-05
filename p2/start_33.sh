#/bin/bash
java -jar VirtualNetwork.jar -v s1 &> /dev/null &
java -jar VirtualNetwork.jar -v s2 &> /dev/null &
java -jar VirtualNetwork.jar -v s3 &> /dev/null &
java -jar VirtualNetwork.jar -v r1 -r rtable.r1 -a arp_cache &> /dev/null &
java -jar VirtualNetwork.jar -v r2 -r rtable.r2 -a arp_cache &> /dev/null &
java -jar VirtualNetwork.jar -v r3 -r rtable.r3 -a arp_cache &> /dev/null &
echo 3 switches 3 routers started 
