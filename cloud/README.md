# Install IBM MQ on Ubuntu server

### Add 'mqm' and 'mqclient' group, user 'mqm', 'mqadm', and 'app' user
```
$ groupadd -g 2001 mqm
$ useradd -m -g 2001 -u 2001 -s /bin/bash mqm
$ usermod -aG sudo mqm

$ useradd -m -g 2001 -s /bin/bash mqadm

$ groupadd -g 909 mqclient
$ useradd -m -g 909 -u 909 -s /bin/bash app
```

Add below line into .profile  
`. /opt/mqm/bin/setmqenv -s ` 

### Download and install IBM MQ
login as user 'root'

#### 
```
Ubuntu server
$ wget  https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/messaging/mqadv/9.3.4.0-IBM-MQ-Advanced-for-Developers-UbuntuLinuxX64.tar.gz

$ tar zxvf 9.3.4.0-IBM-MQ-Advanced-for-Developers-UbuntuLinuxX64.tar.gz

RedHat server
$ wget https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/messaging/mqadv/9.3.4.0-IBM-MQ-Advanced-for-Developers-LinuxX64.tar.gz

$ tar zxvf 9.3.4.0-IBM-MQ-Advanced-for-Developers-LinuxX64.tar.gz

$ mv MQServer /var/tmp/
$ cd /var/tmp/MQServer
$ ./mqlicense.sh -text_only -accept
```

#### Ubuntu server
add below into /etc/apt/sources.list.d/IBM_MQ.list  
`deb [trusted=yes] file:/var/tmp/MQServer ./`

```
$ sudo apt-get update
$ sudo apt-get install "ibmmq-*"
```

#### RedHat server  
`rpm -ivh MQSeries*.rpm`


#### After installation, set as primary installation  
`$ setmqinst -i -p /opt/mqm/`

### Install NFS service
#### server side
```
$ sudo apt install nfs-kernel-server nfs-common
$ sudo mkdir /opt/nfs
$ sudo nano /etc/exports
 /opt/nfs  *(rw,sync,no_subtree_check,no_root_squash) (export the directory)

$ sudo systemctl restart nfs-kernel-server.service
$ sudo systemctl status nfs-kernel-server.service
$ showmount -e localhost
$ sudo systemctl enable nfs-kernel-server (auto start after reboot)
```

#### client side
```
$ sudo mkdir /opt/mqHAdata
$ sudo mount 127.0.0.1:/opt/nfs /opt/mqHAdata

$ yum install nfs-utils (Redhat server)
$ sudo nano /etc/fstab
127.0.0.1:/opt/nfs /opt/mqHAdata nfs rsize=8192,wsize=8192,timeo=14,intr (auto mount after reboot)
```

### Update system resource limits
```
$ ulimit -a
$ vi /etc/security/limits.d/30-ibmmq.conf
# IBM MQ nofile limits
mqm 	- 	nofile 	65536
root	-	nofile	65536

$ sysctl -p
```

#### Generate key repository for queue manager
```
mkdir -p /opt/mqHAdata/kdb/qm
cd /opt/mqHAdata/kdb/qm

$ runmqakm -keydb -create -db qm.kdb -pw passw0rd -expire 1000 -type cms -stash
$ runmqckm -cert -create -db qm.kdb -dn "CN=ibm-mq-vm2,OU=QM,O=testing,C=HK" -pw passw0rd -label ccmqm -size 2048 -expire 365 -sig_alg SHA256_WITH_RSA
$ runmqakm -cert -details -label ccmqm -db qm.kdb -stashed
```

Prepare the mqsc file **hasamples.txt** for queue, channel, authentication info, authority record definition
```
STOP LISTENER('SYSTEM.DEFAULT.LISTENER.TCP') IGNSTATE(YES)

DEFINE QLOCAL('DEV.QUEUE.1') DEFPSIST(YES) REPLACE
DEFINE QLOCAL('DEV.QUEUE.2') DEFPSIST(YES) REPLACE
DEFINE QLOCAL('DEV.QUEUE.3') DEFPSIST(YES) REPLACE

ALTER QMGR DEADQ('DEV.DEAD.LETTER.QUEUE')

DEFINE AUTHINFO('DEV.AUTHINFO') AUTHTYPE(IDPWOS) CHCKCLNT(REQDADM) AUTHENMD(PAM) CHCKLOCL(OPTIONAL) ADOPTCTX(YES) REPLACE
ALTER QMGR CONNAUTH('DEV.AUTHINFO')
REFRESH SECURITY(*) TYPE(CONNAUTH)

DEFINE CHANNEL('DEV.APP.SVRCONN') CHLTYPE(SVRCONN) TRPTYPE(TCP) SSLCIPH(ANY_TLS12_OR_HIGHER) SSLCAUTH(OPTIONAL) REPLACE

SET CHLAUTH('*') TYPE(ADDRESSMAP) ADDRESS('*') USERSRC(NOACCESS) DESCR('Back-stop rule - Blocks everyone') ACTION(REPLACE)
SET CHLAUTH('DEV.APP.SVRCONN') TYPE(ADDRESSMAP) ADDRESS('*') USERSRC(CHANNEL) CHCKCLNT(REQUIRED) DESCR('Allows connection via APP channel') ACTION(REPLACE)

DEFINE LISTENER('DEV.LISTENER.TCP') TRPTYPE(TCP) PORT(1414) CONTROL(QMGR) REPLACE

SET AUTHREC PRINCIPAL('app') OBJTYPE(QMGR) AUTHADD(CONNECT,INQ)
SET AUTHREC PRINCIPAL('tommy') OBJTYPE(QMGR) AUTHADD(CONNECT,INQ)

SET AUTHREC PROFILE('DEV.**') PRINCIPAL('app') OBJTYPE(QUEUE) AUTHADD(BROWSE,GET,INQ,PUT)
SET AUTHREC PROFILE('DEV.**') PRINCIPAL('tommy') OBJTYPE(QUEUE) AUTHADD(BROWSE,GET,INQ,PUT)


ALTER QMGR SSLKEYR('/opt/mqHAdata/kdb/qm') CERTLABL('ccmqm')
REFRESH SECURITY(*) TYPE(SSL)
```

