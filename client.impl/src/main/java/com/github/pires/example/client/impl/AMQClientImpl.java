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
package com.github.pires.example.client.impl;

import com.github.pires.example.client.AMQClient;
import io.fabric8.mq.ActiveMQService;
import io.fabric8.mq.ConsumerThread;
import io.fabric8.mq.ProducerThread;
import java.util.Map;
import javax.jms.JMSException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link AMQClient} service.
 */
@Component(name = "com.github.pires.example.client",
    label = "ActiveMQ Client Factory",
    configurationFactory = true,
    immediate = true,
    metatype = true)
public class AMQClientImpl implements AMQClient {

  private static final Logger log = LoggerFactory.getLogger(AMQClient.class);

  ConsumerThread consumer;
  ProducerThread producer;
  ActiveMQService jmsService;
  @Reference(referenceInterface = ActiveMQConnectionFactory.class)
  private ActiveMQConnectionFactory connectionFactory;

  @Override
  public void publish(String message) {
    // TODO
  }

  @Override
  public void consume() {
    // TODO
  }

  @Activate
  void activate(Map<String, ?> configuration) throws Exception {
    updateInternal(configuration);
  }

  @Modified
  void modified(Map<String, ?> configuration) throws Exception {
    deactivateInternal();
    updateInternal(configuration);
  }

  @Deactivate
  void deactivate() {
    deactivateInternal();
  }

  protected void deactivateInternal() {
    if (consumer != null) {
      consumer.setRunning(false);
      jmsService.stop();
    }
  }

  private void updateInternal(Map<String, ?> configuration) throws Exception {
    try {
      // get JMS up and running
      jmsService = new ActiveMQService(connectionFactory);
      jmsService.setMaxAttempts(10);
      jmsService.start();
      String destination = (String) configuration.get("destination");

      // get a producer thread
      producer = new ProducerThread(jmsService, destination);
      producer.setSleep(500);
      producer.start();
      log.info("Producer started");

      // get a consumer thread
      consumer = new ConsumerThread(jmsService, destination);
      consumer.start();
      log.info("Consumer started");
    } catch (JMSException e) {
      throw new Exception("Cannot start JMS..", e);
    }
  }

}
