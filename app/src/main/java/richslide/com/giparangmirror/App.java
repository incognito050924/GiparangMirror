package richslide.com.giparangmirror;

import android.app.Application;

import com.tsengvn.typekit.Typekit;

/**
 * Created by 장윤석 on 2017-12-19.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Typekit.getInstance().addNormal(Typekit.createFromAsset(this,"font/NanumSquareBold.ttf"));
    }
}
