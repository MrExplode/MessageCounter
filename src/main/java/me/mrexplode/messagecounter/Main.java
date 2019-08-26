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
        System.out.println("---=== Welcome to MrExplolde's Messenger Information tool! ===---\nPlease specify the path your downloaded zip file.\n\nPlease note that if you wish to use this tool, all your downloaded data must be json formatted (as been selected at the Facebook settings)");
        reader = new BufferedReader(new InputStreamReader(System.in));
        File zipFile = new File(reader.readLine());
        
        if (!zipFile.exists()) {
            System.out.println("The specified file doesn't exist!");
            return;
        }
        
        fromDirectory(zipFile);
        
    }
    
    private static void fromDirectory(File zipFile) {
        File userdata = new File(workDir, zipFile.getName());
        userdata.mkdirs();
        long start = System.currentTimeMillis();
        try {
            System.out.println("Uncompressing data...");
            unzip(userdata, zipFile);
        } catch (IOException e) {
            System.err.println("Failed to uncompress user data archive.\nThings you can try: Place the archive in non-admin right required folder");
            e.printStackTrace();
            return;
        }
        System.out.println("Done! ("+ (System.currentTimeMillis() - start) + "ms)\n\n");
        
        //checking that the user exported the messaging data from Facebook
        File[] files = userdata.listFiles();
        File messageFolder = null;
        for (File entry : files)
            if (entry.getName().equals("messages"))
                messageFolder = entry;
        if (messageFolder == null) {
            System.out.println("You dumb fool! You didn't exported your messaging data! Go back and check that in.");
            deleteData(userdata);
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
        //kinda misleading var name, it covers any kind of conversation where the user was involved
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
            
            try {
                photoCount = new File(person, "photos").listFiles().length;
                gifCount = new File(person, "gifs").listFiles().length;
                videoCount = new File(person, "videos").listFiles().length;
                audioCount = new File(person, "audio").listFiles().length;
                fileCount = new File(person, "files").listFiles().length;
            } catch (Exception e) {}
            
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
        deleteData(userdata);
        
    }
    
    private static void deleteData(File userdata) {
        System.out.println("\n\n\n\nDeleting user sensitive data...");
        deleteFolder(userdata);
        System.out.println("Done!");
    }
    
    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
    
    //193693ms: compressed size, /4 buffer
    //196576ms: compressed size, /2 buffer, logs
    //198249ms: compressed size, /3 buffer, logs
    //199837ms: size, /4 buffer, logs
    //198740ms: compressed size, /4 buffer, w/o logs
    public static void unzip(File dir, File zip) throws IOException {
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zip));
        ZipEntry entry = zipIn.getNextEntry();
        
        while (entry != null) {
            File newFile = new File(dir, entry.getName());
            if (!entry.isDirectory()) {
                //System.out.println("[DEBUG] compSize: " + entry.getCompressedSize() + " size: " + entry.getSize() + " entry: " + entry.getName());
                byte[] buffer = new byte[(int) (entry.getCompressedSize() < 1024 ? 1024 : entry.getCompressedSize() / 4)];
                FileOutputStream out = new FileOutputStream(newFile);
                int len;
                while ((len = zipIn.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.close();
            } else {
                newFile.mkdir();
            }
            entry = zipIn.getNextEntry();
        }
        
        zipIn.closeEntry();
        zipIn.close();
    }
    
    private static Conversation unknown(String filename) {
        Conversation conv = new Conversation();
        conv.title = filename;
        conv.messages = new Message[] {};
        conv.is_still_participant = false;
        conv.thread_type = "unknown";
        return conv;
    }
    
    @SuppressWarnings("unused")
    private static String repair(String string) {
        string = string.replaceAll("Ã¡", "á")
                .replaceAll("Ã³", "ó")
                .replaceAll("Ã", "Á")
                .replaceAll("Ã©", "é")
                .replaceAll("", "");
        return string;
    }
}
