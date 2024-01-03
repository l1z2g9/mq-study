package sample;

import java.util.Date;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

// @Component
@Slf4j
public class Requester {
    // Create a transaction manager object that will be used to control
    // commit/rollback of operations in the listener.

    static final String qName = "DEV.QUEUE.3"; // A queue from the default MQ Developer container config

    String correlID = null;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Bean
    public JmsTransactionManager transactionManager() {
        return new JmsTransactionManager(jmsTemplate.getConnectionFactory());
    }

    public void requestReply() throws JMSException {
        // Create the JMS Template object to control connections and sessions.

        log.info("MQ JMS Request/Reply Sample started.");

        jmsTemplate.setReceiveTimeout(5 * 1000L); // How long to wait for a reply - milliseconds

        // Create a single message with a timestamp
        String payload = "Hello from IBM MQ at " + new Date();

        // TransactionStatus status = tm.getTransaction(null);

        // Send the message and wait for a reply for up to the specified timeout
        Message replyMsg = jmsTemplate.sendAndReceive(qName, new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                TextMessage message = session.createTextMessage(payload);
                log.info("Sending message: " + message.getText());
                return message;
            }
        });

        if (replyMsg != null) {
            if (replyMsg instanceof TextMessage) {
                log.info("Reply message is: " + ((TextMessage) replyMsg).getText());
            } else {
                log.info("Reply message is: " + replyMsg.toString());
            }
        } else {
            log.info("No reply received");
        }

        // tm.commit(status);

        log.info("Done.");
        System.exit(0);
    }

}
