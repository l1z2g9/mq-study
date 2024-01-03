package sample;

import java.util.Date;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Simple {
    // static final String qName1 = "DEV.QUEUE.1"; // A queue from the default MQ Developer container config
    static final String qName1 = "Q1";
    static final String qName2 = "DEV.QUEUE.2"; // Another queue from the default MQ Developer container config

    @Autowired
    private JmsTemplate jmsTemplate;

    /* @Bean
    public MQConnectionFactoryCustomizer myCustomizer() {
        MQConnectionFactoryCustomizer c = new MQConnectionFactoryCustomizer() {
            @Override
            public void customize(MQConnectionFactory factory) {
                log.info(">> In a customizer method that can modify class " + factory.getClass().getName());
            }
        };
        return c;
    } */

    public void run() throws JMSException {
        JmsTransactionManager tm = new JmsTransactionManager();

        log.info("MQ JMS Transaction Sample with JMS3 and TLS started.");

        // Create the JMS Template object to control connections and sessions.

        // Associate the connection factory with the transaction manager
        tm.setConnectionFactory(jmsTemplate.getConnectionFactory());

        // This starts a new transaction scope. "null" can be used to get a default
        // transaction model
        TransactionStatus status = tm.getTransaction(null);

        // Create a single message with a timestamp
        String outMsg = "Hello from IBM MQ at " + new Date();

        // The default SimpleMessageConverter class will be called and turn a String
        // into a JMS TextMessage which we send to qName1. This operation will be made
        // part of the transaction that we initiated.
        jmsTemplate.convertAndSend(qName1, outMsg);

        // Commit the transaction so the message is now visible
        tm.commit(status);

        // But now we're going to start a new transaction to hold multiple operations.
        status = tm.getTransaction(null);
        // Read it from the queue where we just put it, and then send it straight on to
        // a different queue
        Message inMsg = jmsTemplate.receive(qName1);

        if (inMsg instanceof TextMessage txt) {
            log.info("inMsg = " + txt.getText());
        }

        jmsTemplate.convertAndSend(qName2, inMsg);
        // This time we decide to rollback the transaction so the receive() and send()
        // are
        // reverted. We end up with the message still on qName1.
        tm.rollback(status);

        log.info("Done.");

        System.exit(0);
    }

}
