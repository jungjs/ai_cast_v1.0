package com.aicast.tool.parser;

import com.aicast.tool.model.Scenario;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownScenarioParser {

    public List<Scenario> parse(Path markdownFile) throws IOException {
        String content = Files.readString(markdownFile);
        return parse(content);
    }

    public List<Scenario> parse(String markdownContent) {
        List<Scenario> scenarios = new ArrayList<>();
        int index = 0;

        Pattern titlePattern = Pattern.compile("(?:\\n|^)\\[제목\\]\\s*(.+?)(?=\\n\\[본문\\])", Pattern.DOTALL);
        Pattern bodyPattern = Pattern.compile("(?:\\n|^)\\[본문\\]\\s*(.+?)(?=\\n\\[TTS)", Pattern.DOTALL);
        Pattern ttsPattern = Pattern.compile("(?:\\n|^)\\[TTS 체크 포인트\\]\\s*(.+?)(?=\\n\\n|$)", Pattern.DOTALL);

        Matcher titleMatcher = titlePattern.matcher(markdownContent);
        Matcher bodyMatcher = bodyPattern.matcher(markdownContent);
        Matcher ttsMatcher = ttsPattern.matcher(markdownContent);

        while (titleMatcher.find() && bodyMatcher.find()) {
            index++;
            String title = titleMatcher.group(1).trim();
            String body = bodyMatcher.group(1).trim();
            String ttsCheckpoint = ttsMatcher.find() ? ttsMatcher.group(1).trim() : "";
            scenarios.add(new Scenario(index, title, body, ttsCheckpoint));
        }

        return scenarios;
    }
}
