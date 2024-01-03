package sample;

import java.util.Optional;

import javax.net.ssl.SSLSocketFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.ibm.mq.spring.boot.MQConfigurationProperties;
import com.ibm.mq.spring.boot.MQConnectionFactoryCustomizer;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableJms
@EnableTransactionManagement
@Configuration
@Slf4j
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @Bean
    public MQConnectionFactoryCustomizer sslCustomizer(
            Optional<SslBundles> bundles, Optional<MQConfigurationProperties> properties) {
        log.debug("Creating MQConnectionFactoryCustomizer for SslBundles");

        if (bundles.isEmpty() || properties.isEmpty()) {
            return mqConnectionFactory -> {
            };
        }

        return connectionFactory -> {
            if (properties.get().getSslBundle() != null) {
                log.debug("Trying to configure MqConnectionFactory to use bundle {}", properties.get().getSslBundle());
                SslBundle bundle = bundles.get().getBundle(properties.get().getSslBundle());
                if (bundle != null) {
                    SSLSocketFactory socketFactory = bundle.createSslContext().getSocketFactory();
                    log.debug("Found bundle, created SocketFactory {}", socketFactory);
                    connectionFactory.setSSLSocketFactory(socketFactory);
                }
            }
        };
    }
}
