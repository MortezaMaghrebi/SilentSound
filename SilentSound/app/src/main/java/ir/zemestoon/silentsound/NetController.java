package ir.zemestoon.silentsound;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.UnsupportedEncodingException;

public class NetController {
    MainActivity activity;
    final String MY_PREFS_NAME = "PREFS_SILENT_SOUND";
    SharedPreferences.Editor editor;
    SharedPreferences prefs;

    public static NetController instance;

    public static synchronized NetController getInstance(MainActivity activity) {
        if (instance == null) {
            instance = new NetController(activity);
        }
        return instance;
    }

    public NetController(MainActivity activity) {
        this.activity = activity;
        editor = activity.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        prefs = activity.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    }

    public void DownloadSoundList() throws UnsupportedEncodingException {
        int counter = prefs.getInt("download_sound_counter",2);
        if(counter<2){
            counter++;
            editor.putInt("download_sound_counter",counter);
            editor.commit();
            return;
        }
        RequestQueue queue = Volley.newRequestQueue(activity);
        String url = "https://raw.githubusercontent.com/MortezaMaghrebi/MySoundData/refs/heads/main/soundlist.txt";

        // Variable to store the file content
        final String[] fileContent = {""}; // Using array to allow modification in inner class
        if(BazaarBilling.getInstance(activity).isPremiumActivated())url=url.replace("soundlist","soundlist_pro");
        StringRequest getRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        editor.putInt("download_sound_counter",0);
                        editor.commit();
                        boolean changed = setSoundList(response.trim());
                        if (changed) {
                            activity.loadSounds();
                            ToastUtils.showSafeToast(activity, "لیست صداها آپدیت شد");
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //ToastUtils.showSafeToast(activity, "برای دریافت لیست صداها به اینترنت متصل شوید");
                    }
                }
        );
        queue.getCache().clear();
        queue.add(getRequest);
    }

    public void DownloadMixedList() throws UnsupportedEncodingException {
        int counter = prefs.getInt("download_mixed_counter",2);
        if(counter<2){
            counter++;
            editor.putInt("download_mixed_counter",counter);
            editor.commit();
            return;
        }
        RequestQueue queue = Volley.newRequestQueue(activity);
        String url = "https://raw.githubusercontent.com/MortezaMaghrebi/MySoundData/refs/heads/main/mixedlist.txt";

        // Variable to store the file content
        final String[] fileContent = {""}; // Using array to allow modification in inner class
        if(BazaarBilling.getInstance(activity).isPremiumActivated())url=url.replace("mixedlist","mixedlist_pro");

        StringRequest getRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        editor.putInt("download_mixed_counter",0);
                        editor.commit();
                        boolean changed = setMixedList(response.trim());
                        if (changed) {
                            activity.loadMixes();
                            ToastUtils.showSafeToast(activity, "لیست میکس ها آپدیت شد");
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        //ToastUtils.showSafeToast(activity, "برای دریافت لیست میکس ها به اینترنت متصل شوید");
                    }
                }
        );
        queue.getCache().clear();
        queue.add(getRequest);
    }

    public void DownloadMessage() throws UnsupportedEncodingException {

        int counter = prefs.getInt("download_message_counter",3);
        if(counter<3){
            counter++;
            editor.putInt("download_message_counter",counter);
            editor.commit();
            return;
        }
        RequestQueue queue = Volley.newRequestQueue(activity);
        String url = "https://raw.githubusercontent.com/MortezaMaghrebi/MySoundData/refs/heads/main/message.txt";

        // Variable to store the file content
        final String[] fileContent = {""}; // Using array to allow modification in inner class

        StringRequest getRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        editor.putInt("download_message_counter",0);
                        editor.commit();
                        boolean changed = setMessage(response.trim());
                        if (changed) {
                            showMessageDialog(response);
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {


                    }
                }
        );
        queue.getCache().clear();
        queue.add(getRequest);
    }

    public boolean setSoundList(String soundList) {
        boolean changed = !getSoundList().trim().equals(soundList.trim());
        editor.putString("soundlist", soundList);
        editor.commit();
        return changed;
    }

    public String getSoundList() {
        String soudnlist = prefs.getString("soundlist", "");
        return prefs.getString("soundlist","nature,پرنده,bird.png,nature/bird.mp3,10,0,0\n" +
                "nature,پرندگان,hummingbird.png,nature/birds.mp3,10,0,0\n" +
                "nature,گربه خرخر,cat.png,nature/cat_purring.mp3,10,0,0\n" +
                "nature,جیرجیرک,cricket.png,nature/cricket.mp3,10,0,0\n" +
                "nature,چکه آب,water.png,nature/dripping.mp3,10,0,0\n" +
                "nature,آتش هیزم,campfire.png,nature/firewood.mp3,10,0,0\n" +
                "nature,جنگل,forest.png,nature/forest.mp3,10,0,0\n" +
                "nature,قورباغه,frog.png,nature/frog.mp3,10,0,0\n" +
                "nature,چمنزار,grass.png,nature/grassland.mp3,10,0,0\n" +
                "nature,باران شدید,rain.png,nature/heavy_rain.mp3,10,0,0\n" +
                "nature,پرنده لون,bird.png,nature/loon.mp3,10,0,0\n" +
                "nature,جغد,owl.png,nature/owl.mp3,10,0,0\n" +
                "nature,باران,rain.png,nature/rain.mp3,10,0,0\n" +
                "nature,باران روی سقف,rain.png,nature/rain_on_roof.mp3,10,0,0\n" +
                "nature,باران روی چادر,tent.png,nature/rain_on_tent.mp3,10,0,0\n" +
                "nature,باران روی پنجره,window.png,nature/rain_on_window.mp3,10,0,0\n" +
                "nature,دریا,sea.png,nature/sea.mp3,10,0,0\n" +
                "nature,مرغ دریایی,seagull.png,nature/seagull.mp3,10,0,0\n" +
                "nature,برف,snow.png,nature/snow.mp3,10,0,0\n" +
                "nature,رعد و برق,storm.png,nature/thunder.mp3,10,0,0\n" +
                "nature,زیر آب,submarine.png,nature/under_water.mp3,10,0,0\n" +
                "nature,جریان آب,water.png,nature/water_flow.mp3,10,0,0\n" +
                "nature,آبشار,waterfall.png,nature/waterfall.mp3,10,0,0\n" +
                "nature,نهنگ,whale.png,nature/whale.mp3,10,0,0\n" +
                "nature,باد,wind.png,nature/wind.mp3,10,0,0\n" +
                "nature,گرگ,wolf.png,nature/wolf.mp3,10,0,0\n" +
                "nature,آب روان,water.png,music/dan_gibson/dg_water_flow.mp3,10,0,0\n" +
                "nature,پرنده در باران,bird.png,nature/bird_in_rain.mp3,10,0,0\n" +
                "nature,پرندگان پاییزی,hummingbird.png,nature/birds_autumn.mp3,10,0,0\n" +
                "nature,آواز پرندگان,bird.png,nature/birds_singing.mp3,10,0,0\n" +
                "nature,بلبل,bird.png,nature/bulbul.mp3,10,0,0\n" +
                "nature,جنگل تاریک,forest.png,nature/dark_jungle.mp3,10,0,0\n" +
                "nature,جنگل شب,forest.png,nature/jungle_night.mp3,10,0,0\n" +
                "nature,طبیعت ظهر,sun.png,nature/nature_noon.mp3,10,0,0\n" +
                "nature,طاووس,peacock.png,nature/peacock.mp3,10,0,0\n" +
                "nature,قطرات باران,rain.png,nature/rain_drops.mp3,10,0,0\n" +
                "nature,باران روی سقف ۲,rain.png,nature/rain_on_roof2.mp3,10,0,0\n" +
                "nature,رودخانه,river.png,nature/river.mp3,10,0,0\n" +
                "nature,دریای آرام,sea.png,nature/sea_calm.mp3,10,0,0\n" +
                "nature,دریای شب,sea.png,nature/sea_night.mp3,10,0,0\n" +
                "nature,امواج دریا,waves.png,nature/sea_waves.mp3,10,0,0\n" +
                "nature,امواج آرام دریا ۲,waves.png,nature/sea_waves_calm2.mp3,10,0,0\n" +
                "nature,امواج دریا با باد ملایم,waves.png,nature/sea_waves_light_wind.mp3,10,0,0\n" +
                "nature,رعد و برق ۲,storm.png,nature/thunder2.mp3,10,0,0\n" +
                "nature,باد ۲,wind.png,nature/wind2.mp3,10,0,0\n" +
                "nature,پارو زدن,dinghy.png,nature/oar.mp3,10,0,0\n" +
                "nature,باد ملایم,wind.png,nature/gentle_wind.mp3,10,0,0\n" +
                "nature,جنگل موسمی,rainforest.png,nature/rainforest.mp3,10,0,0\n" +
                "nature,جریان رودخانه,creek.png,nature/river_natural_water.mp3,10,0,0\n" +
                "nature,آب کوهستان,water.png,nature/water_cascade_down_rock.mp3,10,0,0\n" +
                "nature,غروب دریاچه,lake.png,nature/lake_sunset.mp3,10,0,0\n" +
                "music,نور شفابخش,sun.png,music/ai/ai_healing_light.mp3,70,0,0\n" +
                "music,امواج شفابخش,waves.png,music/ai/ai_healing_waves.mp3,70,0,0\n" +
                "music,سرمای قطبی,sun.png,music/ai/ai_antarctic_sadness.mp3,70,0,0\n" +
                "music,بافنده رویا,waves.png,music/ai/ai_dream_weaver.mp3,70,0,0\n" +
                "music,جزر و مد متغیر,tides.png,music/other/other_turning_tides.mp3,70,0,0\n" +
                "noise,نویز قهوه\u200Cای,music.png,noise/brown_noise.mp3,30,0,0\n" +
                "noise,نویز سفید,music.png,noise/white_noise.mp3,30,0,0\n" +
                "wave,آداجیو آلفا,alpha.png,wave/Adagio_Alpha_105_115Hz.mp3,20,0,0\n" +
                "wave,آلفا سعادت,alpha.png,wave/Alpha_Bliss_107_115Hz.mp3,20,0,0\n" +
                "wave,امواج آلفا,alpha.png,wave/Alpha_Brain_Waves.mp3,20,0,0\n" +
                "wave,تمرکز آلفا ۱,alpha.png,wave/Alpha_Focus_107_115Hz.mp3,20,0,0\n" +
                "wave,تمرکز آلفا ۲,alpha.png,wave/Alpha_Focus_127_135Hz.mp3,20,0,0\n" +
                "wave,تمرکز آلفا ۳,alpha.png,wave/Alpha_Focus_97_104Hz.mp3,20,0,0\n" +
                "wave,آلفا اینرورس,alpha.png,wave/Alpha_Innerverse_Reso.mp3,20,0,0\n" +
                "wave,امواج شفاف,alpha.png,wave/Alpha_Lucid_Waves.mp3,20,0,0\n" +
                "wave,مدیتیشن آلفا,alpha.png,wave/Alpha_Meditation.mp3,20,0,0\n" +
                "wave,شب آلفا,alpha.png,wave/Alpha_Night_106_114Hz.mp3,20,0,0\n" +
                "wave,مسیر آلفا,path.png,wave/Alpha_Path_96_105Hz.mp3,20,0,0\n" +
                "wave,رفاه آلفا,alpha.png,wave/Alpha_Prosperity_127_135Hz.mp3,20,0,0\n" +
                "wave,شانت آلفا,alpha.png,wave/Alpha_Shaant_74_82Hz.mp3,20,0,0\n" +
                "wave,سینوس آلفا ۱,alpha.png,wave/Alpha_Sinus_54_57Hz.mp3,20,0,0\n" +
                "wave,سینوس آلفا ۲,alpha.png,wave/Alpha_Sinus_62_66Hz.mp3,20,0,0\n" +
                "wave,سینوس آلفا ۳,alpha.png,wave/Alpha_Sinus_88_94Hz.mp3,20,0,0\n" +
                "wave,سینوس آلفا ۴,alpha.png,wave/Alpha_Sinus_91_101Hz.mp3,20,0,0\n" +
                "wave,روح آلفا,alpha.png,wave/Alpha_Soul_110_117Hz.mp3,20,0,0\n" +
                "wave,کره آلفا,alpha.png,wave/Alpha_Sphere_10Hz.mp3,20,0,0\n" +
                "wave,ترانسند آلفا,alpha.png,wave/Alpha_Transcend_106_114Hz.mp3,20,0,0\n" +
                "wave,یونیورسال آلفا,alpha.png,wave/Alpha_Universal_65_73Hz.mp3,20,0,0\n" +
                "wave,امواج آلفا ۸۸,alpha.png,wave/Alpha_Waves_88_96Hz.mp3,20,0,0\n" +
                "wave,زون آلفا,alpha.png,wave/Alpha_Zone_93_104Hz.mp3,20,0,0\n" +
                "wave,سینوس بتا,beta.png,wave/Beta_Sinus_100_114Hz.mp3,20,0,0\n" +
                "wave,امواج بتا,beta.png,wave/Beta_Waves_110_130Hz.mp3,20,0,0\n" +
                "wave,چرخش دلتا,d.png,wave/Delta_Revolve_125_128Hz.mp3,20,0,0\n" +
                "wave,اکریورم,beta.png,wave/Ecriurem_100_108Hz.mp3,20,0,0\n" +
                "wave,تعادل,balance.png,wave/Equilibrium_96_104Hz.mp3,20,0,0\n" +
                "wave,فلو آلفا,alpha.png,wave/Flow_Alpha_203_211Hz.mp3,20,0,0\n" +
                "wave,حافظه گاما,gamma.png,wave/Gamma_Memory_Training.mp3,20,0,0\n" +
                "wave,سینوس گاما ۱,gamma.png,wave/Gamma_Sinus_100_140Hz.mp3,20,0,0\n" +
                "wave,سینوس گاما ۲,gamma.png,wave/Gamma_Sinus_300_350Hz.mp3,20,0,0\n" +
                "wave,امواج گاما ۸۶,gamma.png,wave/Gamma_Waves_86_89Hz.mp3,20,0,0\n" +
                "wave,گاما ویلو,gamma.png,wave/Gamma_Willow_29_71Hz.mp3,20,0,0\n" +
                "wave,مطالعه داخلی,alpha.png,wave/Inner_Study_110_115Hz.mp3,20,0,0\n" +
                "wave,زندگی,alpha.png,wave/Living_150_158Hz.mp3,20,0,0\n" +
                "wave,لوز آلفا,alpha.png,wave/Luz_Alpha_100_108Hz.mp3,20,0,0\n" +
                "wave,مانترا آلفا_تتا,t.png,wave/Mantra_Alpha_Theta.mp3,20,0,0\n" +
                "wave,فولیا تتا,t.png,wave/Theta_Follia_41_45Hz.mp3,20,0,0\n" +
                "wave,رم تتا,t.png,wave/Theta_Rem_60_66Hz.mp3,20,0,0\n" +
                "wave,راهب آب,water.png,wave/Water_Monk.mp3,20,0,0\n" +
                "story,داستان چمنزار,tree.png,story/story_grassland.mp3,100,0,0\n" +
                "story,داستان قطب شمال,north.png,story/story_north_pole.mp3,100,0,0\n" +
                "story,داستان غواصی,cliff.png,,100,0,0\n" +
                "story,داستان قایق سواری,music.png,,100,0,0\n" +
                "story,داستان دریاچه,lake.png,,100,0,0\n" +
                "story,داستان بزغاله,music.png,,100,0,0\n" +
                "story,داستان کفش آهنی,iron.png,,100,0,0\n" +
                "story,خاله سوسکه,spider.png,,100,0,0\n" +
                "story,خاله پیرزن,old_woman.png,,100,0,0\n" +
                "story,کارخانه شکلات سازی,factory.png,,100,0,0\n" +
                "story,فانوس,lantern.png,,100,0,0\n" +
                "story,کلبه آرامش,cabin.png,,100,0,0\n");
    }

    public boolean setMixedList(String mixedList) {
        boolean changed = !getMixedList().trim().equals(mixedList.trim());
        editor.putString("mixedlist", mixedList);
        editor.commit();
        return changed;
    }

    public String getMixedList() {
        String mixedList = prefs.getString("mixedlist", "m,0,arcticMix,1,ماجرای قطب شمال,covers/arctic_ice_landscape.jpg,1800,سفر به سرزمین یخ\u200Cها\n" +
                "~~\n" +
                "s,باد,wind2,25,0,120,true\n" +
                "s,نهنگ,whale,36,70,110,true\n" +
                "s,جزر و مد متغیر,other_turning_tides,60,0,900,false\n" +
                "s,سرمای قطبی,ai_antarctic_sadness,60,60,900,false \n" +
                "s,رهروی در سرزمین خواب,dg_drifting_in_dreamland,75,60,900,false \n" +
                "s,یک بهار سرد,eamonn_watt_one_cold_spring,70,90,900,false\n" +
                "s,امواج شفاف,Alpha_Lucid_Waves,25,0,1800,true\n" +
                "s,داستان قطب شمال,story_north_pole,100,20,955,false\n" +
                "#\n" +
                "m,1,singleTreeMix,2,تک درخت,covers/single_tree_in_bliss.jpg,1800,دویدن در چمنزار\n" +
                "~~\n" +
                "s,داستان چمنزار,story_grassland,100,10,240,true\n" +
                "s,پرندگان پاییزی,birds_autumn,15,0,50,true\n" +
                "s,بلبل,bulbul,3,0,30,true\n" +
                "s,نیمه شب نیلی,dg_midnight_blue,100,240,900,false \n" +
                "s,پژواک محیطی زمین,chris_anes_ambient_echoes_of_the_earth,35,250,900,false \n" +
                "s,نخستین ستاره در آسمان,dg_first_star_in_sky,30,270,900,false \n" +
                "s,خاطرات,david_tolk_memories,30,290,900,false \n" +
                "s,تمرکز آلفا ۱,Alpha_Focus_107_115Hz,25,0,1200,true\n" +
                "#\n" +
                "m,1,beachMix,3,ساحل آرام,covers/beach_sea_coast_sunset.jpg,1800,ترکیبی آرامش\u200Cبخش از صدای دریا و مرغان دریایی\n" +
                "~~\n" +
                "s,دریای آرام,sea_calm,10,30,0,180,true\n" +
                "s,مرغ دریایی,seagull,10,30,60,false\n" +
                "s,باد,wind,10,20,40,true\n" +
                "s,جاده ابریشم,ai_healing_waves,100,5,1800,false \n" +
                "s,کاروانسرا,ai_healing_light,100,30,1800,false \n" +
                "s,باغ مخفی,dg_gentle_descent,100,50,1800,false \n" +
                "s,امواج شفاف,Alpha_Lucid_Waves,15,0,180,true\n" +
                "#\n" +
                "m,1,darkSeaMix,4,دریای تاریک و طوفانی,covers/dark_thunder_sea.jpg,1800,ترکیبی از دریای طوفانی، رعد و برق و موسیقی کاروان\n" +
                "~~\n" +
                "s,زیر آب,under_water,40,0,10,true\n" +
                "s,نهنگ,whale,100,0,30,true\n" +
                "s,رعد و برق,thunder,80,5,10,false\n" +
                "s,رعد و برق,thunder,70,10,30,false\n" +
                "s,رعد و برق,thunder,80,50,70,false\n" +
                "s,رعد و برق,thunder,70,120,140,false\n" +
                "s,رعد و برق,thunder,70,190,240,false\n" +
                "s,دریای شب,sea_night,70,30,60,true\n" +
                "s,کاروان,dyathon_all_at_sea,100,10,1800,false\n" +
                "s,شب طوفانی,paul_cardall_an_evening_in_paris,90,40,1800,false\n" +
                "s,امواج شفاف,Alpha_Lucid_Waves,25,0,1800,true\n" +
                "#\n" +
                "m,0,forestMix,5,جنگل بارانی,covers/forest_trees_green_nature.jpg,1800,تجربه جنگل در یک روز بارانی با امواج و موسیقی آرامش\u200Cبخش\n" +
                "~~\n" +
                "s,جنگل,forest,5,0,30,true\n" +
                "s,باران,rain,15,0,1800,true\n" +
                "s,پرنده,bird,10,60,400,false\n" +
                "s,پرنده ها,birds,10,40,100,false\n" +
                "s,رعد و برق,thunder,15,120,180,false\n" +
                "s,بهار,dg_twilight_fades,100,20,1800,false\n" +
                "s,رهروی در سرزمین خواب,dg_drifting_in_dreamland,100,40,900,false\n" +
                "s,شب طوفانی,eamonn_watt_one_cold_spring,60,50,1800,false\n" +
                "s,نوازش فرشته\u200Cای,dg_an_angles_caress,70,60,600,false\n" +
                "s,آلفا شفاف,Alpha_Lucid_Waves,15,0,1800,true\n" +
                "s,آلفا مدیتیشن,Alpha_Meditation,15,60,1800,true\n" +
                "s,مانترا آلفا_تتا,Mantra_Alpha_Theta,15,120,1800,true\n" +
                "#\n" +
                "m,0,mountainMix,6,کوهستان مه\u200Cآلود,covers/mountain_peak_fog.jpg,1800,صدای طبیعت بکر کوهستان\n" +
                "~~\n" +
                "s,باد,wind,45,0,1800,true\n" +
                "s,آب کوهستان,water_cascade_down_rock,10,20,1780,true\n" +
                "s,پرنده,bird,25,90,350,false\n" +
                "s,زمستان,paul_cardall_an_evening_in_paris,70,0,1800,false\n" +
                "#\n" +
                "m,0,lakeMix,7,دریاچه آرام,covers/lake_reflection_water.jpg,1800,انعکاس آرامش در آب\u200Cهای دریاچه\n" +
                "~~\n" +
                "s,غروب دریاچه,lake_sunset,55,0,1800,false\n" +
                "s,قورباغه,frog,35,45,180,false\n" +
                "s,جیرجیرک,cricket,30,90,240,true\n" +
                "s,رقص پروانه,other_turning_tides,70,0,1800,false\n" +
                "s,آب روان,dg_water_flow,70,20,300,false\n" +
                "#\n" +
                "m,0,waterfallMix,8,آبشار خروشان,covers/waterfall_river_nature.jpg,1800,انرژی بخش و نشاط آور\n" +
                "~~\n" +
                "s,آبشار,waterfall,65,0,1800,true\n" +
                "sآب کوهستان,water_cascade_down_rock,45,0,1800,true\n" +
                "s,پرنده,bird,30,45,200,false\n" +
                "s,آزادی,dyathon_the_garden_of_words,0,50,0,1800,false\n" +
                "#\n" +
                "m,0,divingStoryMix,9,ماجرای غواصی,covers/scuba_diving_ocean.jpg,1800,سفر به اعماق اقیانوس\n" +
                "~~\n" +
                "s,زیر آب,under_water,40,0,1800,true\n" +
                "s,نهنگ,whale,35,45,120,false\n" +
                "s,چکه آب,dripping,25,30,150,false\n" +
                "s,افق درخشان,david_tolk_pray,70,0,1800,false\n" +
                "#\n" +
                "m,0,boatStoryMix,10,قایق سواری آرام,covers/wooden_boat_lake.jpg,1800,قایق سواری آرامش بخش \n" +
                "~~\n" +
                "s,پارو زدن,oar,8,60,80,true\n" +
                "s,باد ملایم,gentle_wind,25,0,1800,true\n" +
                "s,مرغ دریایی,seagull,10,60,200,false\n" +
                "s,مرغ دریایی,seagull,10,0,15,false\n" +
                "s,مرغ دریایی,seagull,15,40,55,false\n" +
                "s,به کسی که می داند,yanni_to_the_one_who_knows,90,0,1800,false\n" +
                "s,همه در دریا,dyathon_all_at_sea,100,40,1800,true\n" +
                "s,نوازش فرشته\u200Cای,dg_an_angles_caress,100,60,600,false \n" +
                "s,رهروی در سرزمین خواب,dg_drifting_in_dreamland,100,80,900,false \n" +
                "s,زندگی,Living_150_158Hz,15,0,180,true\n" +
                "#\n" +
                "m,0,cabinMix,11,کلبه جنگلی,covers/log_cabin_forest.jpg,1800,پناهگاهی در دل طبیعت\n" +
                "~~\n" +
                "s,آتش هیزم,firewood,50,0,1800,true\n" +
                "s,باران,rain,45,0,1800,true\n" +
                "s,جیرجیرک,cricket,30,60,240,true\n" +
                "s,پناهگاه,chris_anes_ambient_land_of_dreams,50,0,1800,false\n" +
                "#\n" +
                "m,0,lanternMix,12,فانوس جادویی,covers/old_lantern_light.jpg,1800,ماجرای فانوس در شب تاریک\n" +
                "~~\n" +
                "s,باد,wind,45,0,1800,true\n" +
                "s,جیرجیرک,cricket,35,30,1770,true\n" +
                "s,جغد,owl,30,150,200,false\n" +
                "s,شب تاریک,peder_b_helland_my_rose,80,0,1800,false");
        return mixedList;
    }



    public boolean setMessage(String message) {
        boolean changed = !getMessage().trim().equals(message.trim());
        editor.putString("message_html", message);
        editor.commit();
        return changed;
    }

    public String getMessage() {
        String message = prefs.getString("message_html", "");
        return message;
    }

    private void showMessageDialog(String htmlContent) {
        // نمایش در ترد اصلی
        activity.runOnUiThread(() -> {
            MessageDialog dialog = new MessageDialog(activity, htmlContent);
            dialog.show();
        });
    }

    public void ShowWebpage() throws UnsupportedEncodingException {

          RequestQueue queue = Volley.newRequestQueue(activity);
        String url = "https://raw.githubusercontent.com/MortezaMaghrebi/MySoundData/refs/heads/main/webpage.txt";

        // Variable to store the file content
        final String[] fileContent = {""}; // Using array to allow modification in inner class

        StringRequest getRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                           showMessageDialog(response);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {


                    }
                }
        );
        queue.getCache().clear();
        queue.add(getRequest);
    }

}
