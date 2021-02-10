h1 rm -f q3a.txt
h1 touch q3a.txt
h1 ping -c 20 h4 >> q3a.txt &
h7 ping -c 20 h9 >> q3a.txt

h4 java Iperfer -s -p 8080 >> q3a.txt &
h9 java Iperfer -s -p 8080 >> q3a.txt &

h1 sleep 6

h1 java Iperfer -c -h 10.0.0.4 -p 8080 -t 20 >> q3a.txt &
h7 java Iperfer -c -h 10.0.0.9 -p 8080 -t 20 >> q3a.txt

h1 sleep 10
