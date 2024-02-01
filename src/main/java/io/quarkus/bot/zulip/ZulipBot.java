package io.quarkus.bot.zulip;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import com.github.jamesnetherton.zulip.client.Zulip;
import com.github.jamesnetherton.zulip.client.api.stream.StreamSubscriptionRequest;
import com.github.jamesnetherton.zulip.client.exception.ZulipClientException;

import io.quarkus.bot.zulip.commands.Command;
import io.quarkus.bot.zulip.payload.OutgoingWebhookPayload;
import io.quarkus.bot.zulip.payload.OutgoingWebhookResponse;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/")
public class ZulipBot {

    @Inject
    Instance<Command> commands;

    @Inject
    Zulip zulip;

    @POST
    public OutgoingWebhookResponse onWebhook(OutgoingWebhookPayload payload) {
        try {
        if (payload.message.content.toLowerCase(Locale.ROOT).contains("help")) {
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer, true);
            for (Command command : commands) {
                pw.println(command.getHelpText());
            }
                zulip.messages().addEmojiReaction(payload.message.id, "+1").execute();
            
            return new OutgoingWebhookResponse(writer.toString());
        } else {
            for (Command command : commands) {
                if (command.test(payload)) {
                    zulip.messages().addEmojiReaction(payload.message.id, "+1").execute();;
                    return command.process(payload);
                }
            }
            zulip.messages().addEmojiReaction(payload.message.id, "oh_no").execute();;
            String contents = String.format("@**%s** Sorry, I couldn't understand your command. Type `@%s help` if you need assistance",
                                            payload.message.sender_full_name, payload.bot_full_name);
            return new OutgoingWebhookResponse(contents);
        }
    } catch (ZulipClientException e) {
        throw new RuntimeException(e);
    }
    }

        void onStart(@Observes StartupEvent ev) {
            System.out.println("ZulipBot is starting...");
            try {
                zulip.streams().getSubscribedStreams().execute().forEach(sub -> {
                    System.out.println("Subscribed to " + sub.getName());
                });
            } catch (ZulipClientException e) {
                
                e.printStackTrace();
            }
        }
}