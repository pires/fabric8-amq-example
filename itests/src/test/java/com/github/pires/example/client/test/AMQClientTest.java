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
import io.fabric8.api.Profile;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import static io.fabric8.tooling.testing.pax.exam.karaf.FabricKarafTestSupport.executeCommand;
import static java.lang.System.err;
import javax.jms.JMSException;
import org.junit.After;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.UrlReference;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class AMQClientTest extends FabricTestSupport {

  ServiceProxy<FabricService> fabricService;

  // @ProbeBuilder
  // public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
  // probe.setHeader("Service-Component",
  // "OSGI-INF/com.github.pires.example.client.impl.AMQClientImpl.xml");
  // return probe;
  // }
  @Configuration
  public Option[] config() {
    return new Option[] {
        new DefaultCompositeOption(fabricDistributionConfiguration()),
        scanFeatures(getFabricFeatureUrl(), "scr", "mq-fabric") };
  }

  public static UrlReference getFabricFeatureUrl() {
    String type = "xml/features";
    return mavenBundle("io.fabric8", "fabric8-karaf", "1.0.0.Beta4").type(type);
  }

  @Before
  public void setUp() throws Exception {
    fabricService = ServiceProxy.createServiceProxy(bundleContext,
        FabricService.class);

    // bootstrap ensemble
    err.println(executeCommand("create -n --wait-for-provisioning"));

    // create master-slave broker topology
    err.println(executeCommand("mq-create --group a broker1"));
    err.println(executeCommand("container-create-child --jmx-user admin --jmx-password admin --profile mq-broker-a.broker1 root broker1c1"));
    err.println(executeCommand("container-create-child --jmx-user admin --jmx-password admin --profile mq-broker-a.broker1 root broker1c2"));
    // create app profile
    err.println(executeCommand("profile-create --parents mq-amq --parents mq-client-a --parents mq-client mq-example"));

    Container root = fabricService.getService().getContainer("root");
    final Profile profile = fabricService.getService().getProfile("1.0",
        "mq-example");
    root.addProfiles(profile);
    profile.refresh();

    // assign container to broker
    err.println(executeCommand("mq-create --assign-container root --group a broker1"));

    err.println(executeCommand("osgi:install -s mvn:com.github.pires.example/client/0.1-SNAPSHOT"));
    err.println(executeCommand("osgi:install -s mvn:com.github.pires.example/client-impl/0.1-SNAPSHOT"));

    err.println("Sleeping for 5 seconds...");
    Thread.sleep(5000);

    err.println(executeCommand("osgi:list | grep example"));
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
  public void should_send_message() throws JMSException {
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
            if (sr.getUsingBundles() == null) {
              err.println("No bundles linked to this service reference.");
            } else {
              for (Bundle srBundle : sr.getUsingBundles()) {
                err.println(srBundle.getSymbolicName());
              }
            }
          }
        }
      }
    }

    // AMQClient proxy = ServiceLocator.awaitService(this.bundleContext,
    // AMQClient.class);
    // assertNotNull(proxy);
    ServiceReference<AMQClient> sr = bundleContext
        .getServiceReference(AMQClient.class);
    assertNotNull(sr);
    AMQClient client = bundleContext.getService(sr);
    assertNotNull(client);
    client.publish("123");
  }
}
