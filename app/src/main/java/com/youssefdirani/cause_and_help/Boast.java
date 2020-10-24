package com.youssefdirani.cause_and_help;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import androidx.annotation.Nullable;

/**
 * {@link Toast} decorator allowing for easy cancellation of notifications. Use this class if you
 * want subsequent Toast notifications to overwrite current ones. </p>
 * <p/>
 * By default, a current {@link Boast} notification will be cancelled by a subsequent notification.
 * This default behaviour can be changed by calling certain methods like {@link #show(boolean)}.
 */
public class Boast {
    /**
     * Keeps track of certain Boast notifications that may need to be cancelled. This functionality
     * is only offered by some of the methods in this class.
     * <p>
     * Uses a {@link WeakReference} to avoid leaking the activity context used to show the original {@link Toast}.
     */
    @Nullable
    private volatile static WeakReference<Boast> weakBoast = null;

    @Nullable
    private static Boast getGlobalBoast() {
        if (weakBoast == null) {
            return null;
        }

        return weakBoast.get();
    }

    private static void setGlobalBoast(@Nullable Boast globalBoast) {
        Boast.weakBoast = new WeakReference<>(globalBoast);
    }


    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Internal reference to the {@link Toast} object that will be displayed.
     */
    private Toast internalToast;

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Private constructor creates a new {@link Boast} from a given {@link Toast}.
     *
     * @throws NullPointerException if the parameter is <code>null</code>.
     */
    private Boast(Toast toast) {
        // null check
        if (toast == null) {
            throw new NullPointerException("Boast.Boast(Toast) requires a non-null parameter.");
        }

        internalToast = toast;
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Make a standard {@link Boast} that just contains a text view.
     *
     * @param context  The context to use. Usually your {@link android.app.Application} or
     *                 {@link android.app.Activity} object.
     * @param text     The text to show. Can be formatted text.
     * @param duration How long to display the message. Either {@link Toast#LENGTH_SHORT} or
     *                 {@link Toast#LENGTH_LONG}
     */
    @SuppressLint("ShowToast")
    public static Boast makeText(Context context, CharSequence text, int duration) {
        Toast toast = Toast.makeText(context, text, duration);
        //toast.setGravity(Gravity.BOTTOM|Gravity.CENTER,0,200);
        toast.setGravity(Gravity.CENTER,0,0);
        Log.i("Youssef", "toast is in center");
        return new Boast(toast);
    }

    public static Boast makeTextUp(Context context, CharSequence text, int duration) {
        Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(Gravity.TOP|Gravity.RIGHT,0,200);
        Log.i("Youssef", "toast is in up");
        return new Boast(toast);
    }

    public static Boast makeTextBottom(Context context, CharSequence text, int duration) {
        Toast toast = Toast.makeText(context, text, duration);
        Log.i("Youssef", "toast is in bottom");
        toast.setGravity(Gravity.BOTTOM,0,200);
        return new Boast(toast);
    }

    /**
     * Make a standard {@link Boast} that just contains a text view with the text from a resource.
     *
     * @param context  The context to use. Usually your {@link android.app.Application} or
     *                 {@link android.app.Activity} object.
     * @param resId    The resource id of the string resource to use. Can be formatted text.
     * @param duration How long to display the message. Either {@link Toast#LENGTH_SHORT} or
     *                 {@link Toast#LENGTH_LONG}
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    @SuppressLint("ShowToast")
    public static Boast makeText(Context context, int resId, int duration)
            throws Resources.NotFoundException {
        return new Boast(Toast.makeText(context, resId, duration));
    }

    /**
     * Make a standard {@link Boast} that just contains a text view. Duration defaults to
     * {@link Toast#LENGTH_SHORT}.
     *
     * @param context The context to use. Usually your {@link android.app.Application} or
     *                {@link android.app.Activity} object.
     * @param text    The text to show. Can be formatted text.
     */
    @SuppressLint("ShowToast")
    public static Boast makeText(Context context, CharSequence text) {
        return new Boast(Toast.makeText(context, text, Toast.LENGTH_SHORT));
    }

    /**
     * Make a standard {@link Boast} that just contains a text view with the text from a resource.
     * Duration defaults to {@link Toast#LENGTH_SHORT}.
     *
     * @param context The context to use. Usually your {@link android.app.Application} or
     *                {@link android.app.Activity} object.
     * @param resId   The resource id of the string resource to use. Can be formatted text.
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    @SuppressLint("ShowToast")
    public static Boast makeText(Context context, int resId) throws Resources.NotFoundException {
        return new Boast(Toast.makeText(context, resId, Toast.LENGTH_SHORT));
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Show a standard {@link Boast} that just contains a text view.
     *
     * @param context  The context to use. Usually your {@link android.app.Application} or
     *                 {@link android.app.Activity} object.
     * @param text     The text to show. Can be formatted text.
     * @param duration How long to display the message. Either {@link Toast#LENGTH_SHORT} or
     *                 {@link Toast#LENGTH_LONG}
     */
    public static void showText(Context context, CharSequence text, int duration) {
        Boast.makeText(context, text, duration).show();
    }

    /**
     * Show a standard {@link Boast} that just contains a text view with the text from a resource.
     *
     * @param context  The context to use. Usually your {@link android.app.Application} or
     *                 {@link android.app.Activity} object.
     * @param resId    The resource id of the string resource to use. Can be formatted text.
     * @param duration How long to display the message. Either {@link Toast#LENGTH_SHORT} or
     *                 {@link Toast#LENGTH_LONG}
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    public static void showText(Context context, int resId, int duration)
            throws Resources.NotFoundException {
        Boast.makeText(context, resId, duration).show();
    }

    /**
     * Show a standard {@link Boast} that just contains a text view. Duration defaults to
     * {@link Toast#LENGTH_SHORT}.
     *
     * @param context The context to use. Usually your {@link android.app.Application} or
     *                {@link android.app.Activity} object.
     * @param text    The text to show. Can be formatted text.
     */
    public static void showText(Context context, CharSequence text) {
        Boast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show a standard {@link Boast} that just contains a text view with the text from a resource.
     * Duration defaults to {@link Toast#LENGTH_SHORT}.
     *
     * @param context The context to use. Usually your {@link android.app.Application} or
     *                {@link android.app.Activity} object.
     * @param resId   The resource id of the string resource to use. Can be formatted text.
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    public static void showText(Context context, int resId) throws Resources.NotFoundException {
        Boast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Close the view if it's showing, or don't show it if it isn't showing yet. You do not normally
     * have to call this. Normally view will disappear on its own after the appropriate duration.
     */
    public void cancel() {
        internalToast.cancel();
    }

    /**
     * Show the view for the specified duration. By default, this method cancels any current
     * notification to immediately display the new one. For conventional {@link Toast#show()}
     * queueing behaviour, use method {@link #show(boolean)}.
     *
     * @see #show(boolean)
     */
    public void show() {
        show(true);
    }

    /**
     * Show the view for the specified duration. This method can be used to cancel the current
     * notification, or to queue up notifications.
     *
     * @param cancelCurrent <code>true</code> to cancel any current notification and replace it with this new
     *                      one
     * @see #show()
     */
    public void show(boolean cancelCurrent) {
        // cancel current
        if (cancelCurrent) {
            final Boast cachedGlobalBoast = getGlobalBoast();
            if ((cachedGlobalBoast != null)) {
                cachedGlobalBoast.cancel();
            }
        }

        // save an instance of this current notification
        setGlobalBoast(this);
        internalToast.show();
    }

}
class Toasting {
    //    private static Handler mainLooperHandler;
//    private static Runnable toastRunnable;
    private Activity activity;
    private Boast t;

    Toasting(Activity act) {
//        mainLooperHandler = new Handler( Looper.getMainLooper() );
        activity = act;
    }

    void toast (String string, int duration) {
        toast(string, duration, false);
    }
    void toast (String string, int duration, boolean silenced) {
        if (!silenced) {
            show(string, duration);
        }
    }

    private void show(String string, int duration) {
        final String m = string;
        final int longOrShort = duration;
        //mainLooperHandler.removeCallbacks(toastRunnable);

        if (t!=null) {
            t.cancel();
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                t = Boast.makeText(activity, m, longOrShort);
                t.show();
            }
        });
        //Log.i("Youssef -Generic.java", "Toasting - show - A TOAST has been posted");
    }

    void toastUp (String string, int duration) {
        toastUp(string, duration, false);
    }
    void toastUp (String string, int duration, boolean silenced) {
        if (!silenced) {
            showUp(string, duration);
        }
    }

    private void showUp(String string, int duration) {
        final String m = string;
        final int longOrShort = duration;
        //mainLooperHandler.removeCallbacks(toastRunnable);

        if (t!=null) {
            t.cancel();
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                t = Boast.makeTextUp(activity, m, longOrShort);
                //t.internalToast.setGravity(Gravity.TOP|Gravity.RIGHT,0,0);
                t.show();
            }
        });
        //Log.i("Youssef -Generic.java", "Toasting - show - A TOAST has been posted");
    }

    void toastBottom (String string, int duration) {
//        Log.i("Youssef", "toast is in bottom");
        toastBottom(string, duration, false);
    }
    void toastBottom (String string, int duration, boolean silenced) {
        if (!silenced) {
//            Log.i("Youssef", "toast is in bottom");
            showBottom(string, duration);
        }
    }

    private void showBottom(String string, int duration) {
        final String m = string;
        final int longOrShort = duration;
        //mainLooperHandler.removeCallbacks(toastRunnable);

        if (t!=null) {
            t.cancel();
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
//                Log.i("Youssef", "toast is in bottom");
                t = Boast.makeTextBottom(activity, m, longOrShort);
                //t.internalToast.setGravity(Gravity.TOP|Gravity.RIGHT,0,0);
                t.show();
            }
        });
        //Log.i("Youssef -Generic.java", "Toasting - show - A TOAST has been posted");
    }

}
