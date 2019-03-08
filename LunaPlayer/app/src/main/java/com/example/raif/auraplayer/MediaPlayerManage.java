package com.example.raif.auraplayer;

import android.media.MediaPlayer;
import android.provider.MediaStore;

/**
 * Created by Raif on 12.03.2017.
 */


public class MediaPlayerManage {

    private static MediaPlayerManage _player;

    private MediaPlayerManage()
    {
        _mediaPlayer = new MediaPlayer();
    }

    private MediaPlayer _mediaPlayer;

    public static MediaPlayerManage getInstance()
    {
        if(_player == null)
            _player = new MediaPlayerManage();

        return _player;
    }

    public MediaPlayer getMediaPlayer()
    {
        return  _mediaPlayer;
    }
}
