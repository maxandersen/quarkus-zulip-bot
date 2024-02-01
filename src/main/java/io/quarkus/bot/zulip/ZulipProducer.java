package io.quarkus.bot.zulip;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.jamesnetherton.zulip.client.Zulip;
import com.github.jamesnetherton.zulip.client.exception.ZulipClientException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Interacts with the Zulip server using the Zulip REST API (https://zulip.com/api/rest)
 */
@ApplicationScoped
public class ZulipProducer {

    @Inject
    @ConfigProperty(name = "zulip.email")
    String email;

    @Inject
    @ConfigProperty(name = "zulip.key")
    String key;

    @Inject
    @ConfigProperty(name = "zulip.site")
    String site;

    @ApplicationScoped
    @Produces Zulip zulip() {
        try {
            return new Zulip(email, key, site);
        } catch (ZulipClientException e) {
            throw new RuntimeException(e);
        }
    }

}