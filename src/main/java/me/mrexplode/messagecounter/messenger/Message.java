package me.mrexplode.messagecounter.messenger;


public class Message {
    
    public String sender_name;
    public long timestamp_ms;
    public String content = null;
    public Photo[] photos = null;
    public Reaction[] reactions = null;
    public Gif[] gifs = null;
    public Audio[] audio_files = null;
    public MFile[] files = null;
    public String type;

}
