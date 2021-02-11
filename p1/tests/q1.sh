h1 rm -f latency_L{1..5}.txt throughput_L{1..5}.txt
h1 touch latency_L{1..5}.txt throughput_L{1..5}.txt

h1 ping -c 20 h2 >> latency_L1.txt
h2 ping -c 20 h3 >> latency_L2.txt
h3 ping -c 20 h4 >> latency_L3.txt
h2 ping -c 20 h5 >> latency_L4.txt
h3 ping -c 20 h6 >> latency_L5.txt

h2 java Iperfer -s -p 8080 >> throughput_L1.txt &
h1 sleep 3
h1 java Iperfer -c -h 10.0.0.2 -p 8080 -t 20 >> throughput_L1.txt
h1 sleep 5

h3 java Iperfer -s -p 8080 >> throughput_L2.txt &
h1 sleep 3
h2 java Iperfer -c -h 10.0.0.3 -p 8080 -t 20 >> throughput_L2.txt
h1 sleep 5

h4 java Iperfer -s -p 8080 >> throughput_L3.txt &
h1 sleep 3
h3 java Iperfer -c -h 10.0.0.4 -p 8080 -t 20 >> throughput_L3.txt
h1 sleep 5

h5 java Iperfer -s -p 8080 >> throughput_L4.txt &
h1 sleep 3
h2 java Iperfer -c -h 10.0.0.5 -p 8080 -t 20 >> throughput_L4.txt
h1 sleep 5

h6 java Iperfer -s -p 8080 >> throughput_L5.txt &
h1 sleep 3
h3 java Iperfer -c -h 10.0.0.6 -p 8080 -t 20 >> throughput_L5.txt
h1 sleep 5
