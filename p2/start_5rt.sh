#/bin/bash
java -jar VirtualNetwork.jar -v r1 -r rtable.r1 -a arp_cache &> /dev/null &
java -jar VirtualNetwork.jar -v r2 -r rtable.r2 -a arp_cache &> /dev/null &
java -jar VirtualNetwork.jar -v r3 -r rtable.r3 -a arp_cache &> /dev/null &
java -jar VirtualNetwork.jar -v r4 -r rtable.r4 -a arp_cache &> /dev/null &
java -jar VirtualNetwork.jar -v r5 -r rtable.r5 -a arp_cache &> /dev/null &
echo 5 routers started 
