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
import static com.example.curvedkeyboard.R.id.u;

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

    private ConstraintLayout customKeyboardView;
    private Point bottomRightCorner, bottomLeftCorner, topRightCorner, topLeftCorner, touchPoint;

    Keyboard keyboard;
    private boolean isCaps = false;
    public static boolean flag = false;


    // Id-evi textViewa koji su potrebni prilikom povecanja slova
    int[] ids = new int[]{R.id.q,R.id.w,R.id.e,R.id.r,R.id.t,R.id.y,R.id.u,R.id.i,R.id.o,R.id.p,
            R.id.a,R.id.s,R.id.d,R.id.f,R.id.g,R.id.h,R.id.j,R.id.k,R.id.l,
            R.id.z,R.id.x,R.id.c,R.id.v,R.id.b,R.id.n,R.id.m };

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
        initListeners();

        // Button space i done i njihove funkcionalnosti
        Button special = (Button) customKeyboardView.findViewById(R.id.special);
        Button space = (Button) customKeyboardView.findViewById(R.id.space);
        ImageButton enter = (ImageButton) customKeyboardView.findViewById(R.id.done);

        space.bringToFront();

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


        return customKeyboardView;
    }

    private void initListeners() {

        customKeyboardView.findViewById(R.id.right_frame).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x, y;
                x = (int) (screenWidth - (customKeyboardView.findViewById(R.id.right_frame).getWidth() * density - (int) event.getX()));
                y = (int) (screenHeight - (customKeyboardView.findViewById(R.id.right_frame).getHeight() * density - (int) event.getY()));
                touchPoint = new Point(x, y);

                int primaryCode = getRightKeyCode();
                InputConnection ic = getCurrentInputConnection();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        flag = true;
                        // Potrebna je nova dretva da se moze dugo brisati i da izade iz petlje
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (flag) {
                                    if(primaryCode == keyboard.KEYCODE_DELETE) {
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
                        break;

                    case MotionEvent.ACTION_UP:
                        flag = false;
                        if(primaryCode == keyboard.KEYCODE_DELETE) {
                            // Ovako samo jedan karakter brise
                            ic.commitText("", 0);
                        }else {
                            char code = (char) primaryCode;
                            if (Character.isLetter(code) && isCaps)
                                code = Character.toUpperCase(code);
                            ic.commitText(String.valueOf(code), 1);
                        }
                        break;

                }

                return true;
            }

        });

        customKeyboardView.findViewById(R.id.left_frame).setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x, y;
                x = (int) event.getX();
                y = (int) (screenHeight - (customKeyboardView.findViewById(R.id.left_frame).getHeight() * density - (int) event.getY()));
                touchPoint = new Point(x, y);

                int primaryCode = getLeftKeyCode();
                InputConnection ic = getCurrentInputConnection();

                if(primaryCode == Keyboard.KEYCODE_SHIFT){
                    isCaps = !isCaps;
                    ImageView caps = (ImageView) customKeyboardView.findViewById(R.id.caps);

                    // Provjera ako je shift ukljucen
                    // Ide po id-evima i povecava / smanjuje slova u layoutu
                    if(isCaps){
                        caps.setColorFilter(ContextCompat.getColor(customKeyboardView.getContext(), R.color.pink));

                        for(int id : ids){
                            TextView t = (TextView) customKeyboardView.findViewById(id);
                            String s = t.getText().toString();
                            t.setText(s.toUpperCase());
                        }
                    }else{
                        caps.setColorFilter(R.color.grey);
                        for(int id : ids){
                            TextView t = (TextView) customKeyboardView.findViewById(id);
                            String s = t.getText().toString();
                            t.setText(s.toLowerCase());
                        }
                    }

                    //customKeyboardView.invalidate();
                } else{
                    char code = (char) primaryCode;
                    if(Character.isLetter(code) && isCaps)
                        code = Character.toUpperCase(code);
                    ic.commitText(String.valueOf(code), 1);
                }

                return false;
            }
        });
    }

    public int getRightKeyCode(){
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

        return rightQwerty[i][j];

    }

    public int getLeftKeyCode(){
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

        return leftQwerty[i][j];
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