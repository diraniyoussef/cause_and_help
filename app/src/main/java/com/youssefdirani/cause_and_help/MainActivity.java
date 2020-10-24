package com.youssefdirani.cause_and_help;

/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.os.Bundle;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Handler;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity { //, GooglePlayServicesClient.ConnectionCallbacks

    private Button button_salutStatement, button_caseStatement;

    //for beauty - animation
    private Handler waitThenHideView;
    private Runnable runnable_hideInfoText, runnable_hideStatementLL;

    Toasting toasting = new Toasting(this);
    private TextView textView_userGuide2;
    private TextView textView_userGuide3;
    private TextView textView_userGuide4;
    private EditText editText_statement, editText_userName;
    SharedPreferences mPrefs;
    boolean isFirstTime = false;
    private boolean isDisabled_statementButtons = false; /*two statement button will be disabled either if entering a
    * prevention waiting timeout (because user has just sent a statement), either if info is appearing in front of user.
    */
    private Handler waitAfterSendingStatement;
    private Runnable runnable_allowSendingAnotherStatement;
    private boolean isWaitingAfterStatementBeingSent = false;
    private LinearLayout linearLayout_info;
    private LinearLayout linearLayout_statement;

    FloatingActionButton fab_changeMap, fab_seeMyLocation;

    private Spinner spinner_selectStatementsToBeGotten;
    LinearLayout linearLayout_MarkerStatement;
    boolean isSalutsNotCasesSelected;

    UserInteractionForLocationReceipt userInteractionForLocationReceipt;
    MapSetup mapSetup;

    boolean isMemoryLow= false;

    @Override
    public void onTrimMemory(int level) { //implemented by ComponentCallbacks2 automatically in AppCompatActivity
        // Determine which lifecycle or system event was raised.
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
                /*
                   Release any UI objects that currently hold memory.

                   The user interface has moved to the background.
                */
                break;
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */
                //break;
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */
                //break;
            default:
                /*
                  Release any non-critical data structures.

                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                isMemoryLow = true;
                System.gc();
                break;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); //remove the titlebar
        setContentView(R.layout.activity_maps);

        isSalutsNotCasesSelected = true;
        spinner_selectStatementsToBeGotten = findViewById(R.id.spinner_StatementsToBeGotten);
        final ArrayAdapter<String> adapter_doSee = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, getResources()
                .getStringArray( R.array.spinner_do_string_array ) );
        adapter_doSee.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_selectStatementsToBeGotten.setAdapter( adapter_doSee );

        mapSetup = new MapSetup( MainActivity.this );

        if (!mapSetup.isServicesConnected()) {
            toasting.toast("رجاءً نزّل على جهازك Google Play Services", Toast.LENGTH_LONG);
            this.finish();
            return;
        }

        mPrefs = getSharedPreferences("myPrefs", 0);
        isFirstTime = mPrefs.getBoolean("FirstTime", true);
        userInteractionForLocationReceipt = new UserInteractionForLocationReceipt( toasting, MainActivity.this );

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapSetup.askForMapReady();

        markersAction = new MarkersAction(this); //an important arrayList is inside this object instance, and we don't want it to be reinstantiated in onResume.
    }

    final int Max_Statement_Length = 150; //please change it as well in statement.xml

    SocketConnection socketConnection;

    MarkersAction markersAction;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onResume() {
        super.onResume();
/*
        if( mapSetup.mMap == null ) { //for some silly reason like the connection being lost for a silly reason like app being in background for too long
            mapSetup.askForMapReady();
        }
        mapSetup.connectAttemptToGetCurrentLocation(); /*useful in case the connection
        *was lost during a long time app being in background. Although this may never happen*/
        mapSetup.isFirstTimeLocationReceived = true; //not necessary but useful for the sake of pause then resume for some silly reason.
        socketConnection = new SocketConnection( this );

        if( mapSetup.isConnected && isPendingIntentCancelled ) { //isPendingIntentCancelled will happen when the app pauses
            //here you're assuming that it's really connected
            mapSetup.mapOperations();
        }

        linearLayout_MarkerStatement = findViewById(R.id.ll_MarkerStatement);

        editText_userName = findViewById(R.id.editText_name);
        editText_userName.setText(mPrefs.getString("userName", ""));

        spinner_selectStatementsToBeGotten.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View arg1,
                                       int position, long id) {

                String item = parent.getItemAtPosition(position).toString();
                if( item.equalsIgnoreCase("شاهد التحيات") ) {
                    Log.i("Youssef", "user selected saluts");
                    isSalutsNotCasesSelected = true;
                    TextView textView = (TextView) arg1;
                    if( textView != null ) {
                        textView.setText("التحيات - الآن"); /*if you care about the size of the box then
                    * https://stackoverflow.com/questions/12007099/dynamically-change-width-of-spinner-in-android*/
                    } else {
                        Log.i("Youssef", "weird that view was null, in saluts");
                    }
                    if( mapSetup.isLocationReceived ) { //also means that currentScreen is defined
                        toasting.toastUp("انت تشاهد تحيات الناس الآن", Toast.LENGTH_LONG, isFirstTime);
                        /*Now we have mapSetup.currentScreen bounds ready since we instantiated it in mapSetup.updateLocation(),
                        and since we have a location updated, so we can setRequestMessageToSend()
                        * */
                        if( mapSetup.requestMarkersInSameScreen() ) {
                            byte[] final_message_byte = setRequestMessageToSend();
                            socketConnection.socketConnectionSetup( final_message_byte );
                        }
                    } else {
                        toasting.toastUp("عندما يتم تحديد موقعك، سوف تشاهد تحيات الناس", Toast.LENGTH_LONG, isFirstTime);
                    }

                } else if( item.equalsIgnoreCase("شاهد القضايا") ) {
                    Log.i("Youssef", "user selected cases");
                    //spinner_selectStatementsToBeGotten.setAdapter(adapter_youAreSeeing);
                    isSalutsNotCasesSelected = false;
                    TextView textView = (TextView) arg1;
                    if( textView != null ) {
                        textView.setText("القضايا - الآن");
                    } else {
                        Log.i("Youssef", "weird that view was null, in cases");
                    }
                    if( mapSetup.isLocationReceived ) {
                        toasting.toastUp("انت تشاهد قضايا الناس الآن", Toast.LENGTH_LONG, isFirstTime);
                        if( mapSetup.requestMarkersInSameScreen() ) {
                            byte[] final_message_byte = setRequestMessageToSend();
                            socketConnection.socketConnectionSetup( final_message_byte );
                        }
                    } else {
                        toasting.toastUp("عندما يتم تحديد موقعك، سوف تشاهد قضايا الناس", Toast.LENGTH_LONG, isFirstTime);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        button_salutStatement = findViewById(R.id.make_statement_salut);
        button_caseStatement = findViewById(R.id.make_statement_case);

        waitAfterSendingStatement = new Handler();
        runnable_allowSendingAnotherStatement = new Runnable() {
            public void run() {
                enable_StatementButtons();
            }
        };

        editText_statement = findViewById(R.id.editText_statement);
        final Animation in = new AlphaAnimation(0.5f, 1.0f);
        in.setDuration(500);



        button_salutStatement.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if( !commonBehaviorAfterStatementButtonPressed( v ) ) {
                  return;
                }
                toasting.toast("الناس تريد أن ترى أجمل تحية منك",
                        Toast.LENGTH_LONG);
                //smoothDisappearance( linearLayout_info , runnable_hideInfoText );
                linearLayout_info.setVisibility(View.GONE);
                editText_statement.setHint("التحية - " +
                        Max_Statement_Length +
                        " حرفاً كحد أقصى");
                fab_changeMap.setVisibility(View.GONE);
                fab_seeMyLocation.setVisibility(View.GONE);
                linearLayout_statement.setVisibility(View.VISIBLE);
                isSalutNotCaseToBeSent = true;
            }
        });

        button_caseStatement.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if( !commonBehaviorAfterStatementButtonPressed( v ) ) {
                    return;
                }
                toasting.toast("إطرح القضية و اقترح حلاً",
                        Toast.LENGTH_LONG);
                //smoothDisappearance( linearLayout_info , runnable_hideInfoText );
                linearLayout_info.setVisibility(View.GONE);
                fab_changeMap.setVisibility(View.GONE);
                fab_seeMyLocation.setVisibility(View.GONE);
                editText_statement.setHint("القضية - " +
                        Max_Statement_Length +
                        " حرفاً كحد أقصى");
                linearLayout_statement.setVisibility(View.VISIBLE);
                isSalutNotCaseToBeSent = false;
            }
        });

        final Button button_cancelStatement = findViewById( R.id.button_cancelStatement );
        button_cancelStatement.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                enable_StatementButtons();
                fab_changeMap.setVisibility(View.VISIBLE);
                fab_seeMyLocation.setVisibility(View.VISIBLE);

                linearLayout_statement.setVisibility(View.GONE);
            }
        });

        final Button button_sendStatement = findViewById( R.id.button_sendStatement );
        button_sendStatement.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String statement = editText_statement.getText().toString();
                String userName = editText_userName.getText().toString();
                if( userName.equals("") ) {
                    toasting.toast("لطفاً أدخل إسماً", Toast.LENGTH_SHORT);
                    editText_userName.requestFocus();
                    return;
                }
                if( statement.equals("") ) {
                    toasting.toast("لطفاً أدخل عبارة", Toast.LENGTH_SHORT);
                    editText_statement.requestFocus();
                    return;
                }
                if( !mapSetup.isLocationReceived ) {
                    toasting.toast("لم نحدد بعد موقعك لذا لا يمكننا أن نسجّل عبارتك !", Toast.LENGTH_SHORT);
                    return;
                }
                smoothDisappearance( linearLayout_statement , runnable_hideStatementLL );
                if( mapSetup.mMap!= null && mapSetup.isConnected) {
                    byte[] final_message_byte = setStatementMessageToSend(userName, isSalutNotCaseToBeSent, statement);
                    // now message_byte is set
                    sendMessage(final_message_byte);
                }

                fab_changeMap.setVisibility(View.VISIBLE);
                fab_seeMyLocation.setVisibility(View.VISIBLE);
                waitAfterSendingStatement.postDelayed( runnable_allowSendingAnotherStatement, 30000 );
            }
        });

        linearLayout_info = findViewById(R.id.ll_info);
        linearLayout_statement = findViewById(R.id.ll_statement);

        textView_userGuide2 = findViewById(R.id.textView_userGuide2);
        textView_userGuide3 = findViewById(R.id.textView_userGuide3);
        textView_userGuide4 = findViewById(R.id.textView_userGuide4);

        waitThenHideView = new Handler();
        runnable_hideInfoText = new Runnable() { //better this should be made by the deamon service
            @SuppressLint("RestrictedApi")
            public void run() {
                linearLayout_info.setVisibility(View.GONE);
                fab_changeMap.setVisibility(View.VISIBLE);
                fab_seeMyLocation.setVisibility(View.VISIBLE);
                //textView_userGuide2.setVisibility(View.GONE);
                //textView_userGuide3.setVisibility(View.GONE);
                //textView_userGuide4.setVisibility(View.GONE);
                if( isFirstTime ) {
                    mPrefs.edit().putBoolean("FirstTime", false).apply();
                    isFirstTime = false; //a must
                    enable_StatementButtons();
                    spinner_selectStatementsToBeGotten.setEnabled(true);
                    if( !mapSetup.isLocationReceived ) {
                        userInteractionForLocationReceipt.setUserInteraction_WaitForLocationReceipt();
                    } else {
                        toasting.toastUp("انت تشاهد تحيات الناس الآن", Toast.LENGTH_LONG);
                    }
                }
            }
        };
        runnable_hideStatementLL = new Runnable() { //better this should be made by the deamon service
            @SuppressLint("RestrictedApi")
            public void run() {
                linearLayout_statement.setVisibility(View.GONE);
            }
        };

        final FloatingActionButton fab_info = findViewById(R.id.fab_info);
        fab_info.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //smoothDisappearance( linearLayout_statement , runnable_hideStatementLL );
                if( !isWaitingAfterStatementBeingSent && !isFirstTime ) {
                    enable_StatementButtons();
                }

                linearLayout_statement.setVisibility(View.GONE);
                fab_changeMap.setVisibility(View.GONE);
                fab_seeMyLocation.setVisibility(View.GONE);

                linearLayout_info.setVisibility(View.VISIBLE);
            }
        });

        fab_seeMyLocation = findViewById(R.id.fab_currentlocation);
        fab_seeMyLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mapSetup.seeUserMarker();
            }
        });


        fab_changeMap = findViewById(R.id.fab_changemaplayer);
        fab_changeMap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mapSetup.changeMapType();
            }
        });

        final Button button_hideInfo = findViewById( R.id.button_removeScrollView );
        button_hideInfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if( !isWaitingAfterStatementBeingSent ) {
                    enable_StatementButtons();
                }
                smoothDisappearance( linearLayout_info , runnable_hideInfoText );
            }
        });

        final String str_aboutApp1 =
                "أنت تلميذ مدرسة أو عاملاً أو أمّاً أو مديراً أو أي أحد.\n\n" +
                "إنّ هذا التطبيق هو : \n 1) إما لكي تزرع كلمة طيّبة بقلب " +
                "أخيك اللبناني أو من يستضيفه لبنان. " +
                "فالجميع بحاجة أن يسمع أمنية بيوم جميل و أحياناً قد يحب أن يسمعها من شخص لا يعرفه.\n" +
                "2) إما لكي تقترح قضية و حلاً لها، و غالباً مختصة بموقع ما. فيمكنك " +
                "أن تعالج بعض المشاكل من خلال التطبيق أيضاً (كما سيتبيّن لك عندما تقرأ المزيد)." +
                "\n\n" +
                "كلماتك ستصل لمن هم بمحيط موقعك حين أصدرت الكلمات :" +
                        "\n" +
                "قد يكون بمحيط 10 أمتار أو على بعد 50 متر أو 100 متر أو " +
                "حتى 250 متر.\n\n" +
                "ستظهر كلماتك من موقعك... و سيصل كلامك لكل من يفتح التطبيق بشرط أن يكون داخل دائرتك.\n" +
                "من هو خارج الدائرة سيعرف أنّ هناك كلاماً ما بهذا المكان و بهذه " +
                        "\n" +
                "تجدر الإشارة أيضاً أنّ هذه النسخة من التطبيق هي تجريبية -كونها النسخة الأولى- ، " +
                        "لذا ترى على الأيقونة الحرف اليوناني β." +
                        "\n";

        final Spanned str_aboutApp2 = Html.fromHtml(
        "<strong>أخي المواطن (أختي المواطنة) – حضرة المقيم (المقيمة) بلبنان.</strong>" +
                "<br/>" +
                "بإمكانك تقول ما يخطر ببالك مما يحتاج الناس لسماعه من كلام لطيف (بمصطلح التطبيق أن \"تنشئ تحية\")." +
                "<br/>" +
                "مثلاً قد تقول صباح الخير (بدائرة 10 أمتار إن كان هناك حساسيات بينك و بين جارك)، " +
                "أو تقول نهار عمل سعيد يا جيراني (بدائرة 50 متر)، أو تقول جدّوا و اجتهدوا يا طلاب المدارس (بدائرة 70 متر)" +
                " بعد أن تكون قد مررت بمدرسة ما." +
                "<br/>" +
                " أو قد تقول لموظّفيك (بدائرة 50 متر) أنه لولاهم لما استمرّت الشركة بالعطاء " +
                "و أنك تقدّر جهودهم و تتمنى أن يعذروك لعدم تمكّنك" +
                " أن تسلّم عليهم واحداً واحداً." +
                "<br/>" +
                "أو قد تكون موظّفاً و تريد أن ترسل سلاماً لزملائك إن كان لا مانع من ذلك في سياسة الشركة." +
                "<br/>" +
                "و كما علمت بإمكانك ان تحدد حجم الدائرة التي سيسمع من بداخلها فقط كلامك. " +
                "مركز الدائرة سيكون موقعك حين أنشأت التحية." +
                "<br/>" +
                "<br/>" +
                "أنت تعلم أنّ ما يريده اللبنانيون هو ليس سيجارة يطفئون غضبهم بها، و لا أرغيلة يمضون وقتهم بها" +
                " -فهذا متوفر- ، " +
                "بل هي كلمة طيبة لا تجرح أحداً و من محب لمحب." +
                "<br/>" +
                "يمكنك أن تبدي رأيك و أنت بمكان ما، و (في هذه النسخة من التطبيق) من سيكون بداخل دائرتك سيقرأ كلماتك؛ " +
                "ناس ممّن لا تعرفهم هناك سيقرأون كلماتك. لذا قل خيراً و بأسلوب جيد و لطيف و محبّب، و لا تقل شيئاً آخراً." +
                "<br/>" +
                "<br/>" +
                "يمكنك أن تثير النظر حول أمر ما (بمصطلح التطبيق أن \"تنشئ قضية\")، لكن بشرط أن تذكر حلاً مناسباً له " +
                "و أن يكون هذا الحل من الناس فقط، و " +
                "ليس من أي سلطة رسمية أو شبه رسمية. و أن يكون انتقادك لطيفاً جداً " +
                "و غير مؤلم لأحد، لا لأفراد أو جمعيات أو مؤسسات أو سُلُطات. فلتتكلّم بالإشارة مثلاً و لتتكلّم بمحبة و لا تتكلّم بتهكّم. " +
                "و " +
                "<strong>اقترح و لا تنتقد</strong>" +
                " - فهذا ألطف؛ لا نريد لمستوى الكلام أن ينحدر إلى ضيق خُلُق أو ضيق بالنفس بل نريد سعة الصدر" +
                " و بالأخص من الذي يبتدئ الكلام." +
                "<br/>" +
                "يعني فكّر بجملتك 15 مرة قبل أن تكتبها. و كن مؤدّباً لأقصى الحدود و ابتعد عن الأشياء الشخصية و فكّر بمصلحة الجميع." +
                "<br/>" +
                "<br/>" +
                "مثلاً، أنت كهربجي و مضطر ان تقطع الكهرباء غداً أو بعد 4 ساعات لكي تصلّح عطلاً فنّياً، فلكي تتصرّف بطريقة حضارية و كي لا تفاجئ السكان " +
                "بقطعان الكهرباء، يمكنك أن تقول بجملة لطيفة أنك عذراً ستقطع الكهرباء كي تصلّح عطلاً ما. و تكسب احترام السكان." +
                "<br/>" +
                "أو مثلاً، قد ضيّعت محفظتك (جزدانك) بمكان ما هنا، و أنت لا تعرف أين، فتصدر بياناً " +
                "مثلاً بدائرة 150 متر، أنّ من يجد المحفظة فليضعها في المحل الفلاني، " +
                "أو فليضعها في المحل الذي أصدرت منه البيان. فإذا وضعها تصبح على علم." +
                "<br/>" +
                "أو مثلاً، قد يكون اقتراحك فكرة تجارية يمكن لبعض الأشخاص أن يقوموا بها." +
                "<br/>" +
                "أو مثلاً لا يوجد ناطور ببنايتك و سيأتي مصلّح ما إلى بنايتك ليصلّح شيئاً ما بطلب من جارك بالأقسام المشتركة، " +
                "و أنت تعلم أنه بهذا المكان هناك قساطل مياه و تخشى أن يقدّح الحائط بنفس المكان، " +
                "فيمكنك أن تترك إشعاراً و توضح فيه اهتمامك." +
                "<br/>" +
                "أو مثلاً، و أنت تقود السيارة، " +
                "مررت بجانب شخص وقع منه شيئاً على الطريق، فتقول بذلك الموقع و بدائرة أنت تحددها : " +
                "\"أتمنى من كل الأصدقاء المشاة أن " +
                "يزيلوا القمامة من الطريق و هم يمشون. فقد وقعت من أحد المشاة محرمة عبر الخطأ و انا لا أقدر ان أشيلها.\" " +
                "بهذه الطريقة أنت ذكرت المشكلة و ذكرت الحل و كان الحل من الشعب." +
                "<br/>" +
                "أو مثلاً، إذا كنت متواجداً على شاطئ البحر و أردت ان تقترح على الموجودين أن نعمل جميعنا حملة نظافة." +
                "<br/>" +
                "أو مثلاً، إذا مررت بمكان فسيح و لا يستثمره أحد، " +
                "فتقول بعد ان تكون حدّدت حجم الدائرة : " +
                "\"يا حبذا لو نستغل هذه البوراية لأجل حديقة عامة. ما رأيكم ؟ " +
                "هل يعرف أحد صاحب هذه الأرض ؟ رقم هاتفي .....\"" +
                "<br/>" +
                " أيضاً بهذه الطريقة أنت ذكرت المشكلة و ذكرت الحل و كان الحل من الشعب." +
                "<br/>" +
                "أحياناً قد يصدف أنّك تريد أن تطرح قضية ما و لا تريد أن تذكر اسمك. فيمكنك ذلك بكل تأكيد !" +
                "<br/>" +
                "يعني تستطيع أن تلفت نظر الموجودين بمكان ما عن مسألة ما أو اقتراح ما. " +
                "أنت تترك بصمتك و كل من سيمرّ بمكانك سيلتفت !" +
                "<br/>" +
                "كم و كم من الأشياء نريد أن نبدي رأينا بها !" +
                "<br/>" +
                "أمّا إن مررت فوق مطب للسيارات" +
                " و أردت أن تقول \"أرجو من البلدية ان يزيلوا المطب لأن سيارتي تضرّرت.\" " +
                "فقد يكون في هذا الكلام إهانة للبلدية لذا لا تنشر" +
                " هكذا كلام. " +
                "و حتى لو أردت ان تقترح حلاً من الشعب لهذا المطبّ فأيضاً فيه إهانة للبلدية. " +
                "لذا لا تثر هكذا قضية من الأساس و اذهب و تكلّم معهم شخصياً،" +
                " فهناك آلاف القضايا التي يمكن للشعب أن يحلّها بنفسه عبر التطبيق و يجعل من بلده جنّة بها، و لا " +
                "نحتاج منّة أحد.  هذه فرصة للمجتمع الأهلي كي يثبت وجوده." +
                "<br/>" +
                "<br/>" +
                "أمّا بالنسبة لإصلاح الفساد الإداري، فإن صانع التطبيق مهتم مثلك، و إذا عندك فكرة نيّرة فلتطرحها معه." +
                "<br/>" +
                "<br/>" +
                "فيما يخصّ تطبيقنا هنا، القاعدة بشكل عام هي، أذكر أي قضية لها مردود إيجابي بشرط ألّا تجرح أو تحرج أحداً." +
                "<br/>" +
                "من سياسة التطبيق أيضاً ألا تصفّي حساباتك الشخصية هنا. مثلاً عندك مشكلة مع والدك أو جارك أو إبنك أو مديرك، " +
                "فهذا الأمر خاص بكما. هذا التطبيق ليس للتشهير كما علمت. " +
                "و لتكن غيرتك على سمعة أخيك في لبنان مثل غيرتك على سمعة عائلتك. و من دون شك ستكون " +
                "جنابك نموذجاً لكل من يسمع كلامك " +
                "و عنواناً للوعي و الأدب اللبناني." +
                "<br/>" +
                "<br/>" +
                "قد تبدو سياسة التطبيق صعبة، لكن العقل يحتاج لصوت هادئ لكي يسمع، " +
                "فمهما تكلّمت بحق و قد آذيت المشاعر - بالأخص أنك تتكلّم علناً - فلن تصل لنتيجة. لذا خذ وقتك و تكلّم بلطف." +
                "<br/>" +
                "إعلم أنّ كلامك سيصوّت عليه الناس، و إذا كانت نتيجة التصويت سلبية ستحذف كلماتك تلقائياً و فوراً من التطبيق." +
                "<br/>" +
                "إنّ كلامك سيظل لفترة 24 ساعة - و لكن إذا حاز على تصويت إيجابي بنسبة كبيرة جداً فسيظلّ لوقت أطول." +
                "<br/>" +
                "يمنع أن تذكر فئة أو مجتمعاً أو ديناً أو حزباً أو تنظيماً أو مسؤولاً أو أي جهة بسوء (و الأفضل لا حتى بخير). " +
                "يمكنك فقط أن تطرح مشاكل و حلول " +
                "يمكن للناس - و فقط الناس - أن يقوموا بها." +
                "<br/>" +
                "بإمكانك أن تتحكّم، كما أسلفنا، بحجم الدائرة (دائرة التبليغ)، فتوسّعها أو تضيّقها حسب ما تراه مناسباً " +
                "(و ذلك بأن تضغط على موقعك باللون الأحمر كي تتمكّن من تعديل الدائرة، " +
                "ثم تضغط ضغطة طويلة على النقطة الموجودة أعلى الدائرة و تحرّكها، فتصغّر الدائرة او تكبّرها). " +
                "<br/>" +
                "عليك أن تدخل إسمك الحقيقي (أو المستعار)، ثم تحدد سعة الدائرة التي تريد أن توصل صوتك لها." +
                "<br/>" +
                "بما أنك ستستعمل الخريطة، يستحسن أن تعرف كيف تتحكّم بالخريطة بواسطة إصبع أو إصبعين. " +
                "<br/>" +
                "و حيثما تريد أن ترى تفاعلات الناس، إقترب بالشاشة نحو ذلك المكان الذي تريده." +
                "<br/>" +
                "إذا ابتعدت كثيراً قد تختفي تفاعلات الناس." +
                "<br/>" +
                "<br/>" +
                "أنت باستعمالك هذا التطبيق تكون قد وافقت ضمناً على سياسته." +
                "<br/><br/>" +
                "إنّ هذا التطبيق ليس مجانياً، و ثمنه أن تخبر أصدقاءك و من يجري معهم لسانك عنه." +
                "<br/><br/>" +
                "يمكنك أن ترسل اقتراحك أو شكرك أو دعمك المعنوي أو المادي لصانع هذا التطبيق كي يستمرّ بالعطاء." +
                "<br/>" +
                " و في ما يلي من تطبيقات، سيأتي ما يقرّب بين اللبنانيين و يزيل  الحواجز النفسية بينهم، ما يساهم ببناء الدولة الآمنة " +
                "و صناعة الأمن القومي." +
                "<br/>"
        );

        final Spanned str_aboutApp3 = Html.fromHtml("البريد الإلكتروني : diraniyoussef@gmail.com" +
                "<br/>" +
                "العنوان : النميرية، قضاء النبطية، طريق عام الشرقية، " +
                "بناية الندى 1، مفرق الملعب البلدي، محل أخيكم \"إلكتروتل ديراني\" - " +
                "الإحداثيات (خط طول : 35.41302 , خط عرض : 33.4165429)."
        );
        final String str_aboutApp4 = "أو على الخريطة.";


        final TextView textView_userGuide1 = findViewById(R.id.textView_userGuide1);
        textView_userGuide1.setText(str_aboutApp1);

        final TextView textView_userGuideMore = findViewById(R.id.textView_userGuideMore);
        textView_userGuideMore.setText("المزيد حول التطبيق و سياسته");

        textView_userGuide2.setText(str_aboutApp2);
        textView_userGuide3.setText(str_aboutApp3);
        textView_userGuide4.setText(str_aboutApp4);

        textView_userGuideMore.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                explainMore();
            }
        });

        if (isFirstTime) {
            Log.i("Youssef", "disabling the SOS button as this is the first time");
            disable_StatementButtons();
            spinner_selectStatementsToBeGotten.setEnabled(false);
            linearLayout_info.setVisibility(View.VISIBLE);
            fab_changeMap.setVisibility(View.GONE);
            fab_seeMyLocation.setVisibility(View.GONE);
            explainMore();
        }

        textView_userGuide4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                smoothDisappearance( linearLayout_info , runnable_hideInfoText );
                if( mapSetup.mMap!= null && mapSetup.isConnected) {
                    mapSetup.referent = "electrotel";
                    mapSetup.changeMap();
                }
            }
        });

        final ImageButton imageButton_like = findViewById(R.id.imagebutton_like);
        imageButton_like.setOnTouchListener( /* I may have dismissed the setOnTouchListener here since I already have
                 * the dispatchTouchEvent below but I didn't know how to identify a touch on this linear layout.
                 * So a workaround is to use this block in coordination with the dispatchTouchEvent, and this is possible since the app listens here before it does there,
                 * thus a boolean here assigned here, if checked there, then it's a valid touch event.
                 */
                //This really works as it should; a touch on the "like" or "dislike" buttons isn't caught here.
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        /*Checking if user touched outside the marker statement if it was already shown. If touched inside, we'll do nothing. If
                         * touched outside, we'll remove the marker statement. (Actually not quite the right thing to do, what needs to be checked
                         * in addition, is whether the same marker has been clicked. For this, it's sort of wrong to remove the statement right away.)
                         * A better mechanism can be made, like a timer for the check to be made in onMarkerClick. This timer would be something like 400ms.
                         * If it turned to be that user clicked on the SAME MARKER AS THE LAST ONE CLICKED then we don't reanimate linearLayout_MarkerStatement.
                         * But for now, we'll simply remove it right away, and it's ot that wrong.
                         */
                        Log.i("Youssef", "imageButton_like is touched");
                        isTouched_imageButton_like = true;
                        return false;
                    }
                });

        final ImageButton imageButton_dislike = findViewById(R.id.imagebutton_dislike);
        imageButton_dislike.setOnTouchListener( /* I may have dismissed the setOnTouchListener here since I already have
                 * the dispatchTouchEvent below but I didn't know how to identify a touch on this linear layout.
                 * So a workaround is to use this block in coordination with the dispatchTouchEvent, and this is possible since the app listens here before it does there,
                 * thus a boolean here assigned here, if checked there, then it's a valid touch event.
                 */
                //This really works as it should; a touch on the "like" or "dislike" buttons isn't caught here.
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        /*Checking if user touched outside the marker statement if it was already shown. If touched inside, we'll do nothing. If
                         * touched outside, we'll remove the marker statement. (Actually not quite the right thing to do, what needs to be checked
                         * in addition, is whether the same marker has been clicked. For this, it's sort of wrong to remove the statement right away.)
                         * A better mechanism can be made, like a timer for the check to be made in onMarkerClick. This timer would be something like 400ms.
                         * If it turned to be that user clicked on the SAME MARKER AS THE LAST ONE CLICKED then we don't reanimate linearLayout_MarkerStatement.
                         * But for now, we'll simply remove it right away, and it's ot that wrong.
                         */
                        Log.i("Youssef", "imageButton_dislike is touched");
                        isTouched_imageButton_dislike = true;
                        return false;
                    }
                });

        linearLayout_MarkerStatement.setOnTouchListener( /* I may have dismissed the setOnTouchListener here since I already have
                * the dispatchTouchEvent below but I didn't know how to identify a touch on this linear layout.
                * So a workaround is to use this block in coordination with the dispatchTouchEvent, and this is possible since the app listens here before it does there,
                * thus a boolean here assigned here, if checked there, then it's a valid touch event.
                */
                //This really works as it should; a touch on the "like" or "dislike" buttons isn't caught here.
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        /*Checking if user touched outside the marker statement if it was already shown. If touched inside, we'll do nothing. If
                         * touched outside, we'll remove the marker statement. (Actually not quite the right thing to do, what needs to be checked
                         * in addition, is whether the same marker has been clicked. For this, it's sort of wrong to remove the statement right away.)
                         * A better mechanism can be made, like a timer for the check to be made in onMarkerClick. This timer would be something like 400ms.
                         * If it turned to be that user clicked on the SAME MARKER AS THE LAST ONE CLICKED then we don't reanimate linearLayout_MarkerStatement.
                         * But for now, we'll simply remove it right away, and it's ot that wrong.
                         */
                        Log.i("Youssef", "linearLayout_MarkerStatement is touched");
                        isTouched_linearLayout_MarkerStatement = true;
                        return false;
                    }
                });

    }

    private boolean isTouched_linearLayout_MarkerStatement = false;
    private boolean isTouched_imageButton_like = false;
    private boolean isTouched_imageButton_dislike = false;

    private boolean isSalutNotCaseToBeSent = true;

    final int Max_Name_Length = 20;//please change this as well in statement.xml
    /*message is like that : message type (s for statement and r for request) -> userName -> trailor -> statement ->
    * trailor -> statementType(either s for salute or c for cause) -> lat 8 bytes -> lng 8 bytes ->
    * radius 1 byte -> trailor
    */


    private byte[] setStatementMessageToSend( String userName , boolean isSalutNotCase , String statement ) {
        /*the format :
        * 's' for salute or 'c' for cause->userName->trailor->statement->trailor->lat->lng->radius->trailor
        * */
        /*An interesting note concerning chars of userName and statement :
        * Taking one byte is not fine as it doesn't differentiate between J (U+004A) and ي in arabic (U+064A) so
        * the second byte is still necessary; we know that the size of a char in java is 2 bytes. BTW, the arabic usually ranges from
        * U+0600 to U+06FF.
        * (unicodes can probably reach 3 bytes but I guess the third byte is not a char).
        * */
        final byte trailor = 127; /*BTW the choice of this trailor as an indicator to the end of char sequence if good , since
        * searching for FF as the MSByte of a char here https://en.wikipedia.org/wiki/List_of_Unicode_characters has no results. (FF
        * is not the upper - higher byte of any char) */
        final int sizeOfChar = 2;
        final int Max_Buffer_Size = 1 + Max_Name_Length * sizeOfChar + 1 + Max_Statement_Length * sizeOfChar + 1 + 8 + 8 + 1 + 1; //362
        final byte message_byte[] = new byte [Max_Buffer_Size];

        int byte_index = 0;
        if( isSalutNotCase ) {
            message_byte[ byte_index ] = 's';
        } else {
            message_byte[ byte_index ] = 'c';
        }

        byte_index++;
        int charsToCopy = userName.length(); //we already know charsToCopy won't be 0
        for( int i = 0 ; i < charsToCopy ; i++ ) {
            char c = userName.charAt( i );
            //now let's take the MSByte
            message_byte[ byte_index + i ] = (byte) ( c >>> 8 ); //shifting to the right 8 times. //not wrong to use >> instead of >>>
            //now the LSByte
            message_byte[ byte_index + i + 1 ] = (byte) c;
        }
        byte_index += sizeOfChar * charsToCopy;
        message_byte[ byte_index ] = trailor;

        byte_index++;
        charsToCopy = statement.length(); //we already know charsToCopy won't be 0
        for( int i = 0 ; i < charsToCopy ; i++ ) {
            char c = statement.charAt( i );
            message_byte[ byte_index + i ] = (byte) ( c >>> 8 );
            message_byte[ byte_index + i + 1 ] = (byte) c;
        }
        byte_index += charsToCopy * sizeOfChar;
        message_byte[ byte_index ] = trailor;

        byte_index++;
        byte[] EightBytes = doubleToByteArray( mapSetup.getLat() );
        for( int i = 0 ; i < 8 ; i++ ) {
            message_byte[ i + byte_index ] = EightBytes[ i ];
            EightBytes[ i ] = 0;
        }

        byte_index += 8;
        EightBytes = doubleToByteArray( mapSetup.getLng() );
        for ( int i = 0 ; i < 8 ; i++ ) {
            message_byte[ i + byte_index ] = EightBytes[ i ];
            EightBytes[ i ] = 0;
        }

        byte_index += 8;
        message_byte[ byte_index ] = mapSetup.getRadius();

        byte_index++;
        message_byte[ byte_index ] = trailor;

        /*
        byte_index++;
        if( byte_index < Max_Buffer_Size ) {
            message_byte[ byte_index ] = '\0'; //I'm not dealing with printwriter so this isn't good I belive
        }
        */
        byte[] final_message_byte = new byte[ byte_index + 1 ];
        System.arraycopy( message_byte,0, final_message_byte,0,byte_index + 1  );
        return final_message_byte;
    }

    void sendMessage( byte[] message_byte ) {
        if( mapSetup.mMap!= null && mapSetup.isConnected) {
            socketConnection.socketConnectionSetup(message_byte);
        }
    }

    byte[] setRequestMessageToSend() {
        final byte trailor = 127;
        /*should also send isSalutsNotCasesSelected and the current view bounds.
        * The format should be like : r->s (for salute) or c (for cause)->8 bytes lat of northeast->8 bytes lng of northeast->
        * 8 bytes lat of southwest->8 bytes lng of southwest->trailor
        */
        final int Max_Buffer_Size = 1 + 1 + 8 + 8 + 8 + 8 + 1;
        final byte message_byte[] = new byte [Max_Buffer_Size];

        message_byte[ 0 ] = 'r'; //as of "request"
        if( isSalutsNotCasesSelected ) {
            message_byte[ 1 ] = 's';
        } else {
            message_byte[ 1 ] = 'c';
        }
        /*
        Log.i("Youssef", "old NE_lat is " + String.valueOf(mapSetup.oldScreen.northeast.latitude) );
        Log.i("Youssef", "old NE_lng is " + String.valueOf(mapSetup.oldScreen.northeast.longitude) );
        Log.i("Youssef", "old SW_lat is " + String.valueOf(mapSetup.oldScreen.southwest.latitude) );
        Log.i("Youssef", "old SW_lng is " + String.valueOf(mapSetup.oldScreen.southwest.longitude) );
         */

        int byte_index = 2;
        byte[] EightBytes = doubleToByteArray( 2 * mapSetup.oldScreen.northeast.latitude - mapSetup.oldScreen.southwest.latitude );
        Log.i("Youssef", "new NE_lat is " + String.valueOf(2 * mapSetup.oldScreen.northeast.latitude - mapSetup.oldScreen.southwest.latitude) );
        for( int i = 0 ; i < 8 ; i++ ) {
            message_byte[ i + byte_index ] = EightBytes[ i ];
            EightBytes[ i ] = 0;
        }
        byte_index += 8;
        EightBytes = doubleToByteArray( 2 * mapSetup.oldScreen.northeast.longitude - mapSetup.oldScreen.southwest.longitude );
        Log.i("Youssef", "new NE_lng is " + String.valueOf(2 * mapSetup.oldScreen.northeast.longitude - mapSetup.oldScreen.southwest.longitude) );
        for( int i = 0 ; i < 8 ; i++ ) {
            message_byte[ i + byte_index ] = EightBytes[ i ];
            EightBytes[ i ] = 0;
        }
        byte_index += 8;
        EightBytes = doubleToByteArray( 2 * mapSetup.oldScreen.southwest.latitude - mapSetup.oldScreen.northeast.latitude );
        Log.i("Youssef", "new SW_lat is " + String.valueOf(2 * mapSetup.oldScreen.southwest.latitude - mapSetup.oldScreen.northeast.latitude) );
        for( int i = 0 ; i < 8 ; i++ ) {
            message_byte[ i + byte_index ] = EightBytes[ i ];
            EightBytes[ i ] = 0;
        }
        byte_index += 8;
        EightBytes = doubleToByteArray( 2 * mapSetup.oldScreen.southwest.longitude - mapSetup.oldScreen.northeast.longitude );
        Log.i("Youssef", "new SW_lng is " + String.valueOf(2 * mapSetup.oldScreen.southwest.longitude - mapSetup.oldScreen.northeast.longitude) );
        for( int i = 0 ; i < 8 ; i++ ) {
            message_byte[ i + byte_index ] = EightBytes[ i ];
            EightBytes[ i ] = 0;
        }
        byte_index += 8;
        message_byte[ byte_index ] = trailor;
//        byte_index++;
        //if( byte_index < Max_Buffer_Size ) { //which is of course true
            //message_byte[byte_index] = '\0';
        //}
        byte[] final_message_byte = new byte[ byte_index + 1 ];
        System.arraycopy( message_byte,0, final_message_byte,0,byte_index + 1  );
        return final_message_byte;
    }

    byte[] doubleToByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }
