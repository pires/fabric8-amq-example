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
import java.io.Serializable;
import java.util.Map;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link AMQClient} service.
 */
@Component(name = "com.github.pires.example.client",
    label = "Test ActiveMQ Client",
    immediate = true)
@Service(AMQClient.class)
public class AMQClientImpl implements AMQClient {

  private static final Logger log = LoggerFactory.getLogger(AMQClient.class);

  @Reference(referenceInterface = ActiveMQConnectionFactory.class)
  private ActiveMQConnectionFactory connectionFactory;
  private Connection jmsConnection;
  private Session jmsSession;
  private JMSProducer producer;
  private JMSConsumer consumer;

  public void publish(Serializable message) {
    try {
      producer.send(message);
    } catch (JMSException e) {
      log.error("There was an error while sending a message.", e);
    }
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

  private void deactivateInternal() {
    try {
      jmsSession.close();
      jmsConnection.close();
      producer = null;
      consumer = null;
    } catch (JMSException e) {
      log.error("There was an exception while clearing JMS resources.", e);
    }
  }

  private void updateInternal(Map<String, ?> configuration) throws JMSException {
    // get JMS up and running
    jmsConnection = connectionFactory.createQueueConnection();
    jmsSession = jmsConnection.createSession(true, Session.AUTO_ACKNOWLEDGE);
    final String queueName = "test";
    Destination destination = null;
    if (queueName != null && !queueName.isEmpty()) {
      destination = jmsSession.createQueue(queueName);
    } else {
      throw new JMSException("Can't find queueName in AMQClient configuration.");
    }
    producer = new JMSProducer(jmsSession, destination);
    consumer = new JMSConsumer(jmsSession, destination);
  }

}
