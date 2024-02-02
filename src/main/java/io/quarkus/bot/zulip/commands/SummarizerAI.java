package io.quarkus.bot.zulip.commands;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface SummarizerAI {

    /**
     * Ask the LLM to summarize the given content.
     *
     * @param topic the topic of the poem
     * @param lines the number of line of the poem
     * @return the poem
     */
    @SystemMessage("You are a bot in a zulip chat room. Tasked to summarize the content of a topic as an intro into a new topic.")
    @UserMessage("""
        Write a summary for the following text: 
        ```
        {content}
        ```

        The summary should be maximum {maxLines} lines long and be aware the new suggested topic is '{newTopic}'.
    """)
    String summarize(String content, int maxLines, String newTopic);

}