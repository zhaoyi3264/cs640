h1 rm -f latency_h1-h4.txt latency_h5-h6.txt throughput_h1-h4.txt throughput_h5-h6.txt
h1 touch latency_h1-h4.txt latency_h5-h6.txt throughput_h1-h4.txt throughput_h5-h6.txt
h1 ping -c 20 h4 >> latency_h1-h4.txt &
h5 ping -c 20 h6 >> latency_h5-h6.txt

h4 java Iperfer -s -p 8080 >> throughput_h1-h4.txt &
h6 java Iperfer -s -p 8080 >> throughput_h5-h6.txt &

h1 sleep 3

h1 java Iperfer -c -h 10.0.0.4 -p 8080 -t 20 >> throughput_h1-h4.txt &
h5 java Iperfer -c -h 10.0.0.6 -p 8080 -t 20 >> throughput_h5-h6.txt

h1 sleep 10
