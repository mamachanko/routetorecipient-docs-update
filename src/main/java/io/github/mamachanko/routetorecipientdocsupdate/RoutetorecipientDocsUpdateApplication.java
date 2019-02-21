package io.github.mamachanko.routetorecipientdocsupdate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.messaging.Message;

@SpringBootApplication
public class RoutetorecipientDocsUpdateApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoutetorecipientDocsUpdateApplication.class, args);
    }

}

@Configuration
class IntegrationConfig {

    @Bean
    public IntegrationFlow routeFlowByLambda() {
        return IntegrationFlows.from("routerInput")
                .<Integer, Boolean>route(p -> p % 2 == 0,
                        m -> m.suffix("Channel")
                                .channelMapping(true, "even")
                                .channelMapping(false, "odd")
                )
                .get();
    }

    @Bean
    public IntegrationFlow routeFlowByExpression() {
        return IntegrationFlows.from("routerInput")
                .route("headers['destChannel']")
                .get();
    }

    @Bean
    public IntegrationFlow recipientListFlow() {
        return IntegrationFlows.from("recipientListInput")
                .<String, String>transform(p -> p.replaceFirst("Payload", ""))
                .routeToRecipients(r -> r
                        .recipient("thing1-channel", "'thing1' == payload")
                        .recipientMessageSelector("thing2-channel", m ->
                                m.getHeaders().containsKey("recipient")
                                        && (boolean) m.getHeaders().get("recipient"))
                        .recipientFlow("'thing1' == payload or 'thing2' == payload or 'thing3' == payload",
                                f -> f.<String, String>transform(String::toUpperCase)
                                        .channel(c -> c.queue("recipientListSubFlow1Result")))
                        .recipientFlow((String p) -> p.startsWith("thing3"),
                                f -> f.transform("Hello "::concat)
                                        .channel(c -> c.queue("recipientListSubFlow2Result")))
                        .recipientFlow(new FunctionExpression<Message<?>>(m ->
                                        "thing3".equals(m.getPayload())),
                                f -> f.channel(c -> c.queue("recipientListSubFlow3Result")))
                        .defaultOutputToParentFlow())
                .get();
    }
}
