package com.riv0trill.spriter;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static SharedPreferences get(Context c) { return c.getSharedPreferences("spriter", Context.MODE_PRIVATE); }
}
