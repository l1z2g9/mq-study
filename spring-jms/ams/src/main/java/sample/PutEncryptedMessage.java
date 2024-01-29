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
public class PutEncryptedMessage implements ApplicationRunner {
    static final String qName1 = "SEC.Q";

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

        jmsTemplate.send(qName1, session -> {
            String txt = String.format("%s <%s>", prefix, "Text Message");
            log.info("Send message : " + txt);
            return session.createTextMessage(txt);
        });

        System.exit(0);
    }
}
