#!/bin/sh

$JBOSS_HOME/bin/add-user.sh -a perfUser 'perfUser1234;'
$JBOSS_HOME/bin/add-user.sh -a engUser 'engUser1234;'
echo "perfUser=admin" >> $JBOSS_HOME/standalone/configuration/application-roles.properties
echo "engUser=admin,engineering" >> $JBOSS_HOME/standalone/configuration/application-roles.properties

