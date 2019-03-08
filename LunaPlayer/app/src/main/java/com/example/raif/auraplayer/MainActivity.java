package com.example.raif.auraplayer;

import android.*;
import android.Manifest;
import android.app.DownloadManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.jar.*;


import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.support.v7.widget.SearchView;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;


public class MainActivity extends AppCompatActivity implements IPlaySong,MediaPlayer.OnPreparedListener
                                                    ,SearchView.OnQueryTextListener{


    private ArrayList<Song> songList;
    private ListView songView;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private SeekBar seekBar;
    private final Handler handler = new Handler();
    private TextView txtCurrentTime;
    private TextView txtDuration;
    private TextView txtIsPlaying;
    private ImageButton btnBackward;
    private ImageButton btnForward;
    private ImageButton btnPlay;
    private ImageButton btnPause;
    private PhoneStateListener phoneStateListener;

    private MediaPlayer mp;
    private String Spath;
    private boolean isPlaying;
    private int lastPosition;
    private int musicCount;
    private int forwardTime = 5000;
    private int backwardTime = 5000;
    private boolean onCall, onRepeat = false, onShuffle = false, onPause = false, onStart = false,themeClick=false;
    private long currentDuration, currentPosition;
    private long second, minute;
    private SongAdapter songAdapter;
    private InterstitialAd interstitialAd;
    private int durationShowTime=1500;
    private final int REQUEST_PERMISSION_READ_STORAGE_STATE=123;

    //seekbarın müzik ilerlemesini takip etmesi için gerekli blok
    private final Runnable updatePositionRunnable = new Runnable() {
        @Override
        public void run() {
            updatePosition();
        }
    };
    private final Runnable updateTime = new Runnable() {
        @Override
        public void run() {
            updateTime();
        }
    };

    private void updatePosition() {
        handler.removeCallbacks(updatePositionRunnable);

        seekBar.setProgress(mp.getCurrentPosition());

        handler.postDelayed(updatePositionRunnable, 2000);
    }


    //end seekbar müzik ilerleme takibi
    private void updateTime() {
        long second, minute;
        handler.removeCallbacks(updateTime);
        second = (mp.getCurrentPosition() / 1000) % 60;
        minute = mp.getCurrentPosition() / 60000;
        txtCurrentTime.setText((int) minute + ":" + (int) second);
        handler.postDelayed(updateTime, 1000);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)!=
                PackageManager.PERMISSION_GRANTED){

            checkPermissions();
        }
        else{

        mp = MediaPlayerManage.getInstance().getMediaPlayer();

        songView = (ListView) findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        //geçiş reklamı
        interstitialAd=new InterstitialAd(this);
        interstitialAd.setAdUnitId(getString(R.string.interstitialAdUnitID));
        //AdMob reklamları
        MobileAds.initialize(getApplicationContext(), getString(R.string.bannerAdUnitId));
        AdView mAdView = (AdView) findViewById(R.id.adView7);
        //AdMob test device
        final AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("9714026D3A772E3C62C1DF0307D647B0")
                //.addTestDevice("711F7FAD10E221DD3BDF7AB332CD1B27")
                //.addTestDevice("04187F794B8C74072641859BE2FB0FF2")
                //.addTestDevice("A87F102322C3F7FA0C2E8FD764FA267C")

                .build();
        mAdView.loadAd(adRequest);

        /*Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return o1.getTitle().compareToIgnoreCase(o2.getTitle());
            }
        });*/

        SongAdapter songAdt = new SongAdapter(this, this, songList);
        songView.setAdapter(songAdt);

        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnPause = (ImageButton) findViewById(R.id.btnPause);
        ImageButton btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
        ImageButton btnNext = (ImageButton) findViewById(R.id.btnNext);
        final ImageButton btnShuffle = (ImageButton) findViewById(R.id.btnShuffle);
        final ImageButton btnRepeat = (ImageButton) findViewById(R.id.btnRepeat);
        seekBar = (SeekBar) findViewById(R.id.songProgressBar);
        btnBackward = (ImageButton) findViewById(R.id.btnBackward);
        btnForward = (ImageButton) findViewById(R.id.btnForward);
        txtCurrentTime = (TextView) findViewById(R.id.txtCurrentTime);
        txtDuration = (TextView) findViewById(R.id.txtDuration);
        txtIsPlaying = (TextView) findViewById(R.id.txtIsPlaying);
        txtIsPlaying.setTypeface(txtIsPlaying.getTypeface(), Typeface.BOLD);

        //Arama yapıldığında müzikçaları durdurmak için gerekli blok
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    mp.pause();
                    onCall = true;
                }
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    if (onCall == true) {
                        mp.start();
                        onCall = false;
                    }

                }
                if (state == TelephonyManager.CALL_STATE_OFFHOOK) {

                }
            }
        };
        TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (manager != null) {
            manager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        //end müzikçalar durdurma

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) mp.seekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onStart) {
                    onStart = false;
                    onPause = true;
                    btnPlay.setImageResource(R.drawable.play);
                    btnPause.setImageResource(R.drawable.pauseactive);
                    pause("");
                } else {
                    onStart = true;
                    onPause = false;
                    btnPlay.setImageResource(R.drawable.playactive);
                    btnPause.setImageResource(R.drawable.pausepassive);
                    play(Spath, lastPosition);
                }
            }
        });
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onPause) {
                    onPause = false;
                    onStart = true;
                    btnPause.setImageResource(R.drawable.pausepassive);
                    btnPlay.setImageResource(R.drawable.playactive);
                    play(Spath,lastPosition);
                } else {
                    onPause = true;
                    onStart = false;
                    btnPause.setImageResource(R.drawable.pauseactive);
                    btnPlay.setImageResource(R.drawable.play);
                    pause("");
                }
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                nextMusic();
            }
        });
        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousMusic();
            }
        });
        btnRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onRepeat) {
                    onRepeat = false;
                    Toast.makeText(getApplicationContext(), "Repeat Off", Toast.LENGTH_SHORT).show();
                    btnRepeat.setImageResource(R.drawable.repeatpassive);
                } else {
                    onRepeat = true;
                    Toast.makeText(getApplicationContext(), "Repeat is ON", Toast.LENGTH_SHORT).show();
                    onShuffle = false;
                    btnRepeat.setImageResource(R.drawable.repeatactive);
                    btnShuffle.setImageResource(R.drawable.shufflepassive);
                }
                showAds();
            }
        });
        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onShuffle) {
                    onShuffle = false;
                    Toast.makeText(getApplicationContext(), "Shuffle Off", Toast.LENGTH_SHORT).show();
                    btnShuffle.setImageResource(R.drawable.shufflepassive);
                } else {
                    onShuffle = true;
                    Toast.makeText(getApplicationContext(), "Shuffle is ON", Toast.LENGTH_SHORT).show();
                    onRepeat = false;
                    btnShuffle.setImageResource(R.drawable.shuffleactive);
                    btnRepeat.setImageResource(R.drawable.repeatpassive);
                }
                showAds();
            }
        });
        getSongList();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                int currentPosition = lastPosition;
                /*String nextPath=String.valueOf(songList.get(currentPosition+1).getID());
                play(nextPath,currentPosition+1);*/
                if (onRepeat) {
                    String repeatPath = String.valueOf(songList.get(currentPosition).getID());
                    play(repeatPath, currentPosition);
                } else if (onShuffle) {
                    randomMusic();
                } else {
                    if (currentPosition < songList.size() - 1) {
                        String nextPath = String.valueOf(songList.get(currentPosition + 1).getID());
                        play(nextPath, currentPosition + 1);
                    } else {
                        String firstPath = String.valueOf(songList.get(0).getID());
                        play(firstPath, 0);
                    }
                }
            }
        });
        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPosition = mp.getCurrentPosition();
                if (currentPosition + forwardTime <= mp.getDuration())
                    mp.seekTo(currentPosition + forwardTime);
                else
                    mp.seekTo(mp.getCurrentPosition());
            }
        });
        btnBackward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPosition = mp.getCurrentPosition();
                if (currentPosition - backwardTime >= 0)
                    mp.seekTo(currentPosition - backwardTime);
                else
                    mp.seekTo(0);
            }
        });
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdOpened() {
                showInterstitialAd();
            }

            @Override
            public void onAdClosed() {
                showInterstitialAd();
            }

            @Override
            public void onAdLoaded() {
                showInterstitialAd();
            }
        });
        showInterstitialAd();
        }

    }
    private void showInterstitialAd(){
        AdRequest adRequestIn=new AdRequest.Builder()
                .addTestDevice("9714026D3A772E3C62C1DF0307D647B0")
               // .addTestDevice("711F7FAD10E221DD3BDF7AB332CD1B27")
               // .addTestDevice("04187F794B8C74072641859BE2FB0FF2")
                //.addTestDevice("A87F102322C3F7FA0C2E8FD764FA267C")

                .build();
        interstitialAd.loadAd(adRequestIn);

    }
    private void showAds(){
        interstitialAd.isLoaded();
        interstitialAd.show();
    }

    public void getSongList() {        //retrieve song info

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int indexColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.getPath());
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisIndex = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.getPath() + "/" + thisTitle;
                songList.add(new Song(thisId, thisTitle, thisArtist, thisTitle));
            }
            while (musicCursor.moveToNext());
        }
        musicCount = (int) songList.size();

    }

    @Override
    public void play(String path, final int position) {
        try {
            if (path != Spath) {
                mp.reset();
                //mp.stop();
                setMediaPlayerDataSource(this, mp, "content://media/external/audio/media/" + path);
                //mp.setDataSource("content://media/external/audio/media/"+path+".mp3");
                mp.prepare();
                mp.start();
                onStart = true;
                onPause = false;
                btnPlay.setImageResource(R.drawable.playactive);
                btnPause.setImageResource(R.drawable.pausepassive);
                lastPosition = position;
                isPlaying = true;
                if (mp.isPlaying()) {
                    updatePosition();
                    updateTime();
                    closeKeyboard();
                    //View listItem=songView.getChildAt(lastPosition);
                    //listItem.setBackgroundColor(Color.parseColor("#D3D3D3"));
                } else
                    handler.removeCallbacks(updatePositionRunnable);
            } else {
                if (!isPlaying)
                    mp.start();

            }
            currentDuration = mp.getDuration();
            second = (currentDuration / 1000) % 60;
            minute = currentDuration / 60000;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        seekBar.setMax(mp.getDuration());
        txtIsPlaying.setText(String.valueOf(songList.get(position).getPath()));
        txtDuration.setText((int) minute + ":" + (int) second);
        smoothScroll(lastPosition);
        final Toast toast=Toast.makeText(getApplicationContext(),songList.get(lastPosition).getPath(),Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP,0,0);
        toast.show();
        Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toast.cancel();
            }
        },1000);
    }
    @Override
    public boolean getMpPlaying(){
        return mp.isPlaying();
    }

    @Override
    public void smoothScroll(int position) {
        songView.smoothScrollToPosition(position);
    }

    @Override
    public int getPlayingPosition() {
        return lastPosition;
    }

    @Override
    public int getSongListSize() {
        return songList.size();
    }

    @Override
    public String getSongPath(int position) {
        return songList.get(position).getPath();
    }

    @Override
    public String getSongArtist(int position) {
        return songList.get(position).getArtist();
    }

    @Override
    public void pause(String path) {
        mp.pause();
        isPlaying = false;
    }


    @Override
    public void randomMusic() {
        Random rnd = new Random();
        int nextSong;
        nextSong = rnd.nextInt(songList.size());
        String rndPath = String.valueOf(songList.get(nextSong).getID());
        play(rndPath, nextSong);
    }

    @Override
    public void nextMusic() {
        int nextSong;
        if (lastPosition < songList.size() - 1) {
            nextSong = lastPosition + 1;
            String nextPath = String.valueOf(songList.get(nextSong).getID());
            play(nextPath, nextSong);
        } else {
            nextSong = 0;
            String nextPath = String.valueOf(songList.get(nextSong).getID());
            play(nextPath, nextSong);
        }

    }

    @Override
    public void previousMusic() {
        int previousSong;
        if (lastPosition - 1 >= 0) {
            previousSong = lastPosition - 1;
            String previousPath = String.valueOf(songList.get(previousSong).getID());
            play(previousPath, previousSong);
        } else {
            previousSong = songList.size() - 1;
            String previousPath = String.valueOf(songList.get(previousSong).getID());
            play(previousPath, previousSong);
        }
    }

    public static void setMediaPlayerDataSource(Context context,
                                                MediaPlayer mp, String fileInfo) throws Exception {

        if (fileInfo.startsWith("content://")) {
            try {
                Uri uri = Uri.parse(fileInfo);
                fileInfo = getRingtonePathFromContentUri(context, uri);
            } catch (Exception e) {
            }
        }

        try {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
                try {
                    setMediaPlayerDataSourcePreHoneyComb(context, mp, fileInfo);
                } catch (Exception e) {
                    setMediaPlayerDataSourcePostHoneyComb(context, mp, fileInfo);
                }
            else
                setMediaPlayerDataSourcePostHoneyComb(context, mp, fileInfo);

        } catch (Exception e) {
            try {
                setMediaPlayerDataSourceUsingFileDescriptor(context, mp,
                        fileInfo);
            } catch (Exception ee) {
                String uri = getRingtoneUriFromPath(context, fileInfo);
                mp.reset();
                mp.setDataSource(uri);
            }
        }
    }

    private static void setMediaPlayerDataSourcePreHoneyComb(Context context,
                                                             MediaPlayer mp, String fileInfo) throws Exception {
        mp.reset();
        mp.setDataSource(fileInfo);
    }

    private static void setMediaPlayerDataSourcePostHoneyComb(Context context,
                                                              MediaPlayer mp, String fileInfo) throws Exception {
        mp.reset();
        mp.setDataSource(context, Uri.parse(Uri.encode(fileInfo)));
    }

    private static void setMediaPlayerDataSourceUsingFileDescriptor(
            Context context, MediaPlayer mp, String fileInfo) throws Exception {
        File file = new File(fileInfo);
        FileInputStream inputStream = new FileInputStream(file);
        mp.reset();
        mp.setDataSource(inputStream.getFD());
        inputStream.close();
    }

    private static String getRingtoneUriFromPath(Context context, String path) {
        Uri ringtonesUri = MediaStore.Audio.Media.getContentUriForPath(path);
        Cursor ringtoneCursor = context.getContentResolver().query(
                ringtonesUri, null,
                MediaStore.Audio.Media.DATA + "='" + path + "'", null, null);
        ringtoneCursor.moveToFirst();

        long id = ringtoneCursor.getLong(ringtoneCursor
                .getColumnIndex(MediaStore.Audio.Media._ID));
        ringtoneCursor.close();

        if (!ringtonesUri.toString().endsWith(String.valueOf(id))) {
            return ringtonesUri + "/" + id;
        }
        return ringtonesUri.toString();
    }

    public static String getRingtonePathFromContentUri(Context context,
                                                       Uri contentUri) {
        String[] proj = {MediaStore.Audio.Media.DATA};
        Cursor ringtoneCursor = context.getContentResolver().query(contentUri,
                proj, null, null, null);
        ringtoneCursor.moveToFirst();

        String path = ringtoneCursor.getString(ringtoneCursor
                .getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

        ringtoneCursor.close();
        return path;
    }

    protected void onPause() {
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        Log.d("CALL","onResume called.");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case  R.id.theme:
                if(themeClick){
                    themeClick=false;
                    item.setIcon(R.drawable.dark);
                    songView.setBackgroundColor(Color.TRANSPARENT);

                }
                else{
                    themeClick=true;
                    item.setIcon(R.drawable.white);
                    songView.setBackgroundColor(Color.BLACK);

                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        String searchItem=query.toLowerCase();
        doSearch(searchItem);
        closeKeyboard();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    public void doSearch(String query){
        boolean result1=false,result2=false;
        String targetPath,targetArtist,notNullPath,notNullArtist;
        for(int i=0;i<songList.size();i++){
            targetArtist=songList.get(i).getArtist();
            targetPath=songList.get(i).getPath();
            if(targetArtist!=null && targetPath!=null){
                notNullArtist=targetArtist.toLowerCase();
                notNullPath=targetPath.toLowerCase();
                result1=notNullArtist.contains(query);
                result2=notNullPath.contains(query);
                if(result1==true||result2==true){
                    //smoothScroll(i);
                    songView.setSelection(i);
                    break;
                }
            }
            /*
            result1=targetPath.contains(query);
            result2=targetArtist.contains(query);
            if(result1==true||result2==true){
                //smoothScroll(i);
                songView.setSelection(i);
                break;
            }
            */

        }
        if(result1==false&&result2==false){
            final Toast toast=Toast.makeText(getApplicationContext(),"No Result",Toast.LENGTH_SHORT);
            toast.show();
            Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    toast.cancel();
                }
            },1000);
        }

    }

    private void closeKeyboard(){
        View view=this.getCurrentFocus();
        InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(),0);
    }

    private void checkPermissions(){

        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)
                !=PackageManager.PERMISSION_GRANTED){

            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)){

            }
            else{
                ActivityCompat.requestPermissions(MainActivity.this,new String[]
                        {Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_PERMISSION_READ_STORAGE_STATE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_READ_STORAGE_STATE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Intent intent=getIntent();
                        finish();
                    startActivity(intent);
                    //permission was granted
                } else {
                    denyPermissionState();
                    //permission denied
                }
                break;
            default:
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void denyPermissionState(){

        int denyPermission=ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if(denyPermission!=PackageManager.PERMISSION_GRANTED){
            if(!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){
                showMessageDialog("Need access to storage. Re-install Please! Or You can open manually options" +
                        ">apps>Luna>permissions "+"İzin gereklidir! Tekrar kurun ya da " +
                        "Manuel Açmak ayarlar>uygulamalar>luna>izinler",
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]
                                    {Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_READ_STORAGE_STATE);
                        finish();

                    }

                });
                return;
            }
            ActivityCompat.requestPermissions(MainActivity.this,new String[]
                    {Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_PERMISSION_READ_STORAGE_STATE);
            return;

        }
    }

    private void showMessageDialog(String message,DialogInterface.OnClickListener onClickListener){
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK",onClickListener)
                .setNegativeButton("Cancel",null)
                .create()
                .show();

    }
}