/*
    byte[] intToByteArray(int value) {
        byte[] bytes = new byte[4];
        ByteBuffer.wrap( bytes ).putInt( value );
        return bytes;
    }
*/
    boolean commonBehaviorAfterStatementButtonPressed( View v ) {
        if (isDisabled_statementButtons) {
            if (isFirstTime) { //we're showing info
                toasting.toast("لطفاً أتمم القراءة قبل المباشرة بالإستخدام", Toast.LENGTH_SHORT);
            } else if( isWaitingAfterStatementBeingSent ) {
                toasting.toast("لطفاً إنتظر قليلاً قبل أن تنشئ تعميماً آخراً", Toast.LENGTH_SHORT);
            } else { //we are actually making a statement.

            }
            return false;
        }
        disable_StatementButtons();
        //just to give the effect of a button click
        //v.startAnimation(out);
        //v.startAnimation(in);
        return true;
    }

    void smoothDisappearance( View LL , Runnable runnable ) {
        final int durationToDisappear = 100;
        waitThenHideView.postDelayed( runnable, durationToDisappear );
        final Animation out = new AlphaAnimation( 1.0f, 0f );
        out.setDuration( durationToDisappear );
        LL.setAnimation( out );
    }

    private void disable_StatementButtons() {
        isDisabled_statementButtons = true;
        button_salutStatement.setAlpha(0.2f);
        button_caseStatement.setAlpha(0.2f);
    }

    private void enable_StatementButtons() {
        isDisabled_statementButtons = false;
        button_salutStatement.setAlpha(1f);
        button_caseStatement.setAlpha(1f);
    }

    private void explainMore() {
        textView_userGuide2.setVisibility(View.VISIBLE);
        textView_userGuide3.setVisibility(View.VISIBLE);
        textView_userGuide4.setVisibility(View.VISIBLE);
    }

    private boolean isPendingIntentCancelled = false;
    public void onPause() {
        super.onPause();
        if( mapSetup.pendingIntent != null ) { //can happen to be null
            mapSetup.pendingIntent.cancel();
            isPendingIntentCancelled = true;
        }
        socketConnection.destroyAllSockets();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        //There's another code, which may be better
        //https://stackoverflow.com/questions/4828636/edittext-clear-focus-on-touch-outside/28939113#28939113
        View w = getCurrentFocus();
        boolean ret = super.dispatchTouchEvent(event);
        //Log.i("Youssef", "dispatchTouchEvent");

        if (w != null ) {
            int scrcoords[] = new int[2];
            try{
                w.getLocationOnScreen(scrcoords);
            } catch( Exception e) {
                Log.i("Youssef", "dispatchTouchEvent getLocationOnScreen caused a Null Pointer exception to be caught");
                return ret;
            }
            float x = event.getRawX() + w.getLeft() - scrcoords[0];
            float y = event.getRawY() + w.getTop() - scrcoords[1];

            //Log.d("Activity", "Touch event "+event.getRawX()+","+event.getRawY()+" "+x+","+y+" rect "+w.getLeft()+","+w.getTop()+","+w.getRight()+","+w.getBottom()+" coords "+scrcoords[0]+","+scrcoords[1]);
            if( event.getAction() == MotionEvent.ACTION_DOWN &&
                    ( x < w.getLeft() || x >= w.getRight() || y < w.getTop() || y > w.getBottom() ) ) {
                if( w instanceof EditText ) {
                    //The following 2 lines hide the keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    try {
                        imm.hideSoftInputFromWindow(getWindow().getCurrentFocus().getWindowToken(), 0);
                    } catch (Exception e) {
                        Log.i("Youssef", "dispatchTouchEvent getWindowToken caused a Null Pointer exception to be caught");
                        return ret;
                    }
                    //you can make the cursor disappear by putting the following below without the need
                    //to struggle with xml file...
                    Log.i("Youssef", "Keyboard should be hidden in MainActivity");

                    editText_statement.clearFocus();
                    editText_userName.clearFocus();

                    saveUserNameChangeToPrefs( editText_userName );
                    return ret;
                }
                /*
                else if( w instanceof ImageButton ) { //does not work. dispatchTouchEvent does not get buttons. They get something else, like layouts maybe, IDK.
                    //Log.i("Youssef", "dispatchTouchEvent image button is clicked !");
                }
                */
            }
        }

        if( linearLayout_MarkerStatement != null && linearLayout_MarkerStatement.getVisibility() == View.VISIBLE &&
                !isTouched_linearLayout_MarkerStatement && !isTouched_imageButton_like &&
                !isTouched_imageButton_dislike ) { //Try later this condition   && !markersAction.isClickedMarkerInfoWindowShown()
            smoothDisappearance( linearLayout_MarkerStatement , new Runnable() { //better this should be made by the deamon service
                @SuppressLint("RestrictedApi")
                public void run() {
                    markersAction.dontShowCircleOfLastClickedMarker();
                    linearLayout_MarkerStatement.setVisibility( View.GONE );
                    fab_changeMap.setVisibility( View.VISIBLE );
                    fab_seeMyLocation.setVisibility( View.VISIBLE );
                }
            });
            Log.i("Youssef", "MarkerStatement must be gone now !");
        }
        isTouched_linearLayout_MarkerStatement = false;
        isTouched_imageButton_like = false;
        isTouched_imageButton_dislike = false;

        return ret;
    }

    void saveUserNameChangeToPrefs( EditText editText ) {
        SharedPreferences.Editor mEditor = mPrefs.edit();
        String name = editText.getText().toString();
        mEditor.putString("userName", name).apply();
    }

}


