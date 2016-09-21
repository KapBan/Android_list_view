package com.example.forwork;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity{

    public final static int SIZE=30;

    public String[] titles = new String[SIZE];          //array for titles
    public String[] keys = new String[SIZE];            //array for id of news
    public String[] image_url = new String[SIZE];       //array for image urls to download
    public String data="";
    //that's for parsing
    JSONObject news = null;
    String title = "";
    JSONObject prefs = null;
    JSONObject inner = null;
    JSONObject elements = null;
    JSONObject image =null;
    String pic = "";
    JSONObject json = null;
    JSONArray collection = null;
    JSONObject documents = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView my_list_view = (ListView) findViewById(R.id.list_view);    //custom list view
        CustomList adapter = new CustomList(this,titles,image_url);         //custom adapter
        my_list_view.setAdapter(adapter);
        Parse p = new Parse();                                              //instance of my async task for getting json
        if (isInternetThere(this))                                          //if internet is not absent
            try {
                data = p.execute().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        else  Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();;

        try {
            json = new JSONObject(data);                                    //parsing json
            collection = json.getJSONArray("collection");                   //new's ids
            documents = json.getJSONObject("documents");                    //new's info
            for (int i=0; i<keys.length;i++){
                keys[i]=collection.getString(i);
                news = documents.getJSONObject(keys[i]);
                title = news.getString("title");                            //title
                titles[i]=title;                                            //put title into list
                prefs = news.getJSONObject("prefs");
                inner = prefs.getJSONObject("inner");
                elements = inner.getJSONObject("elements");
                image = elements.getJSONObject("image");
                if (image.has("small_url"))                                 //whether there is a picture?
                    pic = image.getString("small_url");
                else pic = "";
                image_url[i] = pic;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {           //it is just for orientation monitoring
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }

    public class Parse extends AsyncTask<Void,Void,String> {                //my async task #1 that just get a json from url

        @Override
        protected String doInBackground(Void... params) {
            String str= null;
            try {
                str = readJSON();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return str;
        }

        public String readJSON() {                                          //function that get json
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String resultJson = "";                                         //this will be a string with json
            try {
                URL url = new URL("https://meduza.io/api/v3/search?chrono=news&page=0&per_page=30&locale=ru");      //url with 30 news

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();       //stream for getting json
                StringBuffer buffer = new StringBuffer();                       //buffer for json saving

                reader = new BufferedReader(new InputStreamReader(inputStream));        //it just reads

                String line;
                while ((line = reader.readLine()) != null) {                            //line by line..
                    buffer.append(line);                                                //...and add it into buffer
                }

                resultJson = buffer.toString();                                 //result is string got from buffer

            } catch (Exception e) {
                e.printStackTrace();
            }
            return resultJson;
        }
    }

    public class DownloadImage extends AsyncTask<String,Void,Bitmap>{               //my async task #2 for downloading image from url using Bitmap
        private final WeakReference<ImageView> imgv;                                //it means that..
        public DownloadImage(ImageView imageView){
            imgv = new WeakReference<ImageView>(imageView);                        //the object can be deleted by garbage collector without any trubles
        }                                                                          //it helps to avoid memory leaks

        @Override
        protected Bitmap doInBackground(String... strings) {
            return down_Bit(strings[0]);
        }

        @Override
        protected void onPostExecute(Bitmap res){
            if (isCancelled()){
                res=null;
            }

            if(imgv != null){                                                       //if the link is actual...
                ImageView iv = imgv.get();                                          //we get it
                if (iv != null){                                                    //if the object is exists...
                    if (res != null)                                                //and image exests...
                        iv.setImageBitmap(res);                                     //we show it

                }
            }
        }

        private Bitmap down_Bit(String url){                                        //it is for downloading images
            HttpURLConnection urlConnection = null;
            try{
                URL uri = new URL(url);
                urlConnection = (HttpURLConnection) uri.openConnection();
                int statusCode = urlConnection.getResponseCode();
                if (statusCode != HttpStatus.SC_OK)                                 //if our url is not right(for ex. "http" instead of "https"
                    return null;

                InputStream is = urlConnection.getInputStream();
                if (is != null){
                    Bitmap bitmap = BitmapFactory.decodeStream(is);                 //if input stream exists we get the Bitmap from it....
                    return bitmap;                                                  //and return it
                }
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return null;
        }
    }

    public class CustomList extends ArrayAdapter<String>{                           //that is my custom adapter named CustomList. probably for not misunderstanding
        private Activity context;
        private String[] item_name;
        private String[] imgurl;

        public CustomList(Activity context,String[] item_name, String[] imgurl){        //constructor
            super(context,R.layout.my_listview_item,item_name);
            this.context=context;               //activity
            this.item_name=item_name;           //there is will be the titles
            this.imgurl=imgurl;                 //there is will be the images
        }

        public View getView(int position, View view, ViewGroup parent) {
            LayoutInflater inflater = context.getLayoutInflater();
            View rowView = inflater.inflate(R.layout.my_listview_item,null,true);

            TextView txtTitle = (TextView) rowView.findViewById(R.id.txt);
            if (imgurl[position] != "") {                                               //if there is a url of an image(because there some news without it)
                rowView.findViewById(R.id.img).setVisibility(View.VISIBLE);             //show the picture
                imgurl[position] = "https://meduza.io" + imgurl[position];              //and make the url absolute, not relative
                new DownloadImage((ImageView) rowView.findViewById(R.id.img)).execute(imgurl[position]);        //and download the picture using the absolute url
            }
            else
                rowView.findViewById(R.id.img).setVisibility(View.GONE);                //if there is no picture, just hide it
            txtTitle.setText(item_name[position]);                                      //it is title

            return rowView;
        }
    }

    public boolean isInternetThere(Context context) {                                   //it is for internet connection testing
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }




}
