package com.youssefdirani.cause_and_help;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.view.ViewCompat;

import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import static android.graphics.Typeface.BOLD;

class UserInteractionForLocationReceipt {
    private Toasting toasting;
    private Activity activity;
    private boolean isPendingFirstLocationReceiptNotification = false;
    private boolean isPendingSecondLocationReceiptNotification = false;
    private Snackbar snackBar_userNotification;

    UserInteractionForLocationReceipt(Toasting toasting, Activity activity) {
        this.toasting = toasting;
        this.activity = activity;
    }

    void setVariablesOnLocationUpdate() {
        if( isPendingFirstLocationReceiptNotification ) { //most likely to be true
            waitForLocationReceipt.removeCallbacks( runnable_notifyUserAboutGettingLocation );
            isPendingFirstLocationReceiptNotification = false;
        }
        if( isPendingSecondLocationReceiptNotification ) { //most likely to be always true
            waitForLocationReceipt.removeCallbacks( runnable_restartIfNoLocationIsReceived );
            isPendingSecondLocationReceiptNotification = false;
        }
        if( snackBar_userNotification != null ) {
            if (snackBar_userNotification.isShown()) {
                snackBar_userNotification.dismiss();
            }
        }
        if( alertDialog_suggestRestartAfterLocationNotReceived != null ) {
            if( alertDialog_suggestRestartAfterLocationNotReceived.isShowing() ) {
                alertDialog_suggestRestartAfterLocationNotReceived.dismiss();
            }
        }
    }

    void setUserInteraction_WaitForLocationReceipt() {
        //if any of them is already showing, don't try to reshow anything again
        if( snackBar_userNotification != null ) {
            if (snackBar_userNotification.isShown()) {
                return;
            }
        }
        if( alertDialog_suggestRestartAfterLocationNotReceived != null ) {
            if( alertDialog_suggestRestartAfterLocationNotReceived.isShowing() ) {
                return;
            }
        }

        toasting.toast("لطفاً إنتظر حتى يتم إيجاد موقعك", Toast.LENGTH_LONG);
        waitForLocationReceipt.postDelayed( runnable_notifyUserAboutGettingLocation , 10000);
        isPendingFirstLocationReceiptNotification = true;
        waitForLocationReceipt.postDelayed( runnable_restartIfNoLocationIsReceived , 25000);
        isPendingSecondLocationReceiptNotification = true;
    }

    private View getRootView() {
        final ViewGroup contentViewGroup = activity.findViewById(android.R.id.content);
        View rootView = null;

        if(contentViewGroup != null)
            rootView = contentViewGroup.getChildAt(0);

        if(rootView == null)
            rootView = activity.getWindow().getDecorView().getRootView();

        return rootView;
    }

    private final Handler waitForLocationReceipt = new Handler();
    private final Runnable runnable_notifyUserAboutGettingLocation = new Runnable() {
        public void run() {

            isPendingFirstLocationReceiptNotification = false;

            final View rootView = getRootView();
            String textToUser = "لم يتم تحديد موقعك بعد !\n" +
                    "لطفاً تأكّد أنّك متصل بالانترنت، و أنّك" +
                    " قد فعّلت خاصية \"دخول الموقع\" Location في إعدادات هاتفك.\n" +
                    "إن كان كل شيء صحيحاً، تفضّل بالانتظار 15 ثانية ريثما يتم تحديد موقعك.";
            if(rootView == null) {
                toasting.toast(textToUser , Toast.LENGTH_LONG);
                return;
            }

            snackBar_userNotification = Snackbar.make(rootView, textToUser, Snackbar.LENGTH_INDEFINITE);

            //this is to show the text from right to left
            ViewCompat.setLayoutDirection(snackBar_userNotification.getView(), ViewCompat.LAYOUT_DIRECTION_RTL);

            snackBar_userNotification.setAction("حسناً", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackBar_userNotification.dismiss();
                }
            });
            //to show a bit more lines in the snackbar
            View snackbarView = snackBar_userNotification.getView();
            TextView textView = snackbarView.findViewById(R.id.snackbar_text);
            textView.setMaxLines(7);
            //show the snackbar
            snackBar_userNotification.show();

        }
    };

    private AlertDialog alertDialog_suggestRestartAfterLocationNotReceived;
    final private Runnable runnable_restartIfNoLocationIsReceived = new Runnable() {
        public void run() {

            if( snackBar_userNotification.isShown() ) {
                snackBar_userNotification.dismiss();
            }

            isPendingSecondLocationReceiptNotification = false;

            String textToUser = "كي نحدّد موقعك، لطفاً إختر الخيار الأول أو الثاني :\n" +
                    "(الخيار الأول قد يعالج ما إذا كنت لم تفعّل إعدادات تحديد موقع جهازك.\n" +
                    "أمّا الخيار الثاني فهو أسرع ببعض الحالات)";

            final AlertDialog.Builder builder = new AlertDialog.Builder(
                    new ContextThemeWrapper( activity,
                            //R.style.AlertDialogCustomTheme)
                            android.R.style.Theme_Holo_Dialog )
                    //android.R.style.Theme_DeviceDefault_Dialog)
            );

            TextView titleTextView = new TextView( activity );
            titleTextView.setText( textToUser );
            titleTextView.setPadding(20,10,20,10);
            titleTextView.setTypeface(null,BOLD);
            titleTextView.setBackgroundResource(R.color.background_color);
            titleTextView.setTextSize(18);
            titleTextView.setTextColor(Color.LTGRAY );
            titleTextView.setGravity(Gravity.END);
            titleTextView.setGravity(Gravity.RIGHT);
            builder.setCustomTitle( titleTextView );

            final CharSequence[] items = {"إفتح تطبيق Google Maps أوتوماتيكياً",
                    "أعد تشغيل التطبيق أوتوماتيكياً",
                    "سأتصرّف وحدي"};
            builder.setItems( items,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // The 'which' argument contains the index position of the selected item
                            if( which == 0 ) {
                                dialog.dismiss();
                                Intent mapIntent = new Intent(Intent.ACTION_VIEW);
                                mapIntent.setPackage("com.google.android.apps.maps");
                                activity.startActivity(mapIntent);
                            } else if( which == 1 ) {
                                dialog.dismiss();
                                reload();
                            } else if( which == 2 ) {
                                dialog.dismiss();
                            }
                            dialog.dismiss();
                        }
                    });

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("youssef", "alertDialog_suggestRestartAfterLocationNotReceived should be showing now");
                    alertDialog_suggestRestartAfterLocationNotReceived = builder.create();
                    alertDialog_suggestRestartAfterLocationNotReceived.setCanceledOnTouchOutside(false);
                    alertDialog_suggestRestartAfterLocationNotReceived.show();
                }
            });

        }
    };

    private void reload() {
        Intent intent = activity.getIntent();
        activity.overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(intent);
    }

}
