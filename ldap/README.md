## Generate CA and server certificate
```
CA key
$ openssl genrsa -aes256 -out ca-key.pem 4096 - passphase: passw0rd

CA certficate
$ openssl req -new -x509 -nodes -sha256 -days 3650 -key ca-key.pem -out ca-cert.pem

Server key and CSR
$ openssl req -new -nodes -newkey rsa:4096 -keyout server-key.pem -out server-cert.csr

Sign server csr and generate server certficate
$ openssl x509 -req -in server-cert.csr -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial -out server-cert.pem -days 3650 -sha256

Verify server certficate
$ openssl verify -CAfile ca-cert.pem ca-cert.pem server-cert.pem
```

## Import CA and server certficates into key database
```
$ cd /tmp/keys
$ runmqakm -keydb -create -db ldapserver.kdb -pw passw0rd -expire 1000 -type cms -stash
$ runmqakm -cert -add -db ldapserver.kdb -file ca-cert.pem -stashed
$ runmqckm -cert -import -file server-cert.pem -pw passw0rd -type pkcs12 -target ldapserver.kdb -target_pw passw0rd -target_type cms
$ runmqckm -cert -rename -db ldapserver.kdb -label "CN=ccm.local,OU=TSW,O=C&ED,L=HK,ST=Some-State,C=AU" -new_label ldapserver -stashed 
$ runmqakm -cert -details -label ldapserver -db ldapserver.kdb -stashed
$ chmod 666 ld*
```

## Configure LDAP
```
$ endmqm -i EARTH && dltmqm EARTH
$ crtmqm -u DEV.DEAD.LETTER.QUEUE -ll -lp 3 -ls 2 -lf 4096 -oa UserExternal QM1
$ strmqm -x QM1

$ runmqsc QM1 <<EOF
DEFINE AUTHINFO(DEV.IDPW.LDAP) AUTHTYPE(IDPWLDAP) CONNAME('saturn(636)') SHORTUSR('uid') ADOPTCTX(YES) AUTHORMD(SEARCHUSR) FINDGRP(memberOf) CHCKCLNT(OPTIONAL) CHCKLOCL(NONE) BASEDNG('ou=ibmmq,dc=ccm,dc=local') CLASSGRP('groupOfNames') GRPFIELD('cn') BASEDNU('ou=ibmmq,dc=ccm,dc=local') CLASSUSR('inetOrgPerson') LDAPPWD('passw0rd') LDAPUSER('cn=admin,dc=ccm,dc=local') SECCOMM(YES) replace
ALTER QMGR CONNAUTH(DEV.IDPW.LDAP) SSLFIPS(NO) SUITEB(NONE) SSLKEYR('/tmp/keys/ldapserver') CERTLABL('ldapserver')
REFRESH SECURITY
EOF

$ endmqm -i QM1 && strmqm -x QM1

$ runmqsc QM1 <<EOF
DEFINE QLOCAL (TEST_Q) REPLACE
DEFINE CHANNEL (DEV.APP.SVRCONN) CHLTYPE(SVRCONN) SSLCAUTH (OPTIONAL) replace
SET AUTHREC PROFILE('TEST_Q') GROUP('client') OBJTYPE(QUEUE) AUTHADD(BROWSE,GET,INQ,PUT)
SET AUTHREC OBJTYPE(QMGR) GROUP('client') AUTHADD(connect)
SET AUTHREC OBJTYPE(QMGR) GROUP('mqadmin') AUTHADD(connect)

START CHANNEL (DEV.APP.SVRCONN)

START LISTENER(SYSTEM.DEFAULT.LISTENER.TCP)

DIS CHSTATUS(DEV.APP.SVRCONN) ALL

DIS QMSTATUS LDAPCONN
DIS QMGR
EOF
```

## Troubleshoot
```
$ tail -100 /var/mqm/qmgrs/QM1/errors/AMQERR01.LOG
```

## Create LDAP accounts on Openldap server (Saturn)
```
# Optional
$ ldapdelete -D "cn=admin,dc=ccm,dc=local" -w passw0rd ou=ibmmq,dc=ccm,dc=local

cd /tmp/accounts

$ ldapadd -x -D "cn=admin,dc=ccm,dc=local" -w passw0rd -f ibmmq-ou.ldif
$ ldapadd -x -D "cn=admin,dc=ccm,dc=local" -w passw0rd -f mqm-group.ldif
$ ldapadd -x -D "cn=admin,dc=ccm,dc=local" -w passw0rd -f admin.ldif
$ ldapadd -x -D "cn=admin,dc=ccm,dc=local" -w passw0rd -f client-group.ldif
$ ldapadd -x -D "cn=admin,dc=ccm,dc=local" -w passw0rd -f bob.ldif

# Optional
$ ldapadd -x -D "cn=admin,dc=ccm,dc=local" -w passw0rd -f mquser.ldif
$ ldapmodify -x -D "cn=admin,dc=ccm,dc=local" -w passw0rd -f addBobToUserGroup.ldif

# Optional - change ldap account password
$ ldappasswd -s passw0rd -w passw0rd -D "cn=admin,dc=ccm,dc=local" -x "cn=adm,cn=mqadmin,ou=ibmmq,dc=ccm,dc=local"

# Verify, simulate checking account from Liberty application server
$ ldapsearch -D "cn=admin,dc=ccm,dc=local" -p 389 -h localhost -w passw0rd  -b "dc=ccm,dc=local" -s sub -x -a always "(objectclass=*)" "ibm-entryuuid uid objectClass"
```

## Grant administrator privilege to admin group  
```
$ dmpmqaut -m QM1
$ /opt/mqm/samp/bin/amqauthg.sh QM1 mqadmin
```


## Test put/get message
```
$ export MQSERVER='DEV.APP.SVRCONN/TCP/earth(1414)'
$ # export MQSAMP_USER_ID="cn=bob,ou=ibmmq,dc=ccm,dc=local"
$ export MQSAMP_USER_ID=bob / adm
$ /opt/mqm/samp/bin/amqsphac TEST_Q QM1 <<< "passw0rd"
$ /opt/mqm/samp/bin/amqsputc TEST_Q QM1
```

## Configure mqweb console integration with LDAP
```
$ docker cp ldap_registry.xml ldap-earth-1:/var/mqm/web/installations/Installation1/servers/mqweb/mqwebuser.xml
$ tail -f /var/mqm/web/installations/Installation1/servers/mqweb/logs/console.log
```

## Reference
https://medium.com/@ivansla/connecting-containerized-ibm-mq-to-ldap-s-3da2ab46e3a6
https://www.mqtechconference.com/sessions_v2014/IBM_MQ_Connection_Authentication.pdf
https://marketaylor.synology.me/?p=541
https://blogs.perficient.com/2019/08/05/how-to-configure-ibm-mq-authentication-os-and-ldap/
https://tylersguides.com/guides/openldap-memberof-overlay/