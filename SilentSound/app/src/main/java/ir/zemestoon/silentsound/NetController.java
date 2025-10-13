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
                        setSoundList(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context,"No Internet!",Toast.LENGTH_SHORT).show();
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
        return prefs.getString("soundlist","");
    }
}
