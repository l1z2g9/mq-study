### Prepare channel, queue and listener
```
$ runmqsc EARTH <<EOF
DEFINE QLOCAL (SEC.Q) REPLACE

** DEFINE CHANNEL('AMS.SVRCONN') CHLTYPE(SVRCONN) TRPTYPE(TCP) SSLCIPH(ANY_TLS12_OR_HIGHER) SSLCAUTH(OPTIONAL) REPLACE
DEFINE CHANNEL('AMS.SVRCONN') CHLTYPE(SVRCONN) TRPTYPE(TCP) SSLCIPH(ANY_TLS12_OR_HIGHER) SSLCAUTH(OPTIONAL)

SET CHLAUTH('AMS.SVRCONN') TYPE(ADDRESSMAP) ADDRESS('*') USERSRC(CHANNEL) CHCKCLNT(REQUIRED) ACTION(REPLACE)

ALTER QMGR SSLKEYR('/tmp/config/sslkeyr/qm') CERTLABL('ibmwebspheremqqm')
REFRESH SECURITY(*) TYPE(SSL)

DEFINE LISTENER(AMS.LSTR) TRPTYPE(TCP) PORT(1415) CONTROL(QMGR)
START LISTENER(AMS.LSTR)
EOF
```

### Configure authority record for the queue
```
$ setmqaut -m EARTH -t qmgr -p alice -p bob +connect +inq
$ setmqaut -m EARTH -n SEC.Q -t queue -p alice +put
$ setmqaut -m EARTH -n SEC.Q -t queue -p bob +get +inq +browse

$ setmqaut -m EARTH -t queue -n SYSTEM.PROTECTION.POLICY.QUEUE -p alice -p bob +browse
$ setmqaut -m EARTH -t queue -n SYSTEM.PROTECTION.ERROR.QUEUE -p alice -p bob +put
```

### Create key database for alice
```
$ runmqakm -keydb -create -db /tmp/alice/alicekey.kdb -pw passw0rd -stash
$ chmod +r /tmp/alice/alicekey.kdb
$ runmqakm -cert -create -db /tmp/alice/alicekey.kdb -pw passw0rd -label Alice_Cert -dn "cn=alice,O=IBM,c=GB" -default_cert yes
$ chown alice /tmp/alice/alicekey.kdb /tmp/alice/alicekey.sth
$ chmod 600 /tmp/alice/alicekey.kdb /tmp/alice/alicekey.sth
```

/tmp/alice/keystore.conf
```
cms.keystore = /tmp/alice/alicekey
cms.certificate = Alice_Cert

jks.keystore = /tmp/alice/keystore
jks.certificate = Alice_Cert
jks.encrypted = no
jks.keystore_pass = passw0rd
jks.key_pass = passw0rd
jks.provider = IBMJCE
```

Protect the keystore.conf file providing the encryption key file.  

`/opt/mqm/java/bin/runamscred -f /tmp/alice/keystore.conf -sf encryptionKey`


### Create key database for bob
```
$ runmqakm -keydb -create -db /tmp/bob/bobkey.kdb -pw passw0rd -stash
$ chmod +r /tmp/bob/bobkey.kdb
$ runmqakm -cert -create -db /tmp/bob/bobkey.kdb -pw passw0rd -label Bob_Cert -dn "cn=bob,O=IBM,c=GB" -default_cert yes
$ chown bob /tmp/bob/bobkey.kdb /tmp/bob/bobkey.sth
$ chmod 600 /tmp/bob/bobkey.kdb /tmp/bob/bobkey.sth
```

/tmp/bob/keystore.conf
```
cms.keystore = /tmp/bob/bobkey
cms.certificate = Bob_Cert

jks.keystore = /tmp/bob/keystore
jks.certificate = Bob_Cert
jks.encrypted = no
jks.keystore_pass = passw0rd
jks.key_pass = passw0rd
jks.provider = IBMJCE
```

Protect the keystore.conf file providing the encryption key file.  
`/opt/mqm/java/bin/runamscred -f /tmp/bob/keystore.conf -sf encryptionKey`

