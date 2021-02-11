h1 rm -f latency_Q2.txt throughput_Q2.txt
h1 touch latency_Q2.txt throughput_Q2.txt
h1 ping -c 20 h4 >> latency_Q2.txt

h4 java Iperfer -s -p 8080 >> throughput_Q2.txt &

h1 sleep 3

h1 java Iperfer -c -h 10.0.0.4 -p 8080 -t 20 >> throughput_Q2.txt

h1 sleep 5
