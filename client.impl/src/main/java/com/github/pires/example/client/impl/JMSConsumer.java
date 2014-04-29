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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple JMS object message consumer.
 */
public class JMSConsumer implements MessageListener {

  private static final Logger log = LoggerFactory.getLogger(JMSConsumer.class);

  private final Session jmsSession;
  private final Destination destination;
  private MessageConsumer consumer;

  public JMSConsumer(Session jmsSession, Destination destination)
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
    log.info("Initializing JMS consumer..");
    try {
      consumer = jmsSession.createConsumer(destination);
      consumer.setMessageListener(this);
    } catch (JMSException e) {
      log.error("There was an error while initializating JMS consumer.", e);
      throw e;
    }
    log.info("JMS consumer successfuly initialized.");
  }

  public void onMessage(Message message) {
    try {
      log.info("New message received with ID {}.", message.getJMSMessageID());
    } catch (JMSException e) {
      log.error("There was an error while processing incoming message.", e);
    }
  }

}
