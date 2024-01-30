@REM update user as alice in application.yml
java -DMQS_KEYSTORE_CONF=/tmp/client/alice/keystore.conf -DMQS_AMSCRED_KEYFILE=/tmp/client/bob/encryptionKey -jar ams\build\libs\ams-1.0.jar