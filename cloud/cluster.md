## CCM side
```
DEFINE LISTENER (TSW3CLU1.LISTENER) TRPTYPE (TCP) CONTROL (QMGR) PORT (11414) REPLACE
START LISTENER (TSW3CLU1.LISTENER)

DEFINE CHANNEL (TSW3CLU1.TO.CCM) CHLTYPE (CLUSRCVR) CONNAME ('10.3.1.124(11414)') CLUSTER (TSW3CLU1) REPLACE
DEFINE CHANNEL (TSW3CLU1.TO.STD) CHLTYPE (CLUSSDR) CONNAME ('10.1.1.80(11414)') CLUSTER (TSW3CLU1) REPLACE

** Allow all remote queue managers connect to this QM
SET CHLAUTH('*') TYPE(QMGRMAP) QMNAME(*) ADDRESS(*) MCAUSER('mqm') ACTION(REPLACE)

ALTER QMGR REPOS('TSW3CLU1') REPOSNL(' ') MAXMSGL(104857600) PERFMEV(ENABLED) DEADQ('DEV.DEAD.LETTER.QUEUE')
** DEFCLXQ: SCTQ or CHANNEL 
ALTER QMGR DEFCLXQ(SCTQ)

DEFINE QLOCAL('CCM.BACKOUT.Q') DEFPSIST(YES) REPLACE
SET AUTHREC PROFILE('CCM.BACKOUT.Q') GROUP('mqclient') OBJTYPE(QUEUE) AUTHADD(BROWSE,DSP,GET,INQ,PUT,PASSALL)

** Add option DEFBIND(NOTFIXED) in cluster local queue to allow the workload management algorithm to select the most suitable destination on a per message basis, but OPEN is 
** OPEN is to route all messages put to same QMGR
DEFINE QLOCAL ('CCM.ROAD.ACI.SUBMISSION.Q') CLUSTER (TSW3CLU1) DEFPSIST (YES) DEFBIND(OPEN) BOTHRESH(3) BOQNAME('CCM.BACKOUT.Q') REPLACE
SET AUTHREC PROFILE('CCM.ROAD.ACI.SUBMISSION.Q') GROUP('mqclient') OBJTYPE(QUEUE) AUTHADD(BROWSE,DSP,GET,INQ,PUT)

SET AUTHREC PROFILE('SYSTEM.CLUSTER.TRANSMIT.QUEUE') GROUP('mqclient') OBJTYPE(QUEUE) AUTHADD(PUT)
```

----
## STD side
```
DEFINE LISTENER (TSW3CLU1.LISTENER) TRPTYPE (TCP) CONTROL (QMGR) PORT (11414) REPLACE
START LISTENER (TSW3CLU1.LISTENER)

DEFINE CHANNEL (TSW3CLU1.TO.CCM) CHLTYPE (CLUSSDR) CONNAME ('10.3.1.124(11414)') CLUSTER (TSW3CLU1) REPLACE
DEFINE CHANNEL (TSW3CLU1.TO.STD) CHLTYPE (CLUSRCVR) CONNAME ('10.1.1.80(11414)') CLUSTER (TSW3CLU1) REPLACE

** Allow all remote queue managers connect to this QM
SET CHLAUTH('*') TYPE(QMGRMAP) QMNAME(*) ADDRESS(*) MCAUSER('mqm') ACTION(REPLACE)

ALTER QMGR REPOS('TSW3CLU1') REPOSNL(' ') MAXMSGL(104857600) PERFMEV(ENABLED) DEADQ('DEV.DEAD.LETTER.QUEUE')
ALTER QMGR DEFCLXQ(SCTQ) // single transimission queue

** Add option DEFBIND(NOTFIXED) in cluster local queue to allow the workload management algorithm to select the most suitable destination on a per message basis
** OPEN is to route all messages put to same QMGR
DEFINE QLOCAL (STD.ROAD.CROSSING.Q) CLUSTER (TSW3CLU1) DEFPSIST (YES) DEFBIND(OPEN) REPLACE
SET AUTHREC PROFILE('STD.ROAD.CROSSING.Q') GROUP('mqclient') OBJTYPE(QUEUE) AUTHADD(BROWSE,GET,INQ,PUT,DSP)

SET AUTHREC PROFILE('SYSTEM.CLUSTER.TRANSMIT.QUEUE') GROUP('mqclient') OBJTYPE(QUEUE) AUTHADD(PUT)
```

