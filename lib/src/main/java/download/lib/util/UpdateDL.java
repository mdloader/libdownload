package download.lib.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.yausername.youtubedl_android.YoutubeDL;

import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class UpdateDL {

    private static boolean updating =  false ;
    private static final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private static String TAG="UpateDL";

    private static void updateDL(Context context, YoutubeDL.UpdateChannel updateChannel) {
        if (updating) {
            return;
        }

        updating = true;
        // progressBar.setVisibility(View.VISIBLE);
        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().updateYoutubeDL(context, updateChannel))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(status -> {
                    // progressBar.setVisibility(View.GONE);
                    switch (status) {
                        case DONE:
                            ToastUtil.showCustomToast(context, "Update successful " + YoutubeDL.getInstance().versionName(context), true) ;
                            break;
                        case ALREADY_UP_TO_DATE:
                            ToastUtil.showCustomToast(context, "Already up to date " + YoutubeDL.getInstance().versionName(context), true) ;
                           break;
                        default:
                            ToastUtil.showCustomToast(context, status.toString(), true) ;
   break;
                    }
                    updating = false;
                }, e -> {
                    Log.e(TAG, "failed to update", e);
                    //  progressBar.setVisibility(View.GONE);
                    //   Toast.makeText(MainActivity.this, "update failed", Toast.LENGTH_LONG).show();
                    updating = false;
                });
        compositeDisposable.add(disposable);
    }


}