#### Create queue manager
```
$ crtmqm -lc -lp 3 -ls 2 -lf 4096 -md /opt/mqHAdata/qmgrs -ld /opt/mqHAdata/logs -oa UserExternal QM1
$ strmqm -x QM1
$ runmqsc QM1 -f /opt/mqHAdata/hasamples.txt (Create queue, channel, etc.)
```

#### Add standby queue manager
```
addmqinf -s QueueManager -v Name=QM1 -v Directory=QM1 -v Prefix=/var/mqm -v DataPath=/opt/mqHAdata/qmgrs/QM1

$ strmqm -x QM1
$ dspmq -xf
```

### Setup mqweb console
Use local OS account to authenticate the user for web console  
`$ cp /opt/mqm/web/mq/samp/configuration/local_os_registry.xml /var/mqm/web/installations/Installation1/servers/mqweb/mqwebuser.xml`

#### Update SSL configuration with self sign certificate
```
$ cd /var/mqm/web/installations/Installation1/servers/mqweb/resources/security

$ runmqckm -cert -create -db key.jks -dn "CN=ibm-mq-vm2,OU=QM,O=testing,C=HK" -pw password -label ccmqm
```

Update /var/mqm/web/installations/Installation1/servers/mqweb/mqwebuser.xml with below block
```
<keyStore id="defaultKeyStore" location="key.jks" type="JKS" password="password"/>
<!-- <keyStore id="defaultTrustStore" location="trust.jks" type="JKS" password="password"/> --> 
<ssl id="thisSSLConfig" clientAuthenticationSupported="true" keyStoreRef="defaultKeyStore" serverKeyAlias="ccmqm" trustStoreRef="defaultTrustStore" sslProtocol="TLSv1.2"/>
<sslDefault sslRef="thisSSLConfig"/> 
```

Comment out the default SSL configuration  
`<!-- sslDefault sslRef="mqDefaultSSLConfig"/ -->`

#### Start mqweb console
```
$ strmqweb
$ setmqweb properties -k httpHost -v "*"
$ dspmqweb
```

#### Firewall setting
```
$ sudo firewall-cmd --get-active-zones
$ sudo firewall-cmd --zone=public --list-services
$ sudo firewall-cmd --zone=public --add-port=9443/tcp
$ sudo firewall-cmd --zone=public --add-port=1414/tcp
$ sudo firewall-cmd --zone=public --list-ports

$ sudo systemctl stop firewalld
$ sudo systemctl disable firewalld
```

#### Test put message into the queue
Use REST api
```
curl -i -k -X POST  -u app:passw0rd --header "Content-Type: text/plain; charset=utf-8" --header "Accept: application/json" --header "ibm-mq-rest-csrf-token: blank" --header "ibm-mq-md-expiry: unlimited" --header "ibm-mq-md-persistence: persistent" -d "This is a persistent message" https://172.16.241.205:9444/ibmmq/rest/v3/messaging/qmgr/QM1/queue/DEV.QUEUE.1/message
```
Use sample program 
```
$  export MQSAMP_USER_ID=app
$ /opt/mqm/samp/bin/amqsput DEV.QUEUE.1 QM1
```

Extract certificate from key repository and import into truststore for client application connecting to queue manager
```
$ runmqckm -cert -extract -db qm.kdb -label ccmqm -target vcs-ccmqm.crt
$ keytool -import -keystore clientkey.jks -file vcs-ccmqm.crt -alias vcs-ccmmq
```

### Uninstall IBM MQ
login with root
```
$ endmqm QM1
$ dltmqm QM1

$ apt-get remove "ibmmq-*"
$ apt-get purge "ibmmq-*"
$ rm -fr /var/mqm
$ rm -fr /etc/opt/mqm
```

### Links
https://uplinktv.github.io/ibmmq-devops/