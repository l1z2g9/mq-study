package sample;

import java.util.Optional;

import javax.net.ssl.SSLSocketFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.ibm.mq.spring.boot.MQConfigurationProperties;
import com.ibm.mq.spring.boot.MQConnectionFactoryCustomizer;

@SpringBootApplication
@EnableJms
@EnableTransactionManagement
@Configuration
public class MainApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MainApplication.class);
    }

    @Bean
    public MQConnectionFactoryCustomizer sslCustomizer(
            Optional<SslBundles> bundles, Optional<MQConfigurationProperties> properties) {
        System.out.println("Creating MQConnectionFactoryCustomizer for SslBundles");

        if (bundles.isEmpty() || properties.isEmpty()) {
            return mqConnectionFactory -> {
            };
        }

        return connectionFactory -> {
            if (properties.get().getSslBundle() != null) {
                System.out.println("Trying to configure MqConnectionFactory to use bundle " + properties.get().getSslBundle());
                SslBundle bundle = bundles.get().getBundle(properties.get().getSslBundle());
                if (bundle != null) {
                    SSLSocketFactory socketFactory = bundle.createSslContext().getSocketFactory();
                    System.out.println("Found bundle, created SocketFactory " + socketFactory);
                    connectionFactory.setSSLSocketFactory(socketFactory);
                }
            }
        };
    }
}
