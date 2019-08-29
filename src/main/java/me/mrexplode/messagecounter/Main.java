package me.mrexplode.messagecounter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import com.google.gson.Gson;

import me.mrexplode.messagecounter.messenger.Conversation;
import me.mrexplode.messagecounter.messenger.Message;

public class Main {
    
    public static final String normal_range = " qwertzuiopőúasdfghjkléáűóüöíyxcvbnmQWERTZUIOPŐÚASDFGHJKLÉÁŰÓÜÖÍYXCVBNM0123456789,.-<>#&@{};>*?:_\'\"+!%/=()\\|Ä€÷×[]$~^ˇ";
    public static final String workDir = System.getProperty("user.home") + "\\AppData\\Roaming\\MessageCounter";
    
    private static BufferedReader reader;
    
    public static void main(String[] args) throws IOException {
        System.out.println("---=== Welcome to MrExplolde's Messenger Information tool! ===---\nPlease specify the path your downloaded zip file.\n\nPlease note that if you wish to use this tool, all your downloaded data must be json formatted (as been selected at the Facebook settings)");
        reader = new BufferedReader(new InputStreamReader(System.in));
        File contentFile = new File(reader.readLine());
        
        if (!contentFile.exists()) {
            System.out.println("The specified file doesn't exist!");
            return;
        }
        
        if (contentFile.isDirectory()) {
            //unzipped
            System.out.println("Unzipped data detected...");
            proccessData(contentFile);
        } else {
            //zipped
            File userdata = new File(workDir, contentFile.getName());
            userdata.mkdirs();
            long start = System.currentTimeMillis();
            try {
                System.out.println("Uncompressing data... (3 minutes)");
                unzip(userdata, contentFile);
            } catch (IOException e) {
                System.err.println("Failed to uncompress user data archive.\nThings you can try: Place the archive in non-admin right required folder");
                e.printStackTrace();
                return;
            }
            System.out.println("Done! ("+ (System.currentTimeMillis() - start) + "ms)\n\n");
            
            proccessData(userdata);
        }
        
    }
    
    //from a directory structure
    protected static void proccessData(File userdata) {
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
                conv = gson.fromJson(repair(sb.toString()), Conversation.class);
                //removing emoji unicode codes
                for (Message m : conv.messages) {
                    m.content = outsideRange(m.content);
                }
                //export message history to file
                MessageExporter exporter = new MessageExporter(conv);
                exporter.start();
            } catch (Exception e) {
                conv = unknown(person.getName());
                e.printStackTrace();
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
            } catch (Exception e) {/*no folder -> exteption, the int remains 0**/}
            
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
            
