#/bin/sh

PARAMS=""

if [ -n "$suite" ]
then
  PARAMS="$PARAMS -Dsuite=$suite"
fi

if [ -n "$scenario" ]
then
  PARAMS="$PARAMS -Dscenario=$scenario"
elif [ -n "$1" ]
then
  PARAMS="$PARAMS -Dscenario=$1"
fi

if [ -n "$startScriptLocation" ]
then
  PARAMS="$PARAMS -DstartScriptLocation=$startScriptLocation"
fi

if [ -n "$runType" ]
then
  PARAMS="$PARAMS -DrunType=$runType"
fi

if [ -n "$duration" ]
then
  PARAMS="$PARAMS -Dduration=$duration"
fi

if [ -n "$iterations" ]
then
  PARAMS="$PARAMS -Diterations=$iterations"
fi

if [ -n "$warmUp" ]
then
  PARAMS="$PARAMS -DwarmUp=$warmUp"
fi

if [ -n "$warmUpCount" ]
then
  PARAMS="$PARAMS -DwarmUpCount=$warmUpCount"
fi

if [ -n "$warmUpTime" ]
then
  PARAMS="$PARAMS -DwarmUpTime=$warmUpTime"
fi

if [ -n "$auditLogging" ]
then
  PARAMS="$PARAMS -DauditLogging=$auditLogging"
fi

if [ -n "$threads" ]
then
  PARAMS="$PARAMS -Dthreads=$threads"
fi

if [ -n "$reporterType" ]
then
  PARAMS="$PARAMS -DreporterType=$reporterType"
fi

if [ -n "$periodicity" ]
then
  PARAMS="$PARAMS -Dperiodicity=$periodicity"
fi

if [ -n "$reportDataLocation" ]
then
  PARAMS="$PARAMS -DreportDataLocation=$reportDataLocation"
fi

if [ -n "$perfRepo_host" ]
then
  PARAMS="$PARAMS -DperfRepo.host=$perfRepo_host"
fi

if [ -n "$perfRepo_urlPath" ]
then
  PARAMS="$PARAMS -DperfRepo.urlPath=$perfRepo_urlPath"
fi

if [ -n "$perfRepo_username" ]
then
  PARAMS="$PARAMS -DperfRepo.username=$perfRepo_username"
fi

if [ -n "$perfRepo_password" ]
then
  PARAMS="$PARAMS -DperfRepo.password=$perfRepo_password"
fi

if [ -n "$remoteAPI" ]
then
  PARAMS="$PARAMS -DremoteAPI=$remoteAPI"
fi

if [ -n "$jbpm_runtimeManagerStrategy" ]
then
  PARAMS="$PARAMS -Djbpm.runtimeManagerStrategy=$jbpm_runtimeManagerStrategy"
fi

if [ -n "$workbench_username" ]
then
  PARAMS="$PARAMS -Dworkbench.username=$workbench_username"
fi

if [ -n "$workbench_password" ]
then
  PARAMS="$PARAMS -Dworkbench.password=$workbench_password"
fi

if [ -n "$workbench_host" ]
then
  PARAMS="$PARAMS -Dworkbench.host=$workbench_host"
fi

if [ -n "$workbench_port" ]
then
  PARAMS="$PARAMS -Dworkbench.port=$workbench_port"
fi

if [ -n "$workbench_name" ]
then
  PARAMS="$PARAMS -Dworkbench.name=$workbench_name"
fi

if [ -n "$workbench_remotingPort" ]
then
  PARAMS="$PARAMS -Dworkbench.remotingPort=$workbench_remotingPort"
fi

if [ -n "$workbench_jms_connectionFactory" ]
then
  PARAMS="$PARAMS -Dworkbench.jms.connectionFactory=$workbench_jms_connectionFactory"
fi

if [ -n "$workbench_jms_queue_kieSession" ]
then
  PARAMS="$PARAMS -Dworkbench.jms.queue.kieSession=$workbench_jms_queue_kieSession"
fi

if [ -n "$workbench_jms_queue_kieTask" ]
then
  PARAMS="$PARAMS -Dworkbench.jms.queue.kieTask=$workbench_jms_queue_kieTask"
fi

if [ -n "$workbench_jms_queue_kieResponse" ]
then
  PARAMS="$PARAMS -Dworkbench.jms.queue.kieResponse=$workbench_jms_queue_kieResponse"
fi

if [ -n "$workbench_jms_sslEnabled" ]
then
  PARAMS="$PARAMS -Dworkbench.jms.sslEnabled=$workbench_jms_sslEnabled"
fi

if [ -n "$workbench_jms_ssl_keystoreLocation" ]
then
  PARAMS="$PARAMS -Dworkbench.jms.ssl.keystoreLocation=$workbench_jms_ssl_keystoreLocation"
fi

if [ -n "$workbench_jms_ssl_keystorePassword" ]
then
  PARAMS="$PARAMS -Dworkbench.jms.ssl.keystorePassword=$workbench_jms_ssl_keystorePassword"
fi

mvn clean install exec:exec $PARAMS