## STD side
```
$ export MQSAMP_USER_ID=mquser
$ export MQCCDTURL=/tmp/config/ccdt.json
$ export MQSSLKEYR=/opt/mqHAdata/kdb/qm
$ /opt/mqm/samp/bin/amqsphac CCM.ROAD.ACI.SUBMISSION.Q STDQM <<< "P@ssw0rd"
$ /opt/mqm/samp/bin/amqsghac STD.ROAD.CROSSING.Q STDQM <<< "P@ssw0rd"
$ /opt/mqm/samp/bin/amqsphac DEV.QUEUE.3 STDQM <<< "P@ssw0rd"

$ export MQSERVER='DEV.APP.SVRCONN/TCP/localhost(1414)'
$ /opt/mqm/samp/bin/amqsput DEV.QUEUE.1 STDQM <<< "passw0rd"
```

## CCM side
ccdt.json
```
{
   "channel":[
      {
         "name":"DEV.APP.SVRCONN",
         "clientConnection":{
            "connection":[
               {
                  "host":"10.3.1.124",
                  "port":1414
               }],
            "queueManager":"CCMQM"
         },
         "transmissionSecurity":{
            "cipherSpecification":"TLS_RSA_WITH_AES_256_CBC_SHA256"
         },
         "type":"clientConnection"
      }
   ]
}
```
```
$ export MQSAMP_USER_ID=mquser
$ export MQCCDTURL=/tmp/config/ccdt.json
$ export MQSSLKEYR=/opt/mqHAdata/kdb/qm
$ /opt/mqm/samp/bin/amqsphac STD.ROAD.CROSSING.Q CCMQM <<< "passw0rd"
$ /opt/mqm/samp/bin/amqsphac DEV.QUEUE.3 CCMQM <<< "passw0rd"
$ /opt/mqm/samp/bin/amqsput DEV.QUEUE.3 CCMQM
```


Put message from STD side
curl -i -k https://172.17.240.120:39443/ibmmq/rest/v1/messaging/qmgr/STDQM/queue/CCM.ROAD.ACI.SUBMISSION.Q/message -X POST -u mquser:P@ssw0rd -H "ibm-mq-rest-csrf-token: blank" -H "Content-Type: text/plain;charset=utf-8" -d "Recieve fresh ACI submission!"

Consume message in CCM side
curl -i -k https://172.17.240.207:39443/ibmmq/rest/v1/messaging/qmgr/CCMQM/queue/CCM.ROAD.ACI.SUBMISSION.Q/message -X DELETE -u mquser:passw0rd -H "ibm-mq-rest-csrf-token: blank" -H "Content-Type: text/plain;charset=utf-8" 


Put message from CCM side
curl -i -k https://172.17.240.207:39443/ibmmq/rest/v1/messaging/qmgr/CCMQM/queue/STD.ROAD.CROSSING.Q/message -X POST -u mquser:passw0rd -H "ibm-mq-rest-csrf-token: blank" -H "Content-Type: text/plain;charset=utf-8" -d "Car ABC was crossed!"

Consume message in STD side
curl -i -k https://172.17.240.120:39443/ibmmq/rest/v1/messaging/qmgr/STDQM/queue/STD.ROAD.CROSSING.Q/message -X DELETE -u mquser:P@ssw0rd -H "ibm-mq-rest-csrf-token: blank" -H "Content-Type: text/plain;charset=utf-8" 