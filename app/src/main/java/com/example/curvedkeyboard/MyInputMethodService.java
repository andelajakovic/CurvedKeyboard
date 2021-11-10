package com.example.curvedkeyboard;

import android.content.res.Resources;
import android.graphics.Point;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;

import androidx.constraintlayout.helper.widget.CircularFlow;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

import static com.example.curvedkeyboard.R.id.custom;
import static com.example.curvedkeyboard.R.id.u;

public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
    ConstraintLayout customKeyboardView;
    Point A, B, C;

    private Keyboard keyboard;
    private boolean isCaps = false;


    @Override
    public View onCreateInputView() {
        // get the KeyboardView and add our Keyboard layout to it
        /*KeyboardView keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard_view, null);
        Keyboard keyboard = new Keyboard(this, R.xml.number_pad);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);*/

        customKeyboardView = (ConstraintLayout) getLayoutInflater().inflate(R.layout.custom_keyboard_view, null);


        customKeyboardView.findViewById(R.id.right_frame).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x, y;

                int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
                int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

                A = new Point(screenWidth, screenHeight);

                Resources r = getResources();
                float density = r.getDisplayMetrics().density;

                x = (int) (screenWidth - (customKeyboardView.findViewById(R.id.right_frame).getWidth() * density - (int) event.getX()));
                y = (int) (screenHeight - (customKeyboardView.findViewById(R.id.space_frame).getHeight() * density + customKeyboardView.findViewById(R.id.right_frame).getHeight() * density - (int) event.getY()));
                C = new Point(x, y);

                y = (int) customKeyboardView.findViewById(R.id.right_frame).getHeight() + customKeyboardView.findViewById(R.id.space_frame).getHeight();
                B = new Point(screenWidth, y);


                CoordinatesCalculation cc = new CoordinatesCalculation(A, B, C);

                int primaryCode = cc.getRightKeyCode(density);
                InputConnection ic = getCurrentInputConnection();

                char code = (char) primaryCode;

                ic.commitText(String.valueOf(code), 1);

                onPress(primaryCode);
                onRelease(primaryCode);

                return false;
            }
        });

        return customKeyboardView;
    }


    @Override
    public void onKey(int primaryCode, int[] keyCodes) {

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                CharSequence selectedText = ic.getSelectedText(0);
                if (TextUtils.isEmpty(selectedText)) {
                    // no selection, so delete previous character
                    ic.deleteSurroundingText(1, 0);
                } else {
                    // delete the selection
                    ic.commitText("", 1);
                }
                break;
            case Keyboard.KEYCODE_SHIFT:
                isCaps = !isCaps;
                keyboard.setShifted(isCaps);
                customKeyboardView.invalidate();
                break;
            default:
                char code = (char) primaryCode;
                if(Character.isLetter(code) && isCaps)
                    code = Character.toUpperCase(code);
                ic.commitText(String.valueOf(code), 1);
        }
    }

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
