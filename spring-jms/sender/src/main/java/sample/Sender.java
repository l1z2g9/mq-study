package sample;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Sender implements ApplicationRunner {
    static final String qName1 = "Q1";

    @Autowired
    private JmsTemplate jmsTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        AtomicReference<String> prefix = new AtomicReference<>("");
        String[] arg = args.getSourceArgs();
        if (arg.length > 0) {
            prefix.set(arg[0]);
        }

        log.info("message sent with prefix {}", prefix);

        IntStream.range(0, 10).forEach(i -> {
            try {
                /* log.info("isExplicitQosEnabled() " + jmsTemplate.isExplicitQosEnabled());
                log.info("Priority " + jmsTemplate.getPriority());
                log.info("DeliveryMode " + jmsTemplate.getDeliveryMode());
                log.info("SessionAcknowledgeMode " + jmsTemplate.getSessionAcknowledgeMode());
                log.info("isSessionTransacted " + jmsTemplate.isSessionTransacted());
                
                jmsTemplate.setExplicitQosEnabled(true);
                jmsTemplate.setPriority(8);
                jmsTemplate.setDeliveryMode(DeliveryMode.PERSISTENT);
                
                jmsTemplate.setSessionTransacted(true);
                
                jmsTemplate.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE); */

                jmsTemplate.send(qName1, session -> {
                    String txt = prefix.get() + " Text Message " + i;
                    log.info("Send message : " + txt);
                    return session.createTextMessage(txt);
                });

                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        System.exit(0);
    }
}
