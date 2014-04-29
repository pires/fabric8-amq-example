/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.pires.example.client.test;

import com.github.pires.example.client.AMQClient;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import static io.fabric8.tooling.testing.pax.exam.karaf.FabricKarafTestSupport.executeCommand;
import static java.lang.System.err;
import org.junit.After;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class AMQClientTest extends FabricTestSupport {

  ServiceProxy<FabricService> fabricService;

  @Configuration
  public Option[] config() {
    return new Option[] {
        new DefaultCompositeOption(fabricDistributionConfiguration()),
        mavenBundle("com.github.pires.example", "client").versionAsInProject() };
  }

  @Before
  public void setUp() {
    fabricService = ServiceProxy.createServiceProxy(bundleContext,
        FabricService.class);

    // bootstrap ensemble
    err.println(executeCommand("create -n --wait-for-provisioning"));
    // err.println(executeCommand("features:install mq-fabric"));

    // create master-slave broker topology
    err.println(executeCommand("mq-create --group a broker1"));
    err.println(executeCommand("container-create-child --jmx-user admin --jmx-password admin --profile mq-broker-a.broker1 root broker1c1"));
    err.println(executeCommand("container-create-child --jmx-user admin --jmx-password admin --profile mq-broker-a.broker1 root broker1c2"));
    // create app profile
    err.println(executeCommand("profile-create --parents mq-client-a --parents mq-client mq-example"));
    err.println(executeCommand("profile-edit --features scr mq-example"));
    err.println(executeCommand("profile-edit --bundles mvn:com.github.pires.example/client/0.1-SNAPSHOT mq-example"));
    err.println(executeCommand("profile-edit --bundles mvn:com.github.pires.example/client-impl/0.1-SNAPSHOT mq-example"));

    ContainerBuilder.create(fabricService).withName("jmstest")
        .withProfiles("mq-example").assertProvisioningResult().build();

    // err.println(executeCommand("container-add-profile root mq-example"));
    // err.println(executeCommand("profile-refresh mq-example"));
    // err.println(executeCommand("osgi:install -s mvn:com.github.pires.example/client/0.1-SNAPSHOT"));
    // err.println(executeCommand("osgi:install -s mvn:com.github.pires.example/client-impl/0.1-SNAPSHOT"));
    // assign container to broker
    err.println(executeCommand("mq-create --assign-container jmstest1 --group a broker1"));
    err.println(executeCommand("container-connect -u admin -p admin jmstest1 scr:list | grep example"));
    // err.println(executeCommand("scr:list | grep example"));
  }

  @After
  public void tearDown() {
    Container root = fabricService.getService().getContainer("root");
    for (Container container : root.getChildren()) {
      container.destroy();
    }
    root.destroy();
  }

  @Test
  public void should_send_message() {
    // assert our bundle is working as expected
    for (Bundle bundle : bundleContext.getBundles()) {
      if (bundle.getSymbolicName().contains("pires")) {
        err.println("Found bundle " + bundle.getSymbolicName());
        if (bundle.getRegisteredServices() == null) {
          err.println("No services registered.");
        } else {
          err.println("Listing " + bundle.getRegisteredServices().length
              + " service references..");
          for (ServiceReference sr : bundle.getRegisteredServices()) {
            err.println(sr.getClass().getName());
          }
        }
      }
    }
    ServiceReference<AMQClient> sr = bundleContext
        .getServiceReference(AMQClient.class);
    assertNotNull(sr);
    AMQClient client = bundleContext.getService(sr);
    assertNotNull(client);
    client.publish("123");
  }
}
