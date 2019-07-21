package com.draj.org.keychainlib.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyChain;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.draj.org.keychainlib.Keychain;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

/**
 * Created by Dharmraj Acharya on 21/7/19.
 */
public class KeyChain_SDK18 implements Keychain {

    private static final String TAG = "Keychain";
    private SharedPreferences pref;
    private String alias = null;

    @Override
    public boolean init(Context context) {
        pref = context.getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE);
        alias = context.getApplicationContext().getPackageName();

        KeyStore ks;

        try {
            ks = KeyStore.getInstance(Constants.KEYSTORE_PROVIDER);

            //Use null to load Keystore with default parameters.
            ks.load(null);

            // Check if Private and Public already keys exists. If so we don't need to generate them again
            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, null);
            if (privateKey != null && ks.getCertificate(alias) != null) {
                PublicKey publicKey = ks.getCertificate(alias).getPublicKey();
                if (publicKey != null) {
                    // All keys are available.
                    return true;
                }
            }
        } catch (Exception ex) {
            return false;
        }

        // Create a start and end time, for the validity range of the key pair that's about to be
        // generated.
        Calendar start = new GregorianCalendar();
        Calendar end = new GregorianCalendar();
        end.add(Calendar.YEAR, 10);

        // Specify the parameters object which will be passed to KeyPairGenerator
        AlgorithmParameterSpec spec;
        if (android.os.Build.VERSION.SDK_INT < 23) {
            spec = new android.security.KeyPairGeneratorSpec.Builder(context)
                    // Alias - is a key for your KeyPair, to obtain it from Keystore in future.
                    .setAlias(alias)
                    // The subject used for the self-signed certificate of the generated pair
                    .setSubject(new X500Principal("CN=" + alias))
                    // The serial number used for the self-signed certificate of the generated pair.
                    .setSerialNumber(BigInteger.valueOf(1337))
                    // Date range of validity for the generated pair.
                    .setStartDate(start.getTime()).setEndDate(end.getTime())
                    .build();
        } else {
            spec = new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_DECRYPT)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .build();
        }

        // Initialize a KeyPair generator using the the intended algorithm (in this example, RSA
        // and the KeyStore. This example uses the AndroidKeyStore.
        KeyPairGenerator kpGenerator;
        try {
            kpGenerator = KeyPairGenerator.getInstance(Constants.TYPE_RSA, Constants.KEYSTORE_PROVIDER);
            kpGenerator.initialize(spec);
            // Generate private/public keys
            kpGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
            try {
                ks.deleteEntry(alias);
            } catch (Exception e1) {
                // Just ignore any errors here
            }
        }

        // Check if device support Hardware-backed keystore
        try {
            boolean isHardwareBackedKeystoreSupported;
            if (Build.VERSION.SDK_INT < 23) {
                isHardwareBackedKeystoreSupported = KeyChain.isBoundKeyAlgorithm(KeyProperties.KEY_ALGORITHM_RSA);
            } else {
                PrivateKey privateKey = (PrivateKey) ks.getKey(alias, null);
                KeyChain.isKeyAlgorithmSupported(KeyProperties.KEY_ALGORITHM_RSA);
                KeyFactory keyFactory = KeyFactory.getInstance(privateKey.getAlgorithm(), Constants.KEYSTORE_PROVIDER);
                KeyInfo keyInfo = keyFactory.getKeySpec(privateKey, KeyInfo.class);
                isHardwareBackedKeystoreSupported = keyInfo.isInsideSecureHardware();
            }
            Log.d(TAG, "Hardware-Backed Keystore Supported: " + isHardwareBackedKeystoreSupported);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | InvalidKeySpecException | NoSuchProviderException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void setData(String key, byte[] data) {
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance(Constants.KEYSTORE_PROVIDER);

            ks.load(null);
            if (ks.getCertificate(alias) == null) return;

            PublicKey publicKey = ks.getCertificate(alias).getPublicKey();

            if (publicKey == null) {
                Log.d(TAG, "Error: Public key was not found in Keystore");
                return;
            }

            String value = encrypt(publicKey, data);

            SharedPreferences.Editor editor = pref.edit();
            editor.putString(key, value);
            editor.apply();
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException
                | KeyStoreException | CertificateException | IOException e) {
            try {
                if (ks != null)
                    ks.deleteEntry(alias);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public byte[] getData(String key) {
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance(Constants.KEYSTORE_PROVIDER);
            ks.load(null);
            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, null);
            return decrypt(privateKey, pref.getString(key, null));
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
                | UnrecoverableEntryException | InvalidKeyException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException e) {
            try {
                if (ks != null)
                    ks.deleteEntry(alias);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return null;
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

    @Override
    public String getEncryptedData(String key) {
        return  pref.getString(key, "");
    }

    private static String encrypt(PublicKey encryptionKey, byte[] data) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        Cipher cipher = Cipher.getInstance(Constants.RSA_ECB_PKCS1_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        byte[] encrypted = cipher.doFinal(data);
        return Base64.encodeToString(encrypted, Base64.DEFAULT);
    }

    private static byte[] decrypt(PrivateKey decryptionKey, String encryptedData) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if (encryptedData == null)
            return null;
        byte[] encryptedBuffer = Base64.decode(encryptedData, Base64.DEFAULT);
        Cipher cipher = Cipher.getInstance(Constants.RSA_ECB_PKCS1_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, decryptionKey);
        return cipher.doFinal(encryptedBuffer);
    }
}
