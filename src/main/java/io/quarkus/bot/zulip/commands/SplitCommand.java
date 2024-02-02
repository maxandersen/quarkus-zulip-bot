package io.quarkus.bot.zulip.commands;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.github.jamesnetherton.zulip.client.Zulip;
import com.github.jamesnetherton.zulip.client.api.message.Anchor;
import com.github.jamesnetherton.zulip.client.api.narrow.Narrow;
import com.github.jamesnetherton.zulip.client.exception.ZulipClientException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.bot.zulip.ZulipProducer;
import io.quarkus.bot.zulip.payload.OutgoingWebhookPayload;
import io.quarkus.bot.zulip.payload.OutgoingWebhookResponse;

@ApplicationScoped
public class SplitCommand implements Command {

    @Inject Zulip zulip;

    @Inject SummarizerAI summarizer;

    Map<Integer, String> streams = new HashMap<>();

    @Override
    public OutgoingWebhookResponse process(OutgoingWebhookPayload payload) {
 

        String stream = streams.computeIfAbsent(payload.message.stream_id, id -> {
            try {
                return zulip.streams().getStream(id).execute().getName();
            } catch (ZulipClientException e) {
                throw new RuntimeException(e);
            }
        });

        String splitMarker = splitMarker(payload);
        String contentAfterSplitMarker = payload.message.content.substring(payload.message.content.indexOf(splitMarker) + splitMarker.length()).trim();

        String newSubject = contentAfterSplitMarker.isEmpty() ? payload.message.subject + " (split)" : contentAfterSplitMarker;

        final StringBuilder messageSummary = new StringBuilder();
        String newMessageLink;
        try {
            var messages = zulip.messages().getMessages(1000,0, Anchor.NEWEST)
            .withNarrows(Narrow.of("topic", payload.message.subject), Narrow.of("stream", stream))
            .withMarkdown(false)
            .execute();

            messages.forEach(message -> messageSummary.append("\n\n").append(message.getSenderFullName()).append(" said:\n").append(message.getContent()).append(" ").append(message.getSubject()));

            String[] words = messageSummary.toString().split("\\s+");
            if (words.length > 3950) {
                String[] limitedWords = Arrays.copyOfRange(words, words.length - 3950, words.length);
                messageSummary.delete(0,messageSummary.length());
                messageSummary.append(new StringBuilder(String.join(" ", limitedWords).toString()));
            }

            String summary = summarizer.summarize(messageSummary.toString(), 8, newSubject);

            zulip.messages().sendStreamMessage(summary, payload.message.stream_id, newSubject).execute();

            newMessageLink = "#**%s>%s**".formatted(stream, newSubject);

        } catch (ZulipClientException e) {
            throw new RuntimeException(e);
        }

        String newContent = "This topic has been split into new topic: %s.".formatted(newMessageLink);

        return new OutgoingWebhookResponse("@**%s** %s", payload.message.sender_full_name, newContent);
    }

    @Override
    public boolean test(OutgoingWebhookPayload payload) {        
        return payload.message.content.contains(splitMarker(payload));
    }

    private String splitMarker(OutgoingWebhookPayload payload) {
        return "@**" + payload.bot_full_name + "** split";
    }

    @Override
    public String getHelpText() {
        return "`split` - Split the topic into a new thread";
    }
}
