#!/bin/sh -xe

serverKeyPass=SERVER_KEYSTORE_PASSWORD
clientKeyPass=CLIENT_KEYSTORE_PASSWORD

mkdir -p server
serverKeystore=server/keystore.jks
clientKeystore=client_keystore.jks
serverTruststore=server/truststore.jts
clientCertificate=client.cer

clientAlias=client
serverAlias=server

rm -f $serverKeystore $clientKeystore $serverTruststore

# create client keystore
keytool -genkey -alias $clientAlias -keyalg RSA -keystore $clientKeystore -storepass $clientKeyPass -keysize 2048 -storetype pkcs12
# create client certficate from client keystore
keytool -exportcert -keystore $clientKeystore -storetype pkcs12 -storepass $clientKeyPass -alias $clientAlias -file $clientCertificate
# create server keystore
keytool -genkey -alias $clientAlias -keyalg RSA -keystore $serverKeystore -storepass $serverKeyPass
# create server trustore and import client certificate into it
keytool -import -file $clientCertificate -alias $clientAlias -keystore $serverTruststore -storepass $serverKeyPass

# list certificates
keytool -list -v -keystore $serverKeystore -storepass $serverKeyPass

