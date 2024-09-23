### Check path in wsl

 `\\wsl.localhost === \\wsl$` 

optional create following path in volume path : `\\wsl.localhost\docker-desktop-data\version-pack-data\community\docker\volumes\docker_mqHAdata\_data`  

### In host machine
```
mkdir mqHAdata\logs
mkdir mqHAdata\qmgrs
```

If there are files created in above
```
cd /mqHAdata/
rmdir /q /s logs
rmdir /q /s qmgrs

mkdir logs
mkdir qmgrs
```

### In node 1
```
$ endmqm -i EARTH && dltmqm EARTH

$ crtmqm -u DEV.DEAD.LETTER.QUEUE -ll -lp 3 -ls 2 -lf 4096 -md /mqHAdata/qmgrs -ld /mqHAdata/logs -oa UserExternal QM1

$ dspmqinf -o command QM1

Copy the ServiceComponemt stanza from /etc/mqm/qm-service-component.ini to mqHAdata\qmgrs\QM1\qm.ini
Ensure HtpAuth.Service is over than UNIX.auth.service
$ cat /etc/mqm/qm-service-component.ini 

$ strmqm -x QM1
```

### In node 2
```
$ endmqm -i MOON && dltmqm MOON

$ addmqinf -s QueueManager -v Name=QM1 -v Directory=QM1 -v Prefix=/mnt/mqm/data -v DataPath=/mqHAdata/qmgrs/QM1

$ strmqm -x QM1

$ dspmq -xf -m QM1
```

### In node 1, switch over to standby QM
```
runmqsc QM1 -f /mqHAdata/mq-dev-config.mqsc

endmqm -i -s QM1
```

### Go into venus container to verify the switch-over
```
$ /opt/mqm/samp/bin/amqssslc -m QM1 -c DEV.APP.SVRCONN -x moon -s TLS_RSA_WITH_AES_128_CBC_SHA256 <<< "passw0rd"

$ /opt/mqm/samp/bin/amqsphac DEV.QUEUE.1 QM1 <<< "passw0rd"
```

### Open 3 test containers
```
$ /opt/mqm/samp/bin/amqsghac DEV.QUEUE.1 QM1 <<< "passw0rd"
$ /opt/mqm/samp/bin/amqsmhac -s SOURCE -t TARGET -m QM1 <<< "passw0rd"
$ /opt/mqm/samp/bin/amqsphac DEV.QUEUE.1 QM1 <<< "passw0rd"
```


#### Reference
 https://w3partnership.com/Blog/2016/04/29/creating-an-ibm-mq-ha-cluster-using-a-multi-instance-queue-manager/