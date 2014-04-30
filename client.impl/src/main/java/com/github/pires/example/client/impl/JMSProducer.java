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

import java.io.Serializable;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple JMS object message producer.
 */
public class JMSProducer {

  private static final Logger log = LoggerFactory.getLogger(JMSProducer.class);

  private final Session jmsSession;
  private final Destination destination;
  private MessageProducer producer;

  public JMSProducer(Session jmsSession, Destination destination)
      throws JMSException {
    this.jmsSession = jmsSession;
    this.destination = destination;
    init();
  }

  /**
   * Provision the producer itself.
   * 
   * @throws JMSException
   */
  private void init() throws JMSException {
    log.info("Initializing JMS producer..");
    try {
      producer = jmsSession.createProducer(destination);
      producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    } catch (JMSException e) {
      log.error("There was an error while initializating JMS producer.", e);
      throw e;
    }
    log.info("JMS producer successfuly initialized.");
  }

  public void stop() throws JMSException {
    producer.close();
  }

  /**
   * Sends an object through JMS.
   * 
   * @param obj
   * @throws JMSException
   */
  public void send(final Serializable obj) throws JMSException {
    ObjectMessage message = jmsSession.createObjectMessage(obj);
    producer.send(message);
    log.info("Message sent successfully to queue.");
  }

}
