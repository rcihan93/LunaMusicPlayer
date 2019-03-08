package com.example.raif.auraplayer;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SongAdapter extends BaseAdapter {

    private ArrayList<Song> songs;
    private ArrayList<String> songsPath;
    private LayoutInflater songInf;
    private IPlaySong mainActivity;
    private boolean onClick;


    public SongAdapter()
    {
    }

    private List<View> _views,_views2;
    private TextView songView,artistView;
    private LinearLayout songLay;


    public SongAdapter(IPlaySong mainActivity, Context c, ArrayList<Song> theSongs){
        super();
        songs=theSongs;
        songInf=LayoutInflater.from(c);
        this.mainActivity=mainActivity;
        this._views = new ArrayList<View>();
        this._views2=new ArrayList<View>();
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    private int lastPosition;

    public int getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(int lastPosition) {
        this.lastPosition = lastPosition;
    }

    public void selectMusic(int position)    {
        _views.get(position).setBackgroundColor(Color.parseColor("#D3D3D3"));
        _views2.get(position).setBackgroundColor(Color.parseColor("#D3D3D3"));

    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        //map to song layout
        songLay = (LinearLayout)songInf.inflate
                (R.layout.song, parent, false);
        //get title and artist views
        songView = (TextView)songLay.findViewById(R.id.song_title);
        artistView = (TextView)songLay.findViewById(R.id.song_artist);
        //get song using position
        Song currSong = songs.get(position);
        //get title and artist strings
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        //set position as tag
        songLay.setTag(position);
        songView.setTextColor(Color.parseColor("#ffff4444"));
        artistView.setTextColor(Color.parseColor("#C92042"));
        //songView.setHeight(54);
        //songView.setTextSize(15);

        //artistView.setHeight(54);
        //artistView.setTextSize(15);

        Collections.sort(songs, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return o1.getTitle().compareToIgnoreCase(o2.getTitle());
            }
        });

        _views.add(songView);
        _views2.add(artistView);



        songView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    mainActivity.play(String.valueOf(songs.get(position).getID()), position);

                    //reset();
                    //selectMusic(position);
                    lastPosition = position;
                    onClick = true;


            }
        });
        artistView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.play(String.valueOf(songs.get(position).getID()),position);

                //reset();
                //selectMusic(position);
                lastPosition=position;
                onClick=true;
            }
        });



        return songLay;
    }

    public void reset(){
        for (View viewLittle:_views) {
            viewLittle.setBackgroundColor(Color.TRANSPARENT);
        }
        for (View viewLittle:_views2) {
            viewLittle.setBackgroundColor(Color.TRANSPARENT);
        }

    }


}