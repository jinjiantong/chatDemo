package ui;



import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.crashlytics.android.Crashlytics;
import im.WeChat;

import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.donal.wechat.R;

import tools.AppContext;
import tools.AppManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import config.AppActivity;
import config.CommonValue;


/**
 * wechat
 *
 * @author donal
 *
 */
public class Welcome extends AppActivity{
	
	public static final String KEY_HELP_VERSION_SHOWN = "preferences_help_version_shown";
	 public static final String DIR = Environment.getExternalStorageDirectory()
	            .getAbsolutePath() + "/survey/log/";
	    public static final String NAME = getCurrentDateString() + ".txt";
	 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Crashlytics.start(this);
		final View view = View.inflate(this, R.layout.welcome_page, null);
		setContentView(view);
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		double diagonalPixels = Math.sqrt(Math.pow(dm.widthPixels, 2) + Math.pow(dm.heightPixels, 2));
		double screenSize = diagonalPixels / (160*dm.density);
		appContext.saveScreenSize(screenSize);
		AlphaAnimation aa = new AlphaAnimation(0.3f,1.0f);
		aa.setDuration(2000);
		view.startAnimation(aa);
		aa.setAnimationListener(new AnimationListener()
		{
			public void onAnimationEnd(Animation arg0) {
				//启动动画结束
				redirectTo();
			}
			public void onAnimationRepeat(Animation animation) {}
			public void onAnimationStart(Animation animation) {}
			
		});
		
		Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
	}
	

    private UncaughtExceptionHandler uncaughtExceptionHandler = new UncaughtExceptionHandler() {
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
       // LogUtil.showLog("我崩溃了");
        String info = null;
        ByteArrayOutputStream baos = null;
        PrintStream printStream = null;
        try {
            baos = new ByteArrayOutputStream();
            printStream = new PrintStream(baos);
            ex.printStackTrace(printStream);
            byte[] data = baos.toByteArray();
            info = new String(data);
            data = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (printStream != null) {
                    printStream.close();
                }
                if (baos != null) {
                    baos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        writeErrorLog(info);
//        Intent intent = new Intent(getApplicationContext(),
//                CollapseActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
    }
};

/**
 * 向文件中写入错误信息
 * 
 * @param info
 */
protected void writeErrorLog(String info) {
    File dir = new File(DIR);
    if (!dir.exists()) {
        dir.mkdirs();
    }
    File file = new File(dir, NAME);
    try {
        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
        fileOutputStream.write(info.getBytes());
        fileOutputStream.close();
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    }

}


	 /**
     * 获取当前日期
     * 
     * @return
     */
    private static String getCurrentDateString() {
        String result = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",
                Locale.getDefault());
        Date nowDate = new Date();
        result = sdf.format(nowDate);
        return result;
    }
	private void redirectTo(){   
		
		if(!appContext.isLogin()){
//			if(!showWhatsNewOnFirstLaunch()){
				Intent intent = new Intent(this,Login.class);
				startActivity(intent);
				AppManager.getAppManager().finishActivity(this);
//			}
		}
		else {
			Intent intent = new Intent(this, Tabbar.class);
	        startActivity(intent);
	        AppManager.getAppManager().finishActivity(this);
		}
    }
	
	private boolean showWhatsNewOnFirstLaunch() {
	    try {
		      PackageInfo info = getPackageManager().getPackageInfo(CommonValue.PackageName, 0);
		      int currentVersion = info.versionCode;
		      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		      int lastVersion = prefs.getInt(KEY_HELP_VERSION_SHOWN, 0);
		      if (currentVersion > lastVersion) {
			        prefs.edit().putInt(KEY_HELP_VERSION_SHOWN, currentVersion).commit();
//			        Intent intent = new Intent(this, whatsnew.class);
//			        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//			        startActivity(intent);
//			        finish();
			        return true;
		      	}
	    	} catch (PackageManager.NameNotFoundException e) {
	    		e.printStackTrace();
	    	}
	    return false;
	}
}
