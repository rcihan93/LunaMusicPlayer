package com.example.raif.auraplayer;



public class Song {
    private long id;
    private String title;
    private String artist;
    private String path;

    public Song(long songID, String songTitle, String songArtist, String path) {
        id=songID;
        title=songTitle;
        artist=songArtist;
        this.path = path;
    }

    public String getPath() { return this.path; }
    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
}
