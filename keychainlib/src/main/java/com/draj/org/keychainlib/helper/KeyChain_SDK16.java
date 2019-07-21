package com.draj.org.keychainlib.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.draj.org.keychainlib.Keychain;

/**
 * Created by Dharmraj Acharya on 21/7/19.
 */
public class KeyChain_SDK16 implements Keychain {

    private SharedPreferences pref;

    @Override
    public boolean init(Context context) {
        pref = context.getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE);
        return true;
    }

    @Override
    public void setData(String key, byte[] data) {
        if (data == null)
            return;
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, Base64.encodeToString(data, Base64.DEFAULT));
        editor.apply();
    }

    @Override
    public byte[] getData(String key) {
        String res = pref.getString(key, null);
        if (res == null)
            return null;
        return Base64.decode(res, Base64.DEFAULT);
    }

    public String getEncryptedData(String key) {
        return  pref.getString(key, "");
    }

    @Override
    public void remove(String key) {
        SharedPreferences.Editor editor = pref.edit();
        editor.remove(key);
        editor.apply();
    }

    @Override
    public void removeAll() {
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.apply();
    }
}