### Share the certificates between the two key databases 
```
Extract alice's certificate (don't use runmqakm -cert -export)
$ runmqakm -cert -extract -db /tmp/alice/alicekey.kdb -pw passw0rd -label Alice_Cert -target /tmp/alice/alice_public.arm

Import it into bob's keystore
$ runmqakm -cert -add -db /tmp/bob/bobkey.kdb -pw passw0rd -label Alice_Cert -file /tmp/alice/alice_public.arm

Verify alice's certificate
$ runmqakm -cert -details -db /tmp/bob/bobkey.kdb -pw passw0rd -label Alice_Cert

Extract bob's certificate
$ runmqakm -cert -extract -db /tmp/bob/bobkey.kdb -pw passw0rd -label Bob_Cert -target /tmp/bob/bob_public.arm

Import it into alice's keystore
$ runmqakm -cert -add -db /tmp/alice/alicekey.kdb -pw passw0rd -label Bob_Cert -file /tmp/bob/bob_public.arm

Verify bob's certificate
$ runmqakm -cert -details -db /tmp/alice/alicekey.kdb -pw passw0rd -label Bob_Cert
```

### Convert kdb to jks
```
$ runmqckm -keydb -create -db /tmp/alice/keystore.jks -type jks
$ runmqckm -cert -import -db /tmp/alice/alicekey.kdb -type cms -pw passw0rd -target /tmp/alice/keystore.jks -target_type jks -target_pw passw0rd
$ keytool -list -keystore /tmp/alice/keystore.jks -storepass passw0rd

$ runmqckm -keydb -create -db /tmp/bob/keystore.jks -type jks
$ runmqckm -cert -import -db /tmp/bob/bobkey.kdb -type cms -pw passw0rd -target /tmp/bob/keystore.jks -target_type jks -target_pw passw0rd
$ keytool -list -keystore /tmp/bob/keystore.jks -storepass passw0rd
```

### Define queue policy
```
$ setmqspl -m EARTH -p SEC.Q -s SHA256 -a "CN=alice,O=IBM,C=GB" -e AES256 -r "CN=bob,O=IBM,C=GB"
$ dspmqspl -m EARTH
$ dspmqspl -m EARTH -export > restore_my_policies.bat
```

### Test the setup
```
Open one terminal 
$ export MQSAMP_USER_ID=alice
$ export MQS_KEYSTORE_CONF=/tmp/alice/keystore.conf
$ export MQSSLKEYR=/tmp/config/sslkeyr/qm
$ /opt/mqm/samp/bin/amqsput SEC.Q EARTH

Open another terminal 
$ export MQSAMP_USER_ID=bob
$ export MQS_KEYSTORE_CONF=/tmp/bob/keystore.conf
$ export MQSSLKEYR=/tmp/config/sslkeyr/qm
$ /opt/mqm/samp/bin/amqsget SEC.Q EARTH <<< "passw0rd"
```

### Testing encryption
```
$ runmqsc EARTH <<EOF
DEFINE QALIAS(SEC.Q.ALIAS) TARGET(SEC.Q) REPLACE
EOF

$ setmqaut -m EARTH -n SEC.Q.ALIAS -t queue -p bob +browse

$ export MQSAMP_USER_ID=alice
$ export MQS_KEYSTORE_CONF=/tmp/alice/keystore.conf
$ export MQSSLKEYR=/tmp/config/sslkeyr/qm
$ /opt/mqm/samp/bin/amqsput SEC.Q EARTH


$ export MQSAMP_USER_ID=bob
$ export MQS_KEYSTORE_CONF=/tmp/bob/keystore.conf
$ export MQSSLKEYR=/tmp/config/sslkeyr/qm

View encrypted data
$ /opt/mqm/samp/bin/amqsbcg SEC.Q.ALIAS EARTH <<< "passw0rd"

View decrypted data
$ /opt/mqm/samp/bin/amqsbcg SEC.Q EARTH <<< "passw0rd"
$ /opt/mqm/samp/bin/amqsget SEC.Q EARTH <<< "passw0rd"
```

### Reference
https://www.ibm.com/docs/en/ibm-mq/9.3?topic=ams-quick-start-guide-aix-linux

https://www.ibm.com/docs/en/ibm-mq/9.3?topic=ams-quick-start-guide-java-clients
