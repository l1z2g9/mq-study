package sample;

import jakarta.annotation.Resource;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
// @Slf4j
public class Sender {
    static final String qName1 = "DEV.QUEUE.1";

    // @Autowired
    private JmsTemplate jmsTemplate;

    @Resource(lookup = "java:jboss/jms/ivt/IVTCF")
    private ConnectionFactory cf;

    @Resource(lookup = "java:jboss/jms/ivt/IVTQueue")
    private Queue queue;

    @GetMapping("/send-msg")
    public void sendMsg() throws Exception {
        // jmsTemplate.setConnectionFactory(cf);
        Connection conn = cf.createConnection("app", "passw0rd");
        Session s = conn.createSession();
        try (JMSContext context = cf.createContext()) {
            TextMessage m = context.createTextMessage("test message");
            context.createProducer().send(queue, m);
        }

        s.close();
        conn.close();

        /* jmsTemplate.send(qName1, session -> {
            String txt = String.format("<%s>", "Text Message");
            System.out.println("Send message : " + txt);
            return session.createTextMessage(txt);
        }); */

    }
}
