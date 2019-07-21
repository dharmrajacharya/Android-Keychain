package com.draj.org.keychainlib;

import android.content.Context;

/**
 * Created by Dharmraj Acharya on 21/7/19.
 */
public interface Keychain {

    boolean init(Context context);

    void setData(String key, byte[] data);

    byte[] getData(String key);

    void remove(String key);

    void removeAll();

    String getEncryptedData(String key);
}
