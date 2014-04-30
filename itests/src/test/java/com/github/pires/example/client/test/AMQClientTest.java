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
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import static io.fabric8.tooling.testing.pax.exam.karaf.FabricKarafTestSupport.executeCommand;
import static java.lang.System.err;
import javax.jms.JMSException;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.UrlReference;

@RunWith(JUnit4TestRunner.class)
public class AMQClientTest extends FabricTestSupport {

  private ServiceProxy<FabricService> fabricService = null;
  private AMQClient client = null;

  @Configuration
  public Option[] config() {
    return new Option[] {
        new DefaultCompositeOption(fabricDistributionConfiguration()),
        scanFeatures(getFabricFeatureUrl(), "scr", "mq-fabric"),
        mavenBundle("com.github.pires.example", "client").versionAsInProject() };
  }

  public static UrlReference getFabricFeatureUrl() {
    String type = "xml/features";
    return mavenBundle("io.fabric8", "fabric8-karaf", "1.0.0.Beta4").type(type);
  }

  @Before
  public void before() throws InterruptedException {
    fabricService = ServiceProxy.createServiceProxy(bundleContext,
        FabricService.class);

    // bootstrap ensemble
    err.println(executeCommand("create -n --wait-for-provisioning"));

    // create master-slave broker topology
    err.println(executeCommand("mq-create --group a broker1"));
    err.println(executeCommand("container-create-child --jmx-user admin --jmx-password admin --profile mq-broker-a.broker1 root broker1c1"));
    err.println(executeCommand("container-create-child --jmx-user admin --jmx-password admin --profile mq-broker-a.broker1 root broker1c2"));
    // create app profile
    err.println(executeCommand("profile-create --parents mq-client-a --parents mq-client mq-example"));

    Container root = fabricService.getService().getContainer("root");
    final Profile profile = fabricService.getService().getProfile("1.0",
        "mq-example");
    root.addProfiles(profile);
    profile.refresh();

    // assign container to broker
    err.println(executeCommand("mq-create --assign-container root --group a broker1"));

    err.println(executeCommand("osgi:install -s mvn:com.github.pires.example/client/0.1-SNAPSHOT"));
    err.println(executeCommand("osgi:install -s mvn:com.github.pires.example/client-impl/0.1-SNAPSHOT"));

    err.println("Listing installed bundles state...");
    err.println(executeCommand("osgi:list | grep example"));

    // assert our service is available
    client = ServiceLocator.awaitService(this.bundleContext, AMQClient.class);
    assertNotNull(client);
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
  public void test_should_send_and_consume_message() throws JMSException,
      InterruptedException {
    assertEquals(client.getConsumedMessagesTotal(), 0);
    // publish a message
    client.publish("123");
    // wait a little bit for message to hit the queue
    err.println("Let's give it two seconds for the sent message to be delivered..");
    Thread.sleep(2000);
    assertEquals(client.getConsumedMessagesTotal(), 1);
  }

}
