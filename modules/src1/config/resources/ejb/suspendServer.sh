# @EAT({"modules/testcases/jdkAll/Wildfly-20.0.0.Final/ejb/test2-configurations/src/test/resources","modules/testcases/jdkAll/WildflyRelease-24.0.0.Final/ejb/test-configurations2/src/test/resources","modules/testcases/jdkAll/WildflyRelease-13.0.0.Final/ejb/test2-configurations#13.0.0","modules/testcases/jdkAll/Wildfly/ejb/test2-configurations#13.0.0","modules/testcases/jdkAll/ServerBeta/ejb/test2-configurations#13.0.0","modules/testcases/jdkAll/Eap72x/ejb/test2-configurations","modules/testcases/jdkAll/Eap72x-Proposed/ejb/test2-configurations","modules/testcases/jdkAll/Eap71x/ejb/test2-configurations","modules/testcases/jdkAll/Eap71x-Proposed/ejb/test2-configurations","modules/testcases/jdkAll/Eap7Plus/ejb/test2-configurations"}) 

PPIDD=$(ps -aux | grep "standalone.sh" | grep "bin" | awk '{print $2}')
echo $PPIDD
for childprocess in $(ps -o pid --ppid $PPIDD | tail -n +2); 
    do kill -TSTP $childprocess; 
    echo $childprocess;
done;
#kill -TSTP $(ps -aux | grep "standalone.sh" | grep "bin" | awk '{print $2}')

