/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.additional.testsuite.jdkall.present.management.api.security;

import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.integration.management.util.SecuredServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import org.jboss.eap.additional.testsuite.annotations.EAT;
import static org.junit.Assert.assertTrue;


/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@EAT({"modules/testcases/jdkAll/WildflyRelease-13.0.0.Final/management/src/main/java","modules/testcases/jdkAll/Eap72x/management/src/main/java","modules/testcases/jdkAll/Eap72x-Proposed/management/src/main/java","modules/testcases/jdkAll/Eap72x-Proposed/management/src/main/java","modules/testcases/jdkAll/Eap7Plus/management/src/main/java#7.0.0*7.4.9","modules/testcases/jdkAll/Eap71x-Proposed/management/src/main/java","modules/testcases/jdkAll/Eap71x/management/src/main/java","modules/testcases/jdkAll/Eap7.1.0.Beta/management/src/main/java","modules/testcases/jdkAll/Eap70x/management/src/main/java","modules/testcases/jdkAll/Eap70x-Proposed/management/src/main/java","modules/testcases/jdkAll/Wildfly/management/src/main/java#10.0.0.Final*25.0.0.Beta2","modules/testcases/jdkAll/ServerBeta/management/src/main/java","modules/testcases/jdkAll/WildflyRelease-20.0.0.Final/management/src/main/java","modules/testcases/jdkAll/Eap64x/management/src/main/java","modules/testcases/jdkAll/Eap64x-Proposed/management/src/main/java","modules/testcases/jdkAll/Eap63x/management/src/main/java","modules/testcases/jdkAll/Eap62x/management/src/main/java","modules/testcases/jdkAll/Eap61x/management/src/main/java"})
public class SecurityDomainTestCase extends ContainerResourceMgmtTestBase {

    @ContainerResource
    private ManagementClient managementClient;


    @Deployment(name = "secured-servlet", managed = false)
    public static Archive<?> getDeployment2() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "SecurityDomainTestCase.war");
        war.addClass(SecuredServlet.class);
        war.addAsWebInfResource(new StringAsset("<jboss-web><security-domain>test</security-domain></jboss-web>"), "jboss-web.xml");
        war.addAsWebInfResource(new StringAsset(
                "<web-app version=\"2.5\"><login-config><auth-method>BASIC</auth-method></login-config></web-app>"), "web.xml");
        return war;
    }

    @Test
    public void testAddRemoveSecurityDomain(@ArquillianResource Deployer deployer) throws Exception {


        // add security domain
        ModelNode addOp = createOpNode("subsystem=security/security-domain=test", "add");

        // setup lospecify login module options
        ModelNode addAuthClassic = createOpNode("subsystem=security/security-domain=test/authentication=classic", "add");
        ModelNode addLoginModuleOp = createOpNode("subsystem=security/security-domain=test/authentication=classic/login-module="+"Simple", "add");
        addLoginModuleOp.get("code").set("Simple");
        addLoginModuleOp.get("flag").set("required");

        executeOperation(ModelUtil.createCompositeNode(new ModelNode[]{addOp, addAuthClassic, addLoginModuleOp}));

        // deploy secured servlet
        deployer.deploy("secured-servlet");

        // check that the servlet is secured
        boolean failed = false;
        try {
            String response = HttpRequest.get(managementClient.getWebUri() + "/SecurityDomainTestCase/SecuredServlet", 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            assertTrue(e.toString().contains("Status 401"));
            failed = true;
        }
        assertTrue(failed);

        // check that the security domain is active
        try {
            String response = HttpRequest.get(managementClient.getWebUri() + "/SecurityDomainTestCase/SecuredServlet", "test", "test", 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new Exception("Unable to access secured servlet.", e);
        }

        // undeploy servlet
        deployer.undeploy("secured-servlet");


        // remove security domain
        ModelNode op = createOpNode("subsystem=security/security-domain=test", "remove");
        op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        executeOperation(op);

        // check that the security domain is removed
        failed = false;
        try {
            deployer.deploy("secured-servlet");
        } catch (Exception e) {
            failed = true;
        }
        assertTrue(failed);

    }
}
