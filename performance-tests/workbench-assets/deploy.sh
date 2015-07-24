#/bin/sh
mvn clean install deploy -s settings.xml
echo "Setting up worbench-assets deployment ..."
curl -X POST --header "Authorization: Basic cGVyZlVzZXI6cGVyZlVzZXIxMjM0Ow==" http://localhost:8080/business-central/rest/deployment/org.kie.perf:workbench-assets:1.0.0-SNAPSHOT/deploy?strategy=SINGLETON
sleep 5
