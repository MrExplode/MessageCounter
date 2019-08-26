package me.mrexplode.messagecounter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.google.gson.Gson;

import me.mrexplode.messagecounter.messenger.Conversation;
import me.mrexplode.messagecounter.messenger.Message;

public class Main {
    
    public static final String workDir = System.getProperty("user.home") + "\\AppData\\Roaming\\MessageCounter";
    
    private static BufferedReader reader;
    
    public static void main(String[] args) throws IOException {
        System.out.println("---=== Welcome to MrExplolde's Messenger Information tool! ===---\nPlease specify the path your downloaded zip file, or just the single .json file containing the conversation data\nPlease note that if you wish to use this tool, all your downloaded data must be json formatted (as been selected at the Facebook settings)");
        reader = new BufferedReader(new InputStreamReader(System.in));
        File file = new File(reader.readLine());
        
        if (!file.exists()) {
            System.out.println("The specified file doesn't exist!");
            return;
        }
        
        if (file.isDirectory()) {
            fromDirectory(file);
        } else {
            fromFile(file);
        }
        
    }
    
    private static void fromDirectory(File file) {
        File userdata = new File(workDir, file.getName());
        userdata.mkdirs();
        try {
            unzip(userdata, file);
        } catch (IOException e) {
            System.err.println("Failed to uncompress user data archive.\nThings you can try: Place the archive in non-admin right required folder");
            e.printStackTrace();
            return;
        }
        
        //checking that the user exported the messaging data from Facebook
        File[] files = file.listFiles();
        File messageFolder = null;
        for (File entry : files)
            if (entry.getName().equals("messages"))
                messageFolder = entry;
        if (messageFolder == null) {
            System.out.println("You dumb fool! You didn't exported your messaging data! Go back and check that in.");
            return;
        }
        
        //actual data analysis
        System.out.println("Active conversations: " + new File(messageFolder, "inbox").listFiles().length);
        //archived threads
        System.out.println("Archived conversations: " + new File(messageFolder, "archived_threads").listFiles().length);
        //filtered threads
        System.out.println("Filtered conversations: " + new File(messageFolder, "filtered_threads").listFiles().length);
        //used stickers
        System.out.println("You used " + new File(messageFolder, "stickers_used").listFiles().length + " stickers.");
        System.out.println("\n\nActive conversation details:\n\n");
        File[] inboxList = new File(messageFolder, "inbox").listFiles();
        for (File person : inboxList) {
            System.out.println("==============================");
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(new File(person, "message_1.json")));
                String line;
                while((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
            } catch (IOException e) {
                System.err.println("Could not find the message JSON file. you will see less information. (except some errors)");
            }
            Conversation conv = null;
            try {
                Gson gson = new Gson();
                conv = gson.fromJson(sb.toString(), Conversation.class);
            } catch (Exception e) {
                conv = unknown(person.getName());
            }
            
            int photoCount = 0;
            int gifCount = 0;
            int videoCount = 0;
            int audioCount = 0;
            int fileCount = 0;
            
            System.out.println("Conversation title: " + conv.title);
            if (conv.participants.length > 2)
                System.out.println("Participants in group: " + conv.participants.length);
            System.out.println("Messages sent: " + conv.messages.length);
            if (photoCount > 0)
                System.out.println("Photos sent: " + photoCount);
            if (gifCount > 0)
                System.out.println("GIFs sent: " + gifCount);
            if (videoCount > 0)
                System.out.println("Videos sent: " + videoCount);
            if (audioCount > 0)
                System.out.println("Audio messages sent: " + audioCount);
            if (fileCount > 0)
                System.out.println("Files sent: " + fileCount);
            
            LocalDateTime first = LocalDateTime.ofInstant(Instant.ofEpochMilli(conv.messages[conv.messages.length -1].timestamp_ms), ZoneId.systemDefault());
            LocalDateTime last = LocalDateTime.ofInstant(Instant.ofEpochMilli(conv.messages[0].timestamp_ms), ZoneId.systemDefault());
            Duration duration = Duration.between(first, last);
            
            System.out.println("Time between first and last message: " + DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true));
        }
        
        //cleanup user sensitive data
        for (File entry : userdata.listFiles()) {
            entry.delete();
        }
        for (File entry : new File(workDir).listFiles()) {
            entry.delete();
        }
        
        
    }
    
    private static void fromFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder builder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        
        Gson gson = new Gson();
        
        Conversation conv = gson.fromJson(builder.toString(), Conversation.class);
        System.out.println("Conversation title: " + conv.title);
        System.out.println("Messages: " + conv.messages.length);
        
        LocalDateTime first = LocalDateTime.ofInstant(Instant.ofEpochMilli(conv.messages[conv.messages.length -1].timestamp_ms), ZoneId.systemDefault());
        LocalDateTime last = LocalDateTime.ofInstant(Instant.ofEpochMilli(conv.messages[0].timestamp_ms), ZoneId.systemDefault());
        Duration duration = Duration.between(first, last);
        
        System.out.println("Time between first and last message: " + DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true));
    }
    
    public static void unzip(File dir, File zip) throws IOException {
        byte[] buffer = new byte[1024 * 2];
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zip));
        ZipEntry entry = zipIn.getNextEntry();
        while (entry != null) {
            File newFile = new File(dir, entry.getName());
            FileOutputStream out = new FileOutputStream(newFile);
            int len;
            while ((len = zipIn.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            
            out.close();
            entry = zipIn.getNextEntry();
        }
        
        zipIn.closeEntry();
        zipIn.close();
    }
    
    public static Conversation unknown(String filename) {
        Conversation conv = new Conversation();
        conv.title = filename;
        conv.messages = new Message[] {};
        conv.is_still_participant = false;
        conv.thread_type = "unknown";
        return conv;
    }
}
