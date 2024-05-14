package bean;

import java.io.IOException;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class IvtBean {

    // String cfJNDIName = "java:jboss/jms/ivt/IvtCF";

    @Resource(lookup = "java:jboss/jms/ivt/IvtCF")
    private ConnectionFactory cf;

    @Resource(name = "java:jboss/jms/ivt/IvtQueue")
    private Destination ivtQueue;

    @PersistenceContext(unitName = "spu")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void sendMsg() throws IOException {
        System.out.println("getSystemResource(/) " + ClassLoader.getSystemResource("/"));

        try {
            /*
             * InitialContext ctx = new InitialContext();
             * ConnectionFactory cf = (ConnectionFactory) ctx.lookup(cfJNDIName);
             */

            try (Connection connection = cf.createConnection();
                    Session session = connection.createSession(true, Session.SESSION_TRANSACTED);) {
                connection.start();

                // Queue queue = session.createQueue("SEC.Q");
                try (MessageProducer producer = session.createProducer(ivtQueue);) {
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);

                    TextMessage textMessage = session.createTextMessage();
                    textMessage.setText("hello world");

                    Queue reployQueue = session.createQueue("DEV.QUEUE.1");
                    textMessage.setJMSReplyTo(reployQueue);
                    producer.send(textMessage);

                    Query query = em.createNativeQuery("select cn from cud_user where id = 7467");
                    String cn = query.getSingleResult().toString();
                    System.out.println("cn = " + cn);

                    int rows = em.createNativeQuery("update cud_user set dp_rank = 'abcd' where id = 7467")
                            .executeUpdate();
                    System.out.println("updated rows = " + rows);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
