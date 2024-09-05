package sample;

import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JmsErrorHandler implements ErrorHandler {

    // ... logger

    @Override
    public void handleError(Throwable t) {
        log.warn("In default jms error handler...");
        log.error("Error Message : {}", t.getMessage());
    }

}