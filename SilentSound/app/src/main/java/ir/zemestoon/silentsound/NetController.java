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
    Context context;
    final String MY_PREFS_NAME = "PREFS_SILENT_SOUND";
    SharedPreferences.Editor editor;
    SharedPreferences prefs;

    public static  NetController instance;
    public static synchronized NetController getInstance(Context context) {
        if (instance == null) {
            instance = new NetController(context);
        }
        return instance;
    }
    public NetController(Context context) {
        this.context = context;
        editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
    }

    public  void  DownloadSoundList()  throws UnsupportedEncodingException {

        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://raw.githubusercontent.com/MortezaMaghrebi/SilentSound/refs/heads/main/soundlist.txt";

        // Variable to store the file content
        final String[] fileContent = {""}; // Using array to allow modification in inner class

        StringRequest getRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        int responselen = response.trim().length();
                        int sllen = getSoundList().trim().length();
                        if(!response.trim().equals(getSoundList().trim())) {
                            setSoundList(response.trim());
                            Toast.makeText(context,"لیست صداها آپدیت شد",Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context,"برای دریافت لیست صداها به اینترنت متصل شوید",Toast.LENGTH_SHORT).show();
                    }
                }
        );
        queue.getCache().clear();
        queue.add(getRequest);
    }

    public  void  DownloadMixedList()  throws UnsupportedEncodingException {

        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://raw.githubusercontent.com/MortezaMaghrebi/SilentSound/refs/heads/main/mixedlist.txt";

        // Variable to store the file content
        final String[] fileContent = {""}; // Using array to allow modification in inner class

        StringRequest getRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        int responselen = response.trim().length();
                        int sllen = getMixedList().trim().length();
                        if(!response.trim().equals(getMixedList().trim())) {
                            setMixedList(response.trim());
                            Toast.makeText(context,"لیست میکس ها آپدیت شد",Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context,"برای دریافت لیست میکس ها به اینترنت متصل شوید",Toast.LENGTH_SHORT).show();
                    }
                }
        );
        queue.getCache().clear();
        queue.add(getRequest);
    }

    public void setSoundList(String soundList)
    {
        editor.putString("soundlist",soundList);
        editor.commit();
    }

    public String getSoundList()
    {
        String soudnlist = prefs.getString("soundlist","");
        return prefs.getString("soundlist","nature,پرنده,bird.png,nature/bird.mp3,10,0,0\n" +
                "nature,پرنده ها,hummingbird.png,nature/birds.mp3,10,0,0\n" +
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
                "music,نوازش فرشته ای,sleep.png,music/dan_gibson/dg_an_angles_caress.mp3,70,0,0\n" +
                "music,رهروی در سرزمین خواب,sleep.png,music/dan_gibson/dg_drifting_in_dreamland.mp3,70,0,0\n" +
                "music,نخستین ستاره افق,sleep.png,music/dan_gibson/dg_first_star_in_sky.mp3,70,0,0\n" +
                "music,فرود آرام,sleep.png,music/dan_gibson/dg_gentle_descent.mp3,70,0,0\n" +
                "music,نیمه شب نیلی,sleep.png,music/dan_gibson/dg_midnight_blue.mp3,70,0,0\n" +
                "music,آرام و آسوده,sleep.png,music/dan_gibson/dg_safe_and_sound.mp3,70,0,0\n" +
                "music,سایه روشن های شفق,sleep.png,music/dan_gibson/dg_twilight_fades.mp3,70,0,0\n" +
                "music,جهانی دور,sleep.png,music/dan_gibson/dg_worlds_away.mp3,70,0,0\n" +
                "music,کاروانسرا,k.png,music/kitaro/kitaro_caravansary.mp3,70,0,0\n" +
                "music,آکوا,water.png,music/kitaro/kitaro_aqua.mp3,70,0,0\n" +
                "music,عاشقانه,k.png,music/kitaro/kitaro_romance.mp3,70,0,0\n" +
                "music,افق درخشان,sun.png,music/kitaro/kitaro_shimmering_horizon.mp3,70,0,0\n" +
                "music,روح دریاچه,lake.png,music/kitaro/kitaro_spirit_of_the_west_lake.mp3,70,0,0\n" +
                "music,فال نیک,lucky.png,music/kitaro/kitaro_auspicious_omen.mp3,70,0,0\n" +
                "music,کاروان,caravan.png,music/kitaro/kitaro_caravan.mp3,70,0,0\n" +
                "music,دکتر سان و چینگ لینگ,doctor.png,music/kitaro/kitaro_dr_sun_and_ching_ling.mp3,70,0,0\n" +
                "music,کوی,carp.png,music/kitaro/kitaro_koi.mp3,70,0,0\n" +
                "music,زیارت,pilgrimage.png,music/kitaro/kitaro_pilgrimage.mp3,70,0,0\n" +
                "music,فواره مقدس,fountain.png,music/kitaro/kitaro_sacred_fountain.mp3,70,0,0\n" +
                "music,جاده ابریشم,road.png,music/kitaro/kitaro_silk_road.mp3,70,0,0\n" +
                "music,خواهران سونگ,sisters.png,music/kitaro/kitaro_soong_sisters.mp3,70,0,0\n" +
                "music,آداجیو,garden.png,music/secret_garden/sg_adagio.mp3,70,0,0\n" +
                "music,همیشه آنجا,pin.png,music/secret_garden/sg_always_there.mp3,70,0,0\n" +
                "music,آپاسیوناتا,garden.png,music/secret_garden/sg_apassionata.mp3,70,0,0\n" +
                "music,کانتولونا,moon.png,music/secret_garden/sg_cantoluna.mp3,70,0,0\n" +
                "music,شانونه,garden.png,music/secret_garden/sg_chanonne.mp3,70,0,0\n" +
                "music,رویاگیر,dreamcatcher.png,music/secret_garden/sg_dreamcatcher.mp3,70,0,0\n" +
                "music,رویای تو,swing.png,music/secret_garden/sg_dreamed_of_you.mp3,70,0,0\n" +
                "music,دوئت,garden.png,music/secret_garden/sg_duo.mp3,70,0,0\n" +
                "music,یخ\u200Cزده در زمان,winter.png,music/secret_garden/sg_fozen_in_time.mp3,70,0,0\n" +
                "music,موج\u200Cهای سبز,deezer.png,music/secret_garden/sg_greenwaves.mp3,70,0,0\n" +
                "music,سرود امید,lullaby.png,music/secret_garden/sg_hymn_to_hope.mp3,70,0,0\n" +
                "music,لوتوس,lotus.png,music/secret_garden/sg_lotus.mp3,70,0,0\n" +
                "music,شب تاریک,midnight.png,music/secret_garden/sg_morketid.mp3,70,0,0\n" +
                "music,حرکت,dancing.png,music/secret_garden/sg_moving.mp3,70,0,0\n" +
                "music,قرن جدید,clock.png,music/secret_garden/sg_new_century.mp3,70,0,0\n" +
                "music,نوكتورن,garden.png,music/secret_garden/sg_nocturn.mp3,70,0,0\n" +
                "music,سادگی,garden.png,music/secret_garden/sg_ode_to_simplicity.mp3,70,0,0\n" +
                "music,پروانه,butterfly.png,music/secret_garden/sg_papillon.mp3,70,0,0\n" +
                "music,پاساكالیا,garden.png,music/secret_garden/sg_passacaglia.mp3,70,0,0\n" +
                "music,پاستورال,garden.png,music/secret_garden/sg_pastorale.mp3,70,0,0\n" +
                "music,شب مقدس,owl.png,music/secret_garden/sg_sacred_night.mp3,70,0,0\n" +
                "music,پناهگاه,tent.png,music/secret_garden/sg_sanctuary.mp3,70,0,0\n" +
                "music,باغ مخفی,garden.png,music/secret_garden/sg_secret_garden.mp3,70,0,0\n" +
                "music,سرناد,garden.png,music/secret_garden/sg_serenade.mp3,70,0,0\n" +
                "music,سیگما,garden.png,music/secret_garden/sg_sigma.mp3,70,0,0\n" +
                "music,آهنگ خواب,sleep.png,music/secret_garden/sg_sleepsong.mp3,70,0,0\n" +
                "music,سونا,garden.png,music/secret_garden/sg_sona.mp3,70,0,0\n" +
                "music,شب طوفانی,storm.png,music/secret_garden/sg_stormy_night.mp3,70,0,0\n" +
                "music,رویا,unicorn.png,music/secret_garden/sg_the_dream.mp3,70,0,0\n" +
                "music,بی\u200Cخیالی,easy.png,music/secret_garden/sg_without_care.mp3,70,0,0\n" +
                "music,سفر,music.png,music/secret_garden/sg_voyage.mp3,70,0,0\n" +
                "music,پاییز,autumn.png,music/brian_crain/brian_autumn.mp3,70,0,0\n" +
                "music,زمین,globe.png,music/brian_crain/brian_earth.mp3,70,0,0\n" +
                "music,آتش,campfire.png,music/brian_crain/brian_fire.mp3,70,0,0\n" +
                "music,یخ,icy.png,music/brian_crain/brian_ice.mp3,70,0,0\n" +
                "music,باران,rain.png,music/brian_crain/brian_rain.mp3,70,0,0\n" +
                "music,برف,snow.png,music/brian_crain/brian_snow.mp3,70,0,0\n" +
                "music,بهار,spring.png,music/brian_crain/brian_spring.mp3,70,0,0\n" +
                "music,تابستان,sun.png,music/brian_crain/brian_summer.mp3,70,0,0\n" +
                "music,آب,water.png,music/brian_crain/brian_water.mp3,70,0,0\n" +
                "music,باد,wind.png,music/brian_crain/brian_wind.mp3,70,0,0\n" +
                "music,زمستان,winter.png,music/brian_crain/brian_winter.mp3,70,0,0\n" +
                "music,انتظار,hourglass.png,music/cheshmazar/awaiting.mp3,70,0,0\n" +
                "music,آزادی,freedom.png,music/cheshmazar/freedom.mp3,70,0,0\n" +
                "music,عشق پرشور,music.png,music/cheshmazar/passion_of_love.mp3,70,0,0\n" +
                "music,عشق پرشور ۲,music.png,music/cheshmazar/passion_of_love_ii.mp3,70,0,0\n" +
                "music,باران عشق,rain.png,music/cheshmazar/rain_of_love.mp3,70,0,0\n" +
                "music,خیزش,sunrise.png,music/cheshmazar/rising.mp3,70,0,0\n" +
                "music,خواب,sleep.png,music/cheshmazar/sleep.mp3,70,0,0\n" +
                "music,دیدار,handshake.png,music/cheshmazar/visit.mp3,70,0,0\n" +
                "music,رقص پروانه,butterfly.png,music/yanni/yanni_butterfly_dance.mp3,70,0,0\n" +
                "music,فلیتسا,music.png,music/yanni/yanni_felitsa.mp3,70,0,0\n" +
                "music,در آینه,mirror.png,music/yanni/yanni_in_the_mirror.mp3,70,0,0\n" +
                "music,فقط یک خاطره,alzheimer.png,music/yanni/yanni_only_a_memory.mp3,70,0,0\n" +
                "music,سوگندهای مخفی,promise.png,music/yanni/yanni_secret_vows.mp3,70,0,0\n" +
                "music,دوست قدیمی,children.png,music/yanni/yanni_so_long_my_friend.mp3,70,0,0\n" +
                "music,به کسی که می\u200Cداند,reading.png,music/yanni/yanni_to_the_one_who_knows.mp3,70,0,0\n" +
                "music,نور شفابخش,sun.png,music/ai/ai_healing_light.mp3,70,0,0\n" +
                "music,امواج شفابخش,waves.png,music/ai/ai_healing_waves.mp3,70,0,0\n" +
                "music,پژواک محیطی زمین,globe.png,music/chris_anes/chris_anes_ambient_echoes_of_the_earth.mp3,70,0,0\n" +
                "music,سرزمین رویاها,dream.png,music/chris_anes/chris_anes_ambient_land_of_dreams.mp3,70,0,0\n" +
                "music,خاطرات,memories.png,music/david_tolk/david_tolk_memories.mp3,70,0,0\n" +
                "music,دعا,pray.png,music/david_tolk/david_tolk_pray.mp3,70,0,0\n" +
                "music,پس از نیمه شب,midnight.png,music/dyathon/dyathon_after_midnight.mp3,70,0,0\n" +
                "music,همه در دریا,sea.png,music/dyathon/dyathon_all_at_sea.mp3,70,0,0\n" +
                "music,باغ کلمات,garden.png,music/dyathon/dyathon_the_garden_of_words.mp3,70,0,0\n" +
                "music,یک بهار سرد,winter.png,music/eamonn_watt/eamonn_watt_one_cold_spring.mp3,70,0,0\n" +
                "music,شبی در پاریس,eiffel-tower.png,music/paul_cardall/paul_cardall_an_evening_in_paris.mp3,70,0,0\n" +
                "music,گل سرخ من,rose.png,music/peder_b_helland/peder_b_helland_my_rose.mp3,70,0,0\n" +
                "music,عمق جنگل,forest.png,music/other/other_deep_in_the_forest.mp3,70,0,0\n" +
                "music,جزر و مد متغیر,tides.png,music/other/other_turning_tides.mp3,70,0,0\n" +
                "noise,نویز قهوه\u200Cای,music.png,noise/brown_noise.mp3,30,0,0\n" +
                "noise,نویز سفید,music.png,noise/white_noise.mp3,30,0,0\n" +
                "wave,آداجیو آلفا,alpha.png,wave/Adagio_Alpha_105-115Hz.mp3.mp3,20,0,0\n" +
                "wave,آلفا سعادت,alpha.png,wave/Alpha_Bliss_107-115Hz.mp3.mp3,20,0,0\n" +
                "wave,امواج آلفا,alpha.png,wave/Alpha_Brain_Waves.mp3.mp3,20,0,0\n" +
                "wave,تمرکز آلفا ۱,alpha.png,wave/Alpha_Focus_107-115Hz.mp3.mp3,20,0,0\n" +
                "wave,تمرکز آلفا ۲,alpha.png,wave/Alpha_Focus_127-135Hz.mp3.mp3,20,0,0\n" +
                "wave,تمرکز آلفا ۳,alpha.png,wave/Alpha_Focus_97-104Hz.mp3.mp3,20,0,0\n" +
                "wave,آلفا اینرورس,alpha.png,wave/Alpha_Innerverse_Reso.mp3.mp3,20,0,0\n" +
                "wave,امواج شفاف,alpha.png,wave/Alpha_Lucid_Waves.mp3.mp3,20,0,0\n" +
                "wave,مدیتیشن آلفا,alpha.png,wave/Alpha_Meditation.mp3.mp3,20,0,0\n" +
                "wave,شب آلفا,alpha.png,wave/Alpha_Night_106-114Hz.mp3.mp3,20,0,0\n" +
                "wave,مسیر آلفا,path.png,wave/Alpha_Path_96-105Hz.mp3.mp3,20,0,0\n" +
                "wave,رفاه آلفا,alpha.png,wave/Alpha_Prosperity_127-135Hz.mp3.mp3,20,0,0\n" +
                "wave,شانت آلفا,alpha.png,wave/Alpha_Shaant_74-82Hz.mp3.mp3,20,0,0\n" +
                "wave,سینوس آلفا ۱,alpha.png,wave/Alpha_Sinus_54.8-57.3Hz.mp3.mp3,20,0,0\n" +
                "wave,سینوس آلفا ۲,alpha.png,wave/Alpha_Sinus_62.5-66Hz.mp3.mp3,20,0,0\n" +
                "wave,سینوس آلفا ۳,alpha.png,wave/Alpha_Sinus_88-94Hz.mp3.mp3,20,0,0\n" +
                "wave,سینوس آلفا ۴,alpha.png,wave/Alpha_Sinus_91-101Hz.mp3.mp3,20,0,0\n" +
                "wave,روح آلفا,alpha.png,wave/Alpha_Soul_110-117Hz.mp3.mp3,20,0,0\n" +
                "wave,کره آلفا,alpha.png,wave/Alpha_Sphere_10Hz.mp3.mp3,20,0,0\n" +
                "wave,ترانسند آلفا,alpha.png,wave/Alpha_Transcend_106-114Hz.mp3.mp3,20,0,0\n" +
                "wave,یونیورسال آلفا,alpha.png,wave/Alpha_Universal_65-73Hz.mp3.mp3,20,0,0\n" +
                "wave,امواج آلفا ۸۸,alpha.png,wave/Alpha_Waves_88-96Hz.mp3.mp3,20,0,0\n" +
                "wave,زون آلفا,alpha.png,wave/Alpha_Zone_93-104Hz.mp3.mp3,20,0,0\n" +
                "wave,سینوس بتا,beta.png,wave/Beta_Sinus_100-114Hz.mp3.mp3,20,0,0\n" +
                "wave,امواج بتا,beta.png,wave/Beta_Waves_110-130Hz.mp3.mp3,20,0,0\n" +
                "wave,چرخش دلتا,d.png,wave/Delta_Revolve_125-128Hz.mp3.mp3,20,0,0\n" +
                "wave,اکریورم,beta.png,wave/Ecriurem_100-108Hz.mp3.mp3,20,0,0\n" +
                "wave,تعادل,balance.png,wave/Equilibrium_96-104Hz.mp3.mp3,20,0,0\n" +
                "wave,فلو آلفا,alpha.png,wave/Flow_Alpha_203-211Hz.mp3.mp3,20,0,0\n" +
                "wave,حافظه گاما,gamma.png,wave/Gamma_Memory_Training.mp3.mp3,20,0,0\n" +
                "wave,سینوس گاما ۱,gamma.png,wave/Gamma_Sinus_100-140Hz.mp3.mp3,20,0,0\n" +
                "wave,سینوس گاما ۲,gamma.png,wave/Gamma_Sinus_300-350Hz.mp3.mp3,20,0,0\n" +
                "wave,امواج گاما ۸۶,gamma.png,wave/Gamma_Waves_86+89Hz.mp3.mp3,20,0,0\n" +
                "wave,گاما ویلو,gamma.png,wave/Gamma_Willow_29-71Hz.mp3.mp3,20,0,0\n" +
                "wave,مطالعه داخلی,alpha.png,wave/Inner_Study_110-115Hz.mp3.mp3,20,0,0\n" +
                "wave,زندگی,alpha.png,wave/Living_150-158Hz.mp3.mp3,20,0,0\n" +
                "wave,لوز آلفا,alpha.png,wave/Luz_Alpha_100-108Hz.mp3.mp3,20,0,0\n" +
                "wave,مانترا آلفا-تتا,t.png,wave/Mantra_Alpha-Theta.mp3.mp3,20,0,0\n" +
                "wave,فولیا تتا,t.png,wave/Theta_Follia_41-45Hz.mp3.mp3,20,0,0\n" +
                "wave,رم تتا,t.png,wave/Theta_Rem_60-66Hz.mp3.mp3,20,0,0\n" +
                "wave,راهب آب,water.png,wave/Water_Monk.mp3.mp3,20,0,0\n" +
                "story,داستان چمنزار,tree.png,story/story_grassland.mp3,100,0,0\n" +
                "story,داستان قطب شمال,north.png,story/story_north_pole.mp3,100,0,0\n" +
                "story,داستان غواصی,cliff.png,,100,0,0\n" +
                "story,داستان قایق سواری,music.png,,100,0,0\n" +
                "story,داستان دریاچه,lake.png,,100,0,0\n" +
                "story,داستان بزغاله,music.png,,100,0,0\n" +
                "story,داستان کفش آهنی,iron.png,,100,0,0\n" +
                "story,خاله سوسکه,spider.png,,100,0,0\n" +
                "story,خاله پیرزن,old-woman.png,,100,0,0\n" +
                "story,کارخانه شکلات سازی,factory.png,,100,0,0\n" +
                "story,فانوس,lantern.png,,100,0,0\n" +
                "story,کلبه آرامش,cabin.png,,100,0,0\n" +
                "    ");
    }

    public void setMixedList(String mixedList)
    {
        editor.putString("mixedlist",mixedList);
        editor.commit();
    }

    public String getMixedList()
    {
        String mixedList = prefs.getString("mixedlist","");
        return prefs.getString("mixedlist","");
    }
}
