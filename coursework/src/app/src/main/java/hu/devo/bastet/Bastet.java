package hu.devo.bastet;

import android.app.Application;
import android.content.Context;

import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.MaterialModule;
import com.orhanobut.logger.Logger;
import com.raizlabs.android.dbflow.config.FlowManager;

/**
 * This needs to be created to initialize some libraries.
 * Created by Barnabas on 15/11/2015.
 */
public class Bastet extends Application {


    private static Application app;

    public static Application getApplication() {
        return app;
    }

    public static Context getContext() {
        return getApplication().getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        Iconify.with(new MaterialModule());

        Logger.init("BASTET")
                .methodCount(1);

        FlowManager.init(this);
    }
}
