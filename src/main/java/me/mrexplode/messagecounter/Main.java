package me.mrexplode.messagecounter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.google.gson.Gson;

import me.mrexplode.messagecounter.messenger.Conversation;

public class Main {
    
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Please specify the message JSON file");
            return;
        }
        File jsonFile = new File(args[0]);
        if (!jsonFile.exists()) {
            System.out.println("The specified file doesn't exist!");
            return;
        }
        
        BufferedReader reader = new BufferedReader(new FileReader(jsonFile));
        String line;
        StringBuilder builder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        
        Gson gson = new Gson();
        
        Conversation conv = gson.fromJson(builder.toString(), Conversation.class);
        System.out.println("Conversation title: " + conv.title);
        System.out.println("Messages: " + conv.messages.length);
        
        LocalDateTime first = LocalDateTime.ofInstant(Instant.ofEpochMilli(conv.messages[conv.messages.length -1].timestamp_ms), ZoneId.systemDefault());
        LocalDateTime last = LocalDateTime.ofInstant(Instant.ofEpochMilli(conv.messages[0].timestamp_ms), ZoneId.systemDefault());
        Duration duration = Duration.between(first, last);
        
        System.out.println("Time between first and last message: " + DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true));
    }

}
