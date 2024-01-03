package sample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnNotWebApplication
@Slf4j
public class JmsApplicationRunner implements ApplicationRunner {

    // @Autowired
    private Requester requester;

    @Autowired
    private Simple simple;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (String arg : args.getSourceArgs()) {
            log.info("arg = {}", arg);

            switch (arg) {
            case "simple":
                simple.run();
                break;
            case "request-reply":
                requester.requestReply();
                break;
            default:
                log.info("Receive message by listener.");
                break;
            }

        }

    }
}
