package ind.simsim.maruViewer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by admin on 2016-02-18.
 */
public class ComicsViewer extends Activity {
    private View mCustomView;
    private ActionBar actionBar;
    private ImageButton imageButton;
    private String url;
    private TextView titleView;
    private Intent intent;
    private File file;
    private FileWriter fw;
    private BufferedWriter bw;
    private String path;
    private WebView webView;
    private WebSettings settings;
    private Context context;
    private int dWidth, dHeight;
    private ArrayList<ComicsData> comicsDatas;
    private SwipyRefreshLayout nextComics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comics);

        context = this;
        intent = getIntent();
        url = intent.getStringExtra("url");
        actionBar = getActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        mCustomView = LayoutInflater.from(this).inflate(R.layout.custom_actionbar, null);
        actionBar.setCustomView(mCustomView);

        imageButton = (ImageButton) mCustomView.findViewById(R.id.back);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        titleView = (TextView) mCustomView.findViewById(R.id.title);
        titleView.setSelected(true);

        nextComics = (SwipyRefreshLayout) findViewById(R.id.nextComics);
        nextComics.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        nextComics.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection swipyRefreshLayoutDirection) {
                new Comics().execute();
            }
        });

        comicsDatas = new ArrayList<>();

        final Activity activity = this;
        ImageButton save = (ImageButton) mCustomView.findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ComicsSave().save(activity, url);
            }
        });

        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        dWidth = dm.widthPixels;
        dHeight = dm.heightPixels / 2;

        webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());
        settings = webView.getSettings();
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setLoadWithOverviewMode(true);

        new Comics().execute();

        Tracker t = ((ApplicationController)getApplication()).getTracker(ApplicationController.TrackerName.APP_TRACKER);
        t.setScreenName("ComicsViewer");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    class Comics extends AsyncTask<Void, Void, Void> {
        private Elements image, title, next;
        private StringBuilder html;
        private int size;
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(context);
            dialog.setTitle("Load");
            dialog.setMessage("로딩중..");
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Document document = Jsoup.connect(url).timeout(0).post();
                image = document.select("img");
                title = document.select("title");
                next = document.select("a[class*=next]");

                html = new StringBuilder();
                html.append(getResources().getString(R.string.htmlStart));

                size = image.size();
                int width = 0, height = 0;
                Log.i("elementsSize", size + "");
                Log.i("next", next.html());
                for (int i = 0; i < size; i++) {
                    if (image.get(i).attr("data-src").equals("")) {
                        continue;
                    }
                    try {
                        width = Integer.valueOf(image.get(i).attr("width"));
                        height = Integer.valueOf(image.get(i).attr("height"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        width = dWidth;
                        height = dHeight;
                    }
                    height = (height * dWidth) / width;
                    width = dWidth;
                    Log.i("width", width + "");
                    Log.i("height", height + "");
                    html.append("<img src=").append(image.get(i).attr("data-src")).append(" width=").append(width).append(" height=").append(height).append("/> ");
                }
                html.append(getResources().getString(R.string.htmlEnd));

                document = null;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void mVoid) {
            dialog.dismiss();
            path = getCacheDir() + "/maru.html";
            file = new File(path);
            try {
                fw = new FileWriter(file);
                bw = new BufferedWriter(fw);
                bw.write(html.toString());
                bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String temp = title.get(0).text();
            titleView.setText(temp.substring(0, temp.length() - 12));
            loadComics();
        }
    }

    public void loadComics(){
        webView.loadUrl("file://" + path);
    }

    class Episode extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Document document = Jsoup.connect(url).timeout(0).get();
                Elements content = document.select("div[class=content] a");

                int size = content.size();
                for (int i = 0; i < size; i++) {
                    if (!content.get(i).attr("href").equals("")) {
                        if(!content.get(i).attr("href").contains("http"))
                            break;
                        comicsDatas.add(new ComicsData(content.get(i).text(), content.get(i).attr("href").replace("shencomics","yuncomics")));
                    }
                }

                document = null;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void mVoid) {
            for(int i = 0; i < comicsDatas.size(); i++) {
                if (titleView.getText().toString().equals(comicsDatas.get(i).getTitle())) {
                    url = comicsDatas.get(i).getLink();
                    break;
                }
            }
            new Comics().execute();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            bw.close();
            fw.close();
        }catch (Exception e){
        }
    }
}
