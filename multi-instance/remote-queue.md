#### In node2
```
<!-- If don't use default queue manager created by container -->
$ endmqm -i MOON && dltmqm MOON
$ crtmqm -u DEV.DEAD.LETTER.QUEUE MOON
$ strmqm -x MOON

$ runmqsc MOON <<EOF
DEFINE QLOCAL (MOON_Q1.R) REPLACE
DEFINE CHANNEL (EARTH.TO.MOON) CHLTYPE (RCVR) TRPTYPE (TCP) REPLACE
START CHANNEL (EARTH.TO.MOON)

** START LISTENER(SYSTEM.DEFAULT.LISTENER.TCP)

SET CHLAUTH('*') TYPE(QMGRMAP) QMNAME('*') DESCR('Block access from a remote queue manager') USERSRC(NOACCESS)
SET CHLAUTH('EARTH.TO.MOON') TYPE(QMGRMAP) QMNAME(earth) ADDRESS(*) MCAUSER('mqm') ACTION(REPLACE)

SET AUTHREC PROFILE('MOON_Q1.R') PRINCIPAL('app') OBJTYPE(QUEUE) AUTHADD(BROWSE,GET,INQ,PUT)
EOF

<!-- Configure SSL/TLS for channel -->
runmqsc MOON <<EOF
ALTER QMGR SSLKEYR('/tmp/config/sslkeyr/qm') CERTLABL('ibmwebspheremqqm')

ALTER CHANNEL('DEV.APP.SVRCONN') CHLTYPE(SVRCONN) SSLCIPH(ANY_TLS12_OR_HIGHER) SSLCAUTH(OPTIONAL)
REFRESH SECURITY(*) TYPE(SSL)
EOF

<!-- Sometimes need to restart queue manager -->
$ endmqm MOON
$ strmqm MOON

<!-- Test get message -->
/opt/mqm/samp/bin/amqsget MOON_Q1.R MOON

$ tail -f /var/mqm/qmgrs/MOON/errors/AMQERR01.LOG
```

#### In node1
```
<!-- if don't use default queue manager created by container -->
$ endmqm -i EARTH && dltmqm EARTH
$ crtmqm -u DEV.DEAD.LETTER.QUEUE EARTH
$ strmqm -x EARTH

$ runmqsc EARTH <<EOF
DEFINE QLOCAL (MOON_XMITQ) USAGE (XMITQ) REPLACE
DEFINE QREMOTE (MOON_Q1.W) RNAME (MOON_Q1.R) RQMNAME(MOON) XMITQ (MOON_XMITQ) REPLACE
DEFINE CHANNEL(EARTH.TO.MOON) CHLTYPE(SDR) CONNAME('moon') XMITQ(MOON_XMITQ) TRPTYPE(TCP) REPLACE
START CHANNEL (EARTH.TO.MOON)

** START LISTENER(SYSTEM.DEFAULT.LISTENER.TCP)

SET AUTHREC PROFILE('MOON_Q1.W') PRINCIPAL('app') OBJTYPE(QUEUE) AUTHADD(BROWSE,GET,INQ,PUT)
EOF

<!-- Configure SSL/TLS for channel -->
$ runmqsc EARTH <<EOF
ALTER QMGR SSLKEYR('/tmp/config/sslkeyr/qm') CERTLABL('ibmwebspheremqqm')

ALTER CHANNEL('DEV.APP.SVRCONN') CHLTYPE(SVRCONN) SSLCIPH(ANY_TLS12_OR_HIGHER) SSLCAUTH(OPTIONAL)
REFRESH SECURITY(*) TYPE(SSL)
EOF

<!-- Sometimes need to restart queue manager -->
$ endmqm EARTH
$ strmqm EARTH

<!-- Test put message -->
$ /opt/mqm/samp/bin/amqsput MOON_Q1.W EARTH

$ tail -f /var/mqm/qmgrs/EARTH/errors/AMQERR01.LOG
```

##### Test put/get message from client
```
<!-- Enter venus container in one terminal -->
$ export MQSERVER='DEV.APP.SVRCONN/TCP/earth(1414)'
$ unset MQCCDTURL
$ /opt/mqm/samp/bin/amqsphac MOON_Q1.W EARTH <<< "passw0rd"

<!-- Enter venus container in another terminal -->
$ export MQSERVER='DEV.APP.SVRCONN/TCP/moon(1414)'
$ unset MQCCDTURL
$ /opt/mqm/samp/bin/amqsghac MOON_Q1.R MOON <<< "passw0rd"
```

#### Reference
https://www.ibm.com/docs/en/ibm-mq/9.3?topic=tutorials-tutorial-2-sending-message-remote-queue