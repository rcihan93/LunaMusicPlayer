package com.example.raif.auraplayer;

/**
 * Created by Raif on 24.12.2016.
 */


public interface IPlaySong {
    public void play(String path, int position);
    public void pause(String path);
    public void randomMusic();
    public void nextMusic();
    public void previousMusic();
    public void smoothScroll(int position);
    public int getPlayingPosition();
    public int getSongListSize();
    public String getSongPath(int position);
    public String getSongArtist(int position);
    public boolean getMpPlaying();
}