            System.out.println("Time between first and last message: " + DurationFormatUtils.formatDurationWords(Math.abs(duration.toMillis()), true, true));
        }
        
        //cleanup user sensitive data
        deleteData(userdata);
        
    }
    
    protected static void deleteData(File userdata) {
        System.out.println("\n\n\n\nDeleting user sensitive data...");
        //deleteFolder(userdata);
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
    //185966ms: nothing changed since. I have no idea now
    //180011ms: wtf
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
    
    /**
     * Repairing encoding errors, and removing emoji-leftovers, unreadable characters.
     * 
     * IDK is it so fucking hard to export in utf-8? why I have to do all this shit????????
     * @param string
     * @return
     */
    private static String repair(String string) {
        String correct = string//with escaping
                                .replaceAll("\\u00c3\\u00a1", "á")
                                .replaceAll("\\u00c3\\u00a0", "á")
                                .replaceAll("\\u00c3\\u00b6", "ö")
                                .replaceAll("\\u00c3\\u00ad", "í")
                                .replaceAll("\\u00c3\\u00b3", "ó")
                                .replaceAll("\\u00c3\\u00a9", "é")
                                .replaceAll("\\u00c3\\u00bc", "ü")
                                .replaceAll("\\u00c3\\u0089", "É")
                                .replaceAll("\\u00c5\\u0091", "ő")
                                .replaceAll("\\u00c5\\u00ab", "ü")
                                .replaceAll("\\u00c3\\u00a8", "é")
                                .replaceAll("\\u00c5\\u0091", "ő")
                                .replaceAll("\\u00c3\\u00ba", "Ú")
                                .replaceAll("\\u00c3\\u00ba", "ú")
                                .replaceAll("\\u00c5\\u00b1", "ű")
                                //other method
                                .replace("\u00c3\u00a1", "á")
                                .replace("\u00c3\u00a0", "á")
                                .replace("\u00c3\u00b6", "ö")
                                .replace("\u00c3\u00ad", "í")
                                .replace("\u00c3\u00b3", "ó")
                                .replace("\u00c3\u00a9", "é")
                                .replace("\u00c3\u00bc", "ü")
                                .replace("\u00c3\u0089", "É")
                                .replace("\u00c5\u0091", "ő")
                                .replace("\u00c5\u00ab", "ü")
                                .replace("\u00c3\u00a8", "é")
                                .replace("\u00c5\u0091", "ő")
                                .replace("\u00c3\u00ba", "Ú")
                                .replace("\u00c3\u00ba", "ú")
                                .replace("\u00c5\u00b1", "ű")
                                //with escaping
                                .replace("\\u00c3\\u00a1", "á")
                                .replace("\\u00c3\\u00a0", "á")
                                .replace("\\u00c3\\u00b6", "ö")
                                .replace("\\u00c3\\u00ad", "í")
                                .replace("\\u00c3\\u00b3", "ó")
                                .replace("\\u00c3\\u00a9", "é")
                                .replace("\\u00c3\\u00bc", "ü")
                                .replace("\\u00c3\\u0089", "É")
                                .replace("\\u00c5\\u0091", "ő")
                                .replace("\\u00c5\\u00ab", "ü")
                                .replace("\\u00c3\\u00a8", "é")
                                .replace("\\u00c5\\u0091", "ő")
                                .replace("\\u00c3\\u00ba", "Ú")
                                .replace("\\u00c3\\u00ba", "ú")
                                .replace("\\u00c5\\u00b1", "ű");
        //System.out.println("====\n" + string + "\n" + correct);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return correct;
    }
    
    /**
     * Remove every character wich is outside of the normal_range
     * 
     * @param string the string input
     * @return the corrected string
     */
    private static String outsideRange(String string) {
        //why tho
        return string;
        /*
        if (string != null) {
            char[] range = normal_range.toCharArray();
            for (int i = 0; i < string.length(); i++) {
                char current = string.charAt(i);
                if (!ArrayUtils.contains(range, current)) {
                    System.out.println("Outside character: " + current);
                    return string.replaceAll(String.valueOf(current), "[x]");
                }
            }
        }
        return " ";
        */
    }
}

class MessageExporter extends Thread {
    
    private Conversation conversation;
    
    public MessageExporter(Conversation conversation) {
        this.conversation = conversation;
    }
    
    @Override
    public void run() {
        System.out.println("Exporting messaging history to file " + conversation.title + ".txt");
        
        try {
            PrintWriter writer = new PrintWriter(new File(Main.workDir, conversation.title + ".txt"));
        
            Message[] messages = conversation.messages;
            //from old to new messages
            ArrayUtils.reverse(messages);
            Message prev = null;
            for (Message current : messages) {
                LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(current.timestamp_ms), ZoneId.systemDefault());
                if (prev != null) {
                    //not first, check time between messages, if > 2h print time
                    LocalDateTime prev_timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(prev.timestamp_ms), ZoneId.systemDefault());
                    Duration duration = Duration.between(prev_timestamp, timestamp);
                    if (duration.toHours() > 2) {
                        writer.println(timestamp.getYear() + " " + timestamp.getMonth().toString() + " " + timestamp.getDayOfMonth() + " " + timestamp.getHour() + ":" + timestamp.getMinute());
                    }
                } else {
                    //first message, displaying time
                    writer.println(timestamp.getYear() + " " + timestamp.getMonth().toString() + " " + timestamp.getDayOfMonth() + " " + timestamp.getHour() + ":" + timestamp.getMinute());
                    writer.println("## Please note that emojis not supported in texts. ##");
                }
                
                writer.println(current.sender_name.split(" ")[0] + ": " + current.content);
                prev = current;
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
