package com.example.headphonesexperience;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_AUDIO = 41;
    private LinearLayout content;
    private MediaPlayer player;
    private BassBoost bassBoost;
    private PresetReverb reverb;
    private Virtualizer virtualizer;
    private LoudnessEnhancer loudness;
    private Equalizer equalizer;
    private boolean spatialOn=false, reverbOn=false, eightDOn=false, loudnessOn=false;
    private TextView nowTitle, nowState;
    private Handler handler = new Handler();
    private float pan = 0f;

    private final int bg=Color.rgb(8,8,10), panel=Color.rgb(20,20,24), panel2=Color.rgb(27,27,32);
    private final int text=Color.WHITE, muted=Color.rgb(165,165,176), accent=Color.rgb(225,225,235);

    @Override public void onCreate(Bundle b){ super.onCreate(b); home(); }

    private TextView text(String s,float size){
        TextView v=new TextView(this);
        v.setText(s); v.setTextSize(size); v.setTextColor(text);
        v.setIncludeFontPadding(true);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private TextView label(String s){
        TextView v=text(s,12); v.setTextColor(muted); v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        v.setLetterSpacing(.08f); v.setPadding(2,18,2,8); return v;
    }

    private GradientView card(String icon,String name,String desc,View.OnClickListener click){
        GradientView box=new GradientView(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(16,12,14,12);
        box.setBackgroundColor(panel);
        TextView ico=text(icon,20); ico.setGravity(Gravity.CENTER);
        ico.setBackgroundColor(panel2);
        box.addView(ico,new LinearLayout.LayoutParams(48,48));
        LinearLayout words=new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(14,0,6,0);
        TextView a=text(name,17); a.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        TextView d=text(desc,12); d.setTextColor(muted);
        words.addView(a,new LinearLayout.LayoutParams(-1,28));
        words.addView(d,new LinearLayout.LayoutParams(-1,28));
        box.addView(words,new LinearLayout.LayoutParams(0,64,1));
        TextView arrow=text("›",28); arrow.setTextColor(muted); arrow.setGravity(Gravity.CENTER);
        box.addView(arrow,new LinearLayout.LayoutParams(28,64));
        box.setOnClickListener(click);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,76); lp.setMargins(0,0,0,10);
        content.addView(box,lp);
        return box;
    }

    // Simple subclass so cards can be visually distinct without relying on fragile Button themes.
    private static class GradientView extends LinearLayout {
        GradientView(android.content.Context c){ super(c); setClickable(true); }
    }

    private void shell(String title,String sub){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,14,20,14); root.setBackgroundColor(bg);

        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=text("‹",34);
        back.setGravity(Gravity.CENTER); back.setOnClickListener(v->home());
        bar.addView(back,new LinearLayout.LayoutParams(48,58));
        TextView t=text(title,22); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        bar.addView(t,new LinearLayout.LayoutParams(0,58,1));
        TextView menu=text("⋯",28); menu.setGravity(Gravity.CENTER); bar.addView(menu,new LinearLayout.LayoutParams(42,58));
        root.addView(bar);

        TextView s=text(sub,13); s.setTextColor(muted); s.setPadding(0,0,0,10); root.addView(s);

        ScrollView scroll=new ScrollView(this);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0,4,0,24);
        scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    private void home(){
        shell("EXPERIENCE","Transform the way your headphones sound.");

        TextView hero=text("YOUR SOUND",32); hero.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        hero.setPadding(0,10,0,2); content.addView(hero);
        nowState=text(player!=null && player.isPlaying() ? "PLAYING" : "NOT PLAYING",13);
        nowState.setTextColor(muted); content.addView(nowState);

        content.addView(label("NOW PLAYING"));
        card("▶","Now Playing",nowTitle==null ? "Choose a local audio file" : nowTitle.getText().toString(),v->showPlayer());

        content.addView(label("SOUND"));
        card("✦","Effects",effectsSummary(),v->effects());
        card("EQ","Equalizer","Bass · mids · presence · air",v->eq());
        card("◉","Profiles","Original · Music · Podcast · Movie",v->profiles());

        content.addView(label("CONTROL"));
        card("▣","App Controls","Volume profiles for your apps",v->appControls());
        card("⌁","Output","Headphones · Bluetooth · Speaker",v->output());
    }

    private String effectsSummary(){
        StringBuilder s=new StringBuilder();
        if(spatialOn)s.append("Spatial ");
        if(reverbOn)s.append("Reverb ");
        if(eightDOn)s.append("8D ");
        if(loudnessOn)s.append("Loudness ");
        return s.length()==0 ? "Spatial · Reverb · 8D · Loudness" : s.toString().trim();
    }

    private void showPlayer(){
        shell("NOW PLAYING","Experience owns this playback session.");
        nowTitle=text("No track selected",24); nowTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        content.addView(nowTitle);
        nowState=text(player!=null&&player.isPlaying()?"PLAYING":"READY",13); nowState.setTextColor(muted); content.addView(nowState);

        content.addView(label("AUDIO"));
        card("＋","Choose audio","Pick an audio file stored on this phone",v->pickAudio());

        Button play=new Button(this); play.setText(player!=null&&player.isPlaying()?"❚❚  Pause":"▶  Play");
        play.setAllCaps(false); play.setTextSize(16); play.setTextColor(text);
        content.addView(play,new LinearLayout.LayoutParams(-1,58));
        play.setOnClickListener(v->{
            if(player==null){ Toast.makeText(this,"Choose an audio file first.",Toast.LENGTH_SHORT).show(); return; }
            if(player.isPlaying()){ player.pause(); play.setText("▶  Play"); nowState.setText("PAUSED"); }
            else { player.start(); play.setText("❚❚  Pause"); nowState.setText("PLAYING"); start8D(); }
        });

        content.addView(label("QUICK EFFECTS"));
        card("✦","Spatial",spatialOn?"ON":"OFF",v->toggleSpatial());
        card("◎","Reverb",reverbOn?"ON":"OFF",v->toggleReverb());
        card("↔","8D",eightDOn?"ON":"OFF",v->toggle8D());
        card("↗","Loudness",loudnessOn?"ON":"OFF",v->toggleLoudness());
    }

    private void pickAudio(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("audio/*");
        startActivityForResult(i,PICK_AUDIO);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_AUDIO && resultCode==RESULT_OK && data!=null && data.getData()!=null){
            Uri uri=data.getData();
            try{
                releasePlayer();
                player=new MediaPlayer();
                player.setDataSource(this,uri);
                player.setOnPreparedListener(mp->{
                    attachEffects(mp.getAudioSessionId());
                    mp.start();
                    nowState.setText("PLAYING");
                    start8D();
                    Toast.makeText(this,"Playing with Experience effects.",Toast.LENGTH_SHORT).show();
                    showPlayer();
                });
                player.setOnCompletionListener(mp->{ if(nowState!=null) nowState.setText("FINISHED"); stop8D(); });
                player.prepareAsync();
                if(nowTitle!=null) nowTitle.setText("Loading audio…");
            }catch(Exception e){ Toast.makeText(this,"Could not open that audio file.",Toast.LENGTH_LONG).show(); releasePlayer(); }
        }
    }

    private void attachEffects(int session){
        try{
            bassBoost=new BassBoost(0,session); bassBoost.setEnabled(true);
            bassBoost.setStrength((short)500);
        }catch(Exception ignored){}
        try{
            reverb=new PresetReverb(0,session); reverb.setPreset(PresetReverb.PRESET_LARGEHALL); reverb.setEnabled(reverbOn);
        }catch(Exception ignored){}
        try{
            virtualizer=new Virtualizer(0,session); virtualizer.setStrength((short)700); virtualizer.setEnabled(spatialOn);
        }catch(Exception ignored){}
        try{
            loudness=new LoudnessEnhancer(session); loudness.setTargetGain(450); loudness.setEnabled(loudnessOn);
        }catch(Exception ignored){}
        try{
            equalizer=new Equalizer(0,session); equalizer.setEnabled(true);
        }catch(Exception ignored){}
    }

    private void toggleSpatial(){ spatialOn=!spatialOn; if(virtualizer!=null)virtualizer.setEnabled(spatialOn); Toast.makeText(this,spatialOn?"Spatial ON":"Spatial OFF",Toast.LENGTH_SHORT).show(); effects(); }
    private void toggleReverb(){ reverbOn=!reverbOn; if(reverb!=null)reverb.setEnabled(reverbOn); Toast.makeText(this,reverbOn?"Reverb ON":"Reverb OFF",Toast.LENGTH_SHORT).show(); effects(); }
    private void toggleLoudness(){ loudnessOn=!loudnessOn; if(loudness!=null)loudness.setEnabled(loudnessOn); Toast.makeText(this,loudnessOn?"Loudness ON":"Loudness OFF",Toast.LENGTH_SHORT).show(); effects(); }

    private void toggle8D(){
        eightDOn=!eightDOn;
        if(!eightDOn && player!=null) player.setVolume(1f,1f);
        Toast.makeText(this,eightDOn?"8D ON":"8D OFF",Toast.LENGTH_SHORT).show(); effects();
        if(eightDOn) start8D();
    }

    private void start8D(){
        handler.removeCallbacksAndMessages(null);
        if(!eightDOn || player==null || !player.isPlaying()) return;
        handler.post(new Runnable(){ double a=0;
            public void run(){
                if(!eightDOn || player==null || !player.isPlaying()) return;
                a+=0.045;
                float left=(float)(0.72+0.28*Math.sin(a));
                float right=(float)(0.72+0.28*Math.cos(a));
                player.setVolume(left,right);
                handler.postDelayed(this,35);
            }
        });
    }
    private void stop8D(){ handler.removeCallbacksAndMessages(null); if(player!=null)player.setVolume(1f,1f); }

    private void effects(){
        shell("EFFECTS","Real effects apply to audio played inside Experience.");
        card("✦","Spatial",spatialOn?"ON · immersive stereo":"OFF · immersive stereo",v->toggleSpatial());
        card("◎","Reverb",reverbOn?"ON · room depth":"OFF · room depth",v->toggleReverb());
        card("↔","8D",eightDOn?"ON · moving stereo field":"OFF · moving stereo field",v->toggle8D());
        card("↗","Loudness",loudnessOn?"ON · presence":"OFF · presence",v->toggleLoudness());
        card("○","Clean","Turn all effects off",v->{ spatialOn=false;reverbOn=false;eightDOn=false;loudnessOn=false; if(virtualizer!=null)virtualizer.setEnabled(false);if(reverb!=null)reverb.setEnabled(false);if(loudness!=null)loudness.setEnabled(false);stop8D();effects(); });
    }

    private void eq(){
        shell("EQUALIZER","5-band control for the current Experience playback.");
        if(equalizer==null){ card("EQ","Equalizer","Choose and play a track first.",v->showPlayer()); return; }
        short bands=equalizer.getNumberOfBands();
        String[] names={"60 Hz · BASS","250 Hz · LOW","1 kHz · MID","4 kHz · PRESENCE","12 kHz · AIR"};
        for(int i=0;i<5;i++){
            final int band=Math.min(i,bands-1);
            content.addView(label(names[i]));
            SeekBar s=new SeekBar(this); s.setMax(24); s.setProgress(12);
            content.addView(s,new LinearLayout.LayoutParams(-1,46));
            s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar b,int p,boolean from){ if(from && equalizer!=null && bands>0){ short level=(short)((p-12)*100); try{ equalizer.setBandLevel((short)band,level); }catch(Exception ignored){} } }
                public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
            });
        }
    }

    private void profiles(){
        shell("PROFILES","One tap changes the whole Experience.");
        card("○","Original","Natural playback",v->setProfile("Original"));
        card("♫","Music","More bass and space",v->setProfile("Music"));
        card("◉","Podcast","Clearer voice",v->setProfile("Podcast"));
        card("□","Movie","More room and depth",v->setProfile("Movie"));
    }

    private void setProfile(String p){
        if("Original".equals(p)){ spatialOn=false;reverbOn=false;loudnessOn=false; }
        if("Music".equals(p)){ spatialOn=true;reverbOn=false;loudnessOn=true; }
        if("Podcast".equals(p)){ spatialOn=false;reverbOn=false;loudnessOn=true; }
        if("Movie".equals(p)){ spatialOn=true;reverbOn=true;loudnessOn=false; }
        if(virtualizer!=null)virtualizer.setEnabled(spatialOn); if(reverb!=null)reverb.setEnabled(reverbOn); if(loudness!=null)loudness.setEnabled(loudnessOn);
        Toast.makeText(this,p+" profile applied.",Toast.LENGTH_SHORT).show(); home();
    }

    private void appControls(){
        shell("APP CONTROLS","Saved volume targets for your apps.");
        card("♫","Music","100% · profile volume",v->Toast.makeText(this,"Music volume target: 100%",Toast.LENGTH_SHORT).show());
        card("◉","Podcast","85% · profile volume",v->Toast.makeText(this,"Podcast volume target: 85%",Toast.LENGTH_SHORT).show());
        card("□","Games","60% · profile volume",v->Toast.makeText(this,"Games volume target: 60%",Toast.LENGTH_SHORT).show());
        content.addView(label("IMPORTANT"));
        TextView info=text("Android does not give a normal app the same global per-app mixer access Samsung has. These controls are Experience profiles, not a promise to change another app's output.",13);
        info.setTextColor(muted); content.addView(info);
    }

    private void output(){
        shell("OUTPUT","Experience follows the device audio route.");
        card("⌁","Headphones","Use the connected wired/Bluetooth output",v->Toast.makeText(this,"Android chooses the active headphone route.",Toast.LENGTH_SHORT).show());
        card("◌","Bluetooth","Available through Android audio routing",v->Toast.makeText(this,"Connect Bluetooth in Android settings.",Toast.LENGTH_SHORT).show());
        card("▣","Phone speaker","Use the built-in speaker",v->Toast.makeText(this,"Output follows Android routing.",Toast.LENGTH_SHORT).show());
    }

    private void releasePlayer(){
        stop8D();
        if(bassBoost!=null){try{bassBoost.release();}catch(Exception ignored){} bassBoost=null;}
        if(reverb!=null){try{reverb.release();}catch(Exception ignored){} reverb=null;}
        if(virtualizer!=null){try{virtualizer.release();}catch(Exception ignored){} virtualizer=null;}
        if(loudness!=null){try{loudness.release();}catch(Exception ignored){} loudness=null;}
        if(equalizer!=null){try{equalizer.release();}catch(Exception ignored){} equalizer=null;}
        if(player!=null){try{player.release();}catch(Exception ignored){} player=null;}
    }

    @Override protected void onDestroy(){ releasePlayer(); super.onDestroy(); }
}
