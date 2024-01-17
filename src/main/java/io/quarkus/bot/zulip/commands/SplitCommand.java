package io.quarkus.bot.zulip.commands;

import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.bot.zulip.Zulip;
import io.quarkus.bot.zulip.payload.OutgoingWebhookPayload;
import io.quarkus.bot.zulip.payload.OutgoingWebhookResponse;

@ApplicationScoped
public class SplitCommand implements Command {

    @Inject Zulip zulip;

    @Override
    public OutgoingWebhookResponse process(OutgoingWebhookPayload payload) {

        String newSubject = payload.message.subject + " (split)";

        String newContent = payload.message.content + "\n\n"
                + "This topic has been split into a new thread. "
                + "Please continue the discussion in the new thread.";

        zulip.sendMessage(newSubject, payload.message.stream_id, newContent);

        return new OutgoingWebhookResponse("@**%s** **SPLIT**!", payload.message.sender_full_name);
    }

    @Override
    public boolean test(OutgoingWebhookPayload payload) {
        return payload.message.content.toLowerCase(Locale.ROOT).contains("split");
    }

    @Override
    public String getHelpText() {
        return "`split` - Split the topic into a new thread";
    }
}
