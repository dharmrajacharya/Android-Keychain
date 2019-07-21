package com.draj.org.keychainlib;

import android.content.Context;
import android.util.Log;
import com.draj.org.keychainlib.helper.KeyChain_SDK18;

/**
 * Created by Dharmraj Acharya on 21/7/19.
 */
public class KeychainStore {

    private static String TAG = "KeychainStore";

    private Keychain keychain;

    public KeychainStore(Context context) {

        keychain = new KeyChain_SDK18();

        boolean isInitialized = false;

        try {
            isInitialized = keychain.init(context);
        } catch (Exception ex) {
            Log.e(TAG, "KeychainStore initialisation error:" + ex.getMessage(), ex);
        }

        if (!isInitialized && keychain instanceof KeyChain_SDK18) {
            keychain = new KeyChain_SDK18();
            keychain.init(context);
        }
    }


    public void setData(String key, String data) {
        keychain.setData(key, data.getBytes());
    }

    public String getData(String key) {
        return new String(keychain.getData(key));
    }

    public void remove(String key) {
        keychain.remove(key);
    }

    public void removeAll() {
        keychain.removeAll();
    }

    public String getEncryptedData(String key){
        return keychain.getEncryptedData(key);
    }
}
