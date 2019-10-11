#!/bin/bash

# the default node number is 3
N=${1:-6}


# start hadoop master container
sudo docker rm -f h01 &> /dev/null
echo "start h01 container..."
sudo docker run -itd \
                --net=hadoop \
                -p 50070:50070 \
                -p 8088:8088 \
                -p 9870:9870 \
                --name h01 \
                --hostname h01 \
                peppertargaryen/hadoop:1.0 &> /dev/null


# start hadoop slave container
i=2
while [ $i -lt $N ]
do
	sudo docker rm -f h0$i &> /dev/null
	echo "start h0$i container..."
	sudo docker run -itd \
	                --net=hadoop \
	                --name h0$i \
	                --hostname h0$i \
	                peppertargaryen/hadoop:1.0 &> /dev/null
	i=$(( $i + 1 ))
done 

# get into hadoop master container
sudo docker exec -it h01 bash
