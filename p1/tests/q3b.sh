h1 rm -f q3b.txt
h1 touch q3b.txt

h1 ping -c 20 h4 >> q3b.txt &
h7 ping -c 20 h9 >> q3b.txt &
h8 ping -c 20 h10 >> q3b.txt

h4 java Iperfer -s -p 8080 >> q3b.txt &
h9 java Iperfer -s -p 8080 >> q3b.txt &
h10 java Iperfer -s -p 8080 >> q3b.txt &

h1 sleep 9

h1 java Iperfer -c -h 10.0.0.4 -p 8080 -t 20 >> q3b.txt &
h7 java Iperfer -c -h 10.0.0.9 -p 8080 -t 20 >> q3b.txt &
h8 java Iperfer -c -h 10.0.0.10 -p 8080 -t 20 >> q3b.txt

h1 sleep 15
