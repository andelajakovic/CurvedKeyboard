package com.example.curvedkeyboard;

import android.content.res.Resources;
import android.graphics.Point;
import android.util.DisplayMetrics;

public class CoordinatesCalculation {
    int[][] rightKeyCodes =
            {{121, 117, 105, 111, 112},     // y u i o p
            {104, 106, 107, 108, 44},       // h j k l ,
            {98,98,110,109,46}};            // b n m .

    int[][] leftKeyCodes =
            {{113, 119, 101, 114, 116},     // q w e r t
            {97, 115, 100, 102, 103},        // a s d f g
            {-1,122,120,99,118}};           // CAPS z x c v

    Point A = new Point(); // ishodiste polukruznice
    Point B = new Point(); // krajnja desna/krajnja lijeva tocka
    Point C = new Point(); // tocka dodira

    public CoordinatesCalculation(Point a, Point b, Point c) {
        A = a;
        B = b;
        C = c;
    }

    public int getRightKeyCode(float density){

            double disAC = Math.sqrt(Math.pow(A.x - C.x, 2) + Math.pow(A.y - C.y, 2));

            if(disAC <= 125) return -5;

            //double angle = Math.acos(((disAB*disAB) + (disAC*disAC) + (disBC*disBC)) / (2 * disAB * disAC));
            double angle = (double) Math.toDegrees(Math.atan2(B.y-A.y, B.x-A.x)) - Math.toDegrees(Math.atan2(C.y-A.y, C.x-A.x));
            int i = 0, j = 0;

            if(disAC > 292*density) i = 0;
            else if (disAC <= 292*density && disAC > 219*density) i = 1;
            else if (disAC <= 219*density && disAC > 146*density) i = 2;
            else if (disAC <= 146*density) return rightKeyCodes[2][0];

            if(angle < 15) j = 4;
            else if(angle >= 15 && angle < 30) j = 3;
            else if(angle >= 30 && angle < 45) j = 2;
            else if(angle >= 45 && angle < 60) j = 1;
            else if(angle >= 60 && angle < 75) j = 0;

             return rightKeyCodes[i][j];

    }
}
