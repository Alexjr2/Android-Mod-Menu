package com.android.support;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

@SuppressWarnings("all")
public class Main {
    public static final String MODMENU = "libmodmenu";

    public static String libmodmenu() {
        String arch = System.getProperty("os.arch");
        if (arch != null && (arch.contains("aarch64") || arch.contains("x86_64"))) {
            return "libmodmenu64.so";
        } else {
            return "libmodmenu32.so";
        }
    }

    public static void loadLibFromAssets(Context context) throws RuntimeException, IOException {

        if (context == null) {
            return;
        }

        String dummyLib = libmodmenu();

        AssetManager assetManager = context.getAssets();
        String[] list = assetManager.list(MODMENU);
        
        if (list == null) {
            throw new RuntimeException("Asset list is null");
        }

        String listToString = Arrays.toString(list);

        if (!listToString.contains(dummyLib)) {
            throw new RuntimeException("Unable to locate file");
        } else {
            try (InputStream is = assetManager.open(MODMENU.concat("/").concat(dummyLib));
                 OutputStream os = new FileOutputStream(new File(context.getDataDir().getAbsolutePath(), dummyLib))) {
                transfer(is, os);
                doLoad(new File(context.getDataDir().getAbsolutePath(), dummyLib));
            } catch (Exception exception) {
                Log.e("EXR", "loadLibFromAssets: ", exception);
                throw new RuntimeException("Crappy crap happened");
            }
        }
    }

    private static native void CheckOverlayPermission(Context context);

    private static Menu menu;
  
    public static void StartWithoutPermission(Context context) {
        CrashHandler.init(context, true);
        if (context instanceof Activity) {
            //Check if context is an Activity.
            menu = new Menu(context);
            menu.SetWindowManagerActivity();
            menu.ShowMenu();
        } else {
            //Anything else, ask for permission
            CheckOverlayPermission(context);
        }
    }

    public static void Start(Context context) {
        try {
            loadLibFromAssets(context);
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
        CrashHandler.init(context, false);
        CheckOverlayPermission(context);
    }
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    private static void doLoad(File outFile) {
        try {
            System.load(outFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void transfer(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[8192];
        int n;
        while ((n = is.read(buffer)) != -1) {
            os.write(buffer, 0, n);
        }
    }
}
