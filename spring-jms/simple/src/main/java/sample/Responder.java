package sample;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

// @Component
@Slf4j
public class Responder implements SessionAwareMessageListener<Message> {

    @JmsListener(destination = Requester.qName)
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(Message msg, Session session) throws JMSException {
        String text;

        if (msg instanceof TextMessage) {
            text = ((TextMessage) msg).getText();
        } else {
            text = msg.toString();
        }

        log.info("========================================");
        log.info("Responder received message: " + text);
        log.info("Redelivery flag: " + msg.getJMSRedelivered());
        log.info("========================================");

        final String msgID = msg.getJMSMessageID();

        MessageProducer replyDest = session.createProducer(msg.getJMSReplyTo());

        log.info("getDestination " + replyDest.getDestination());

        TextMessage replyMsg = session.createTextMessage("Replying to " + text);
        replyMsg.setJMSCorrelationID(msgID);
        replyDest.send(replyMsg);

        // We deliberately fail the first attempt at sending a reply. The message is
        // put back on its original queue and then redelivered. At that point, we
        // try to commit the reply.
        if (!msg.getJMSRedelivered()) {
            log.info("Doing a rollback");
            session.rollback();
            /*
             * throw new JMSException("Instead of rollback"); - might prefer this to see
             * what happens
             */
        } else {
            log.info("Doing a commit");
            session.commit();
        }

    }

}
