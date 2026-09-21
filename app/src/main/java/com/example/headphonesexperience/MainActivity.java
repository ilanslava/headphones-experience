package com.example.headphonesexperience;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
    LinearLayout root, content;
    TextView title, subtitle;

    int bg=Color.rgb(7,7,9), panel=Color.rgb(18,18,22), text=Color.WHITE, muted=Color.rgb(155,155,165), accent=Color.rgb(220,220,230);

    @Override public void onCreate(Bundle b){ super.onCreate(b); home(); }

    TextView tv(String s,float z){ TextView v=new TextView(this); v.setText(s); v.setTextSize(z); v.setTextColor(text); v.setPadding(0,8,0,8); return v; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextColor(text); return b; }
    void shell(String t,String sub){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(22,18,22,18); root.setBackgroundColor(bg);
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=tv("‹",36); back.setVisibility(t.equals("EXPERIENCE")?View.GONE:View.VISIBLE); back.setOnClickListener(v->home());
        bar.addView(back,new LinearLayout.LayoutParams(55,60)); 
        title=tv(t,22); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); bar.addView(title,new LinearLayout.LayoutParams(0,60,1));
        TextView gear=tv("•••",24); gear.setGravity(Gravity.CENTER); bar.addView(gear,new LinearLayout.LayoutParams(55,60));
        root.addView(bar);
        subtitle=tv(sub,14); subtitle.setTextColor(muted); root.addView(subtitle);
        ScrollView sc=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0,12,0,20); sc.addView(content); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }
    TextView section(String s){ TextView v=tv(s,13); v.setTextColor(muted); v.setPadding(4,20,4,8); content.addView(v); return v; }
    void card(String name,String sub,View.OnClickListener c){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(18,14,18,14); box.setBackgroundColor(panel);
        TextView a=tv(name,18); a.setTypeface(Typeface.DEFAULT,Typeface.BOLD); box.addView(a);
        TextView d=tv(sub,13); d.setTextColor(muted); box.addView(d);
        box.setOnClickListener(c); content.addView(box,new LinearLayout.LayoutParams(-1,72){ {setMargins(0,0,0,10);} });
    }
    void home(){
        shell("EXPERIENCE","Transform the way your headphones sound.");
        TextView hero=tv("YOUR SOUND",32); hero.setTypeface(Typeface.DEFAULT,Typeface.BOLD); content.addView(hero);
        TextView now=tv("Nothing playing in Experience",16); now.setTextColor(muted); content.addView(now);
        section("NOW PLAYING");
        card("Open audio","Play a local track inside Experience",v->showPlayer());
        section("SOUND");
        card("Effects","Spatial · Reverb · 8D · Loudness",v->effects());
        card("EQ","Shape bass, mids and highs",v->eq());
        card("Profiles","Save your favorite sound",v->profiles());
        section("CONTROL");
        card("App controls","Volume and playback controls",v->appControls());
        card("Output","Headphones · Speaker · Bluetooth",v->output());
    }
    void showPlayer(){
        shell("NOW PLAYING","Experience owns this playback session.");
        TextView n=tv("LOCAL AUDIO",28); n.setTypeface(Typeface.DEFAULT,Typeface.BOLD); content.addView(n);
        TextView d=tv("Choose an audio file on your phone and play it through Experience. No upload.",15); d.setTextColor(muted); content.addView(d);
        Button open=btn("Choose audio"); content.addView(open); open.setOnClickListener(v->Toast.makeText(this,"Player source coming next",Toast.LENGTH_SHORT).show());
        Button play=btn("▶  Play"); content.addView(play); play.setOnClickListener(v->Toast.makeText(this,"Playback engine coming next",Toast.LENGTH_SHORT).show());
        section("QUICK EFFECTS");
        card("Spatial","Wide, immersive presentation",v->Toast.makeText(this,"Spatial selected",Toast.LENGTH_SHORT).show());
        card("Reverb","Add room and depth",v->Toast.makeText(this,"Reverb selected",Toast.LENGTH_SHORT).show());
        card("8D","Moving stereo field",v->Toast.makeText(this,"8D selected",Toast.LENGTH_SHORT).show());
    }
    void effects(){
        shell("EFFECTS","Quick transformations.");
        card("Spatial","Wide and immersive",v->Toast.makeText(this,"Spatial",Toast.LENGTH_SHORT).show());
        card("Reverb","Room and depth",v->Toast.makeText(this,"Reverb",Toast.LENGTH_SHORT).show());
        card("8D","Moving stereo field",v->Toast.makeText(this,"8D",Toast.LENGTH_SHORT).show());
        card("Loudness","More presence without clipping",v->Toast.makeText(this,"Loudness",Toast.LENGTH_SHORT).show());
        card("Clean","Return to original",v->Toast.makeText(this,"Clean",Toast.LENGTH_SHORT).show());
    }
    void eq(){
        shell("EQUALIZER","Your sound, your curve.");
        String[] bands={"60 Hz   BASS","250 Hz   LOW","1 kHz   MID","4 kHz   PRESENCE","12 kHz   AIR"};
        for(String x:bands){ section(x); SeekBar s=new SeekBar(this); s.setProgress(50); content.addView(s); }
        Button save=btn("Save as profile"); content.addView(save); save.setOnClickListener(v->profiles());
    }
    void profiles(){
        shell("PROFILES","One tap changes the whole experience.");
        card("Original","Natural playback",v->{});
        card("Music","Bass + space",v->{});
        card("Podcast","Voice clarity",v->{});
        card("Movie","Space + depth",v->{});
        Button c=btn("+  Create profile"); content.addView(c); c.setOnClickListener(v->Toast.makeText(this,"Profile editor coming next",Toast.LENGTH_SHORT).show());
    }
    void appControls(){
        shell("APP CONTROLS","Your apps, your volume.");
        card("Music","100%",v->{}); card("Podcast","85%",v->{}); card("Games","60%",v->{});
    }
    void output(){
        shell("OUTPUT","Where should Experience play?");
        card("Headphones","Connected",v->{}); card("Bluetooth","Available devices",v->{}); card("Phone speaker","Available",v->{});
    }
}