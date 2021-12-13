package com.example.curvedkeyboard;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Point;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;

import androidx.constraintlayout.helper.widget.CircularFlow;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import static com.example.curvedkeyboard.R.drawable.*;
import static com.example.curvedkeyboard.R.id.caps;
import static com.example.curvedkeyboard.R.id.circularFlow_right;
import static com.example.curvedkeyboard.R.id.custom;
import static com.example.curvedkeyboard.R.id.letter;
import static com.example.curvedkeyboard.R.id.popup_preview;
import static com.example.curvedkeyboard.R.id.u;
import static com.example.curvedkeyboard.R.id.visible;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
    private int[][] rightQwerty =
            {{121, 117, 105, 111, 112},     // y u i o p
                    {104, 106, 107, 108, 44},       // h j k l ,
                    {0,98,110,109,46}};            // b n m .

    private int[][] leftQwerty =
            {{113, 119, 101, 114, 116},     // q w e r t
                    {97, 115, 100, 102, 103},        // a s d f g
                    {122, 120, 99, 118, 0}};           // z x c v

    private int screenWidth, screenHeight;
    private float density;
    private Point bottomRightCorner, bottomLeftCorner, topRightCorner, topLeftCorner, touchPoint;

    private ConstraintLayout customKeyboardView;
    private ConstraintLayout popupPreview;
    private Button special, space;
    private ImageButton enter;
    private TextView previewLetter;

    Keyboard keyboard;
    private boolean isCaps = false;
    public static boolean isDelPressed = false;
    char code = 0;
    public static boolean rightIsLocked = false;
    public static boolean leftIsLocked = false;

    // Id-evi textViewa koji su potrebni prilikom povecanja slova
    int[] ids = new int[]{R.id.q,R.id.w,R.id.e,R.id.r,R.id.t,R.id.y,R.id.u,R.id.i,R.id.o,R.id.p,
            R.id.a,R.id.s,R.id.d,R.id.f,R.id.g,R.id.h,R.id.j,R.id.k,R.id.l,
            R.id.z,R.id.x,R.id.c,R.id.v,R.id.b,R.id.n,R.id.m };

    private int[][] leftLetters =
            {{R.id.q,R.id.w,R.id.e,R.id.r,R.id.t},     // q w e r t
                    {R.id.a,R.id.s,R.id.d,R.id.f,R.id.g},        // a s d f g
                    {R.id.z,R.id.x,R.id.c,R.id.v, 0}};           // z x c v
    private int[][] rightLetters =
            {{R.id.y,R.id.u,R.id.i,R.id.o,R.id.p},     // y u i o p
                    {R.id.h,R.id.j,R.id.k,R.id.l, R.id.comma},       // h j k l ,
                    {0,R.id.b,R.id.n,R.id.m,R.id.period}};            // b n m .

    @Override
    public void onInitializeInterface() {
        super.onInitializeInterface();
    }

    @Override
    public View onCreateInputView() {
        // sirina, visina i rezolucija ekrana
        screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        density = getResources().getDisplayMetrics().density;

        // inicijalizacija krajnjih tocaka
        bottomRightCorner = new Point(screenWidth, screenHeight);
        bottomLeftCorner = new Point(0, screenHeight);
        topRightCorner = new Point(screenWidth, 0);
        topLeftCorner = new Point(0, 0);

        customKeyboardView = (ConstraintLayout) getLayoutInflater().inflate(R.layout.custom_keyboard_view, null);

        initViews();
        initListeners();

        return customKeyboardView;
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    private void initViews(){
        special = (Button) customKeyboardView.findViewById(R.id.special);
        space = (Button) customKeyboardView.findViewById(R.id.space);
        enter = (ImageButton) customKeyboardView.findViewById(R.id.done);
        popupPreview = (ConstraintLayout) customKeyboardView.findViewById(popup_preview);
        previewLetter = (TextView) customKeyboardView.findViewById(R.id.letter);

        space.bringToFront();
        popupPreview.bringToFront();
        popupPreview.setX(0);
        popupPreview.setY(0);
    }

    private void initListeners() {

        space.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputConnection ic = getCurrentInputConnection();
                ic.commitText(" ",1);
            }
        });

        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputConnection ic = getCurrentInputConnection();
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            }
        });

        customKeyboardView.findViewById(R.id.right_frame).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(!rightIsLocked){
                    InputConnection ic = getCurrentInputConnection();

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_MOVE:
                            leftIsLocked = true;
                            space.setClickable(false);
                            enter.setClickable(false);
                            special.setClickable(false);

                            int x, y;
                            x = (int) (screenWidth - (customKeyboardView.findViewById(R.id.right_frame).getWidth() * density - (int) event.getX()));
                            y = (int) (screenHeight - (customKeyboardView.findViewById(R.id.right_frame).getHeight() * density - (int) event.getY()));
                            touchPoint = new Point(x, y);

                            int textViewId = getRightKeyId();

                            if(textViewId == Keyboard.KEYCODE_DELETE && event.getAction() == MotionEvent.ACTION_DOWN) {
                                isDelPressed = true;
                                // Potrebna je nova dretva da se moze dugo brisati i da izade iz petlje
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        while (isDelPressed) {
                                            if(textViewId == keyboard.KEYCODE_DELETE) {
                                                CharSequence selectedText = ic.getSelectedText(0);
                                                if (TextUtils.isEmpty(selectedText)) {
                                                    ic.deleteSurroundingText(1, 0);
                                                } else {
                                                    ic.commitText("", 1);
                                                }
                                            }
                                            try {
                                                // Dretva ide na spavanje da del ne bude prebrz
                                                Thread.sleep(200);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }).start();
                                return true;
                            }
                            else if (textViewId > 0){
                                isDelPressed = false;
                                TextView textView = (TextView) customKeyboardView.findViewById(textViewId);
                                String s = textView.getText().toString();
                                code = (char) s.charAt(0);

                                previewLetter.setText(s);
                                popupPreview.setX(textView.getX() - (popupPreview.getWidth()/2) + (textView.getWidth()/2));
                                popupPreview.setY(textView.getY() - popupPreview.getHeight() + textView.getHeight());
                                popupPreview.setVisibility(View.VISIBLE);
                                return true;
                            } else {
                                code = 0;
                                popupPreview.setVisibility(View.GONE);
                                leftIsLocked = false;
                                space.setClickable(true);
                                enter.setClickable(true);
                                special.setClickable(true);
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            isDelPressed = false;
                            if(code == keyboard.KEYCODE_DELETE) {
                                // Ovako samo jedan karakter brise
                                ic.commitText("", 0);
                            } else {
                                if (code != 0) ic.commitText(String.valueOf(code), 1);
                                popupPreview.setVisibility(View.GONE);
                                code = 0;
                            }
                            leftIsLocked = false;
                            space.setClickable(true);
                            enter.setClickable(true);
                            special.setClickable(true);
                            break;
                    }
                }

                return false;
            }

        });

        customKeyboardView.findViewById(R.id.left_frame).setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(!leftIsLocked){
                    InputConnection ic = getCurrentInputConnection();

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_MOVE:
                            rightIsLocked = true;
                            space.setClickable(false);
                            enter.setClickable(false);
                            special.setClickable(false);

                            int x, y;
                            x = (int) event.getX();
                            y = (int) (screenHeight - (customKeyboardView.findViewById(R.id.left_frame).getHeight() * density - (int) event.getY()));
                            touchPoint = new Point(x, y);

                            int textViewId = getLeftKeyId();

                            if(textViewId == Keyboard.KEYCODE_SHIFT && event.getAction() == MotionEvent.ACTION_DOWN) {
                                isCaps = !isCaps;
                                ImageView caps = (ImageView) customKeyboardView.findViewById(R.id.caps);

                                // Provjera ako je shift ukljucen
                                // Ide po id-evima i povecava / smanjuje slova u layoutu
                                if (isCaps) {
                                    caps.setColorFilter(ContextCompat.getColor(customKeyboardView.getContext(), R.color.pink));
                                    for (int id : ids) {
                                        TextView t = (TextView) customKeyboardView.findViewById(id);
                                        String s = t.getText().toString();
                                        t.setText(s.toUpperCase());
                                    }
                                } else {
                                    caps.setColorFilter(R.color.grey);
                                    for (int id : ids) {
                                        TextView t = (TextView) customKeyboardView.findViewById(id);
                                        String s = t.getText().toString();
                                        t.setText(s.toLowerCase());
                                    }
                                }
                                rightIsLocked = false;
                                space.setClickable(true);
                                enter.setClickable(true);
                                special.setClickable(true);
                            }
                            else if (textViewId > 0){
                                TextView textView = (TextView) customKeyboardView.findViewById(textViewId);
                                String s = textView.getText().toString();
                                code = (char) s.charAt(0);

                                previewLetter.setText(s);
                                popupPreview.setX(textView.getX() - (popupPreview.getWidth()/2) + (textView.getWidth()/2));
                                popupPreview.setY(textView.getY() - popupPreview.getHeight() + textView.getHeight());
                                popupPreview.setVisibility(View.VISIBLE);
                                return true;
                            } else {
                                code = 0;
                                popupPreview.setVisibility(View.GONE);
                                rightIsLocked = false;
                                space.setClickable(true);
                                enter.setClickable(true);
                                special.setClickable(true);
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                            if(code != 0) ic.commitText(String.valueOf(code), 1);
                            popupPreview.setVisibility(View.GONE);
                            code = 0;
                            rightIsLocked = false;
                            space.setClickable(true);
                            enter.setClickable(true);
                            special.setClickable(true);
                            break;
                    }
                }

                return false;
            }
        });
    }

    public int getRightKeyId(){
        // radijus
        double disAC = Math.sqrt(Math.pow(bottomRightCorner.x - touchPoint.x, 2) + Math.pow(bottomRightCorner.y - touchPoint.y, 2));

        // kut
        double angle = (double) Math.toDegrees(Math.atan2(topRightCorner.y-bottomRightCorner.y, topRightCorner.x-bottomRightCorner.x))
                - Math.toDegrees(Math.atan2(touchPoint.y-bottomRightCorner.y, touchPoint.x-bottomRightCorner.x));

        int i = 0, j = 0;

        if(disAC <= 146*density && touchPoint.y > 73) return -5; // DEL
        if(disAC <= 365*density && disAC > 292*density){
            i = 0;
            if(angle >= 10 && angle < 20) j = 4;
            else if(angle >= 20 && angle < 30) j = 3;
            else if(angle >= 30 && angle < 40) j = 2;
            else if(angle >= 40 && angle < 50) j = 1;
            else if(angle >= 50 && angle < 60) j = 0;
            else return 0;
        }
        else if (disAC <= 292*density && disAC > 219*density){
            i = 1;
            if(angle >= 0 && angle < 12) j = 4;
            else if(angle >= 12 && angle < 24) j = 3;
            else if(angle >= 24 && angle < 36) j = 2;
            else if(angle >= 36 && angle < 48) j = 1;
            else if(angle >= 48 && angle < 60) j = 0;
            else return 0;
        }
        else if (disAC <= 219*density && disAC > 146*density){
            i = 2;
            if(angle >= 0 && angle < 16) j = 4;
            else if(angle >= 16 && angle < 32) j = 3;
            else if(angle >= 32 && angle < 48) j = 2;
            else if(angle >= 48 && angle < 64) j = 1;
            else return 0;
        }
        else return 0;

        return rightLetters[i][j];

    }

    public int getLeftKeyId(){
        // radijus
        double disAC = Math.sqrt(Math.pow(bottomLeftCorner.x - touchPoint.x, 2) + Math.pow(bottomLeftCorner.y - touchPoint.y, 2));

        // kut
        double angle = (double) Math.toDegrees(Math.atan2(touchPoint.y-bottomLeftCorner.y, touchPoint.x-bottomLeftCorner.x))
                - Math.toDegrees(Math.atan2(topLeftCorner.y-bottomLeftCorner.y, topLeftCorner.x-bottomLeftCorner.x));

        int i = 0, j = 0;

        if(disAC <= 146*density && touchPoint.y > 73) return -1; // DEL
        if(disAC <= 365*density && disAC > 292*density){
            i = 0;
            if(angle >= 0 && angle < 10) j = 0;
            else if(angle >= 10 && angle < 20) j = 1;
            else if(angle >= 20 && angle < 30) j = 2;
            else if(angle >= 30 && angle < 40) j = 3;
            else if(angle >= 40 && angle < 50) j = 4;
            else return 0;
        }
        else if (disAC <= 292*density && disAC > 219*density){
            i = 1;
            if(angle >= 0 && angle < 12) j = 0;
            else if(angle >= 12 && angle < 24) j = 1;
            else if(angle >= 24 && angle < 36) j = 2;
            else if(angle >= 36 && angle < 48) j = 3;
            else if(angle >= 48 && angle < 60) j = 4;
            else return 0;
        }
        else if (disAC <= 219*density && disAC > 146*density){
            i = 2;
            if(angle >= 0 && angle < 16) j = 0;
            else if(angle >= 16 && angle < 32) j = 1;
            else if(angle >= 32 && angle < 48) j = 2;
            else if(angle >= 48 && angle < 64) j = 3;
            else return 0;
        }
        else return 0;

        return leftLetters[i][j];
    }


    @Override
    public void onKey(int primaryCode, int[] keyCodes) { }

    @Override
    public void onPress(int primaryCode) { }

    @Override
    public void onRelease(int primaryCode) { }

    @Override
    public void onText(CharSequence text) { }

    @Override
    public void swipeLeft() { }

    @Override
    public void swipeRight() { }

    @Override
    public void swipeDown() { }

    @Override
    public void swipeUp() { }


}