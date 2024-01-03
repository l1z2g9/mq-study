package sample;

import java.util.concurrent.TimeUnit;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MyListener implements SessionAwareMessageListener<Message> {
    @Override
    @JmsListener(destination = "Q1")
    // @Transactional(rollbackFor = Exception.class)
    public void onMessage(Message message, Session session) throws JMSException {
        String text;

        if (message instanceof TextMessage) {
            text = ((TextMessage) message).getText();
        } else {
            text = message.toString();
        }
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // log.info("session = {}", session);
        log.info("message received = " + text);
    }

}
