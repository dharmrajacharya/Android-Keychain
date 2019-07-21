package com.draj.org.keychaindemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.draj.org.keychainlib.Keychain;
import com.draj.org.keychainlib.KeychainStore;

public class MainActivity extends AppCompatActivity {

    EditText edName, edPassword;
    TextView txtInfo, encriptedInfo;
    Button btnSubmit;
    KeychainStore ks;
    public static String KEY_USER_NAME = "userName";
    public static String KEY_PASSWORD = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ks = new KeychainStore(this);

        edName = findViewById(R.id.userName);
        edPassword = findViewById(R.id.userPassword);
        btnSubmit = findViewById(R.id.btnSubmit);
        txtInfo = findViewById(R.id.txtInfo);
        encriptedInfo = findViewById(R.id.encriptedInfo);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userName = edName.getText().toString();
                String password = edPassword.getText().toString();

                if(!userName.isEmpty() && !password.isEmpty()){
                    ks.setData(KEY_USER_NAME, userName);
                    ks.setData(KEY_PASSWORD, password);

                    txtInfo.setText(String.format("%s : %s\n%s : %s",
                            KEY_USER_NAME, ks.getData(KEY_USER_NAME),
                            KEY_PASSWORD, ks.getData(KEY_PASSWORD)));

                    encriptedInfo.setText(String.format("%s : %s\n%s : %s",
                            KEY_USER_NAME, ks.getEncryptedData(KEY_USER_NAME),
                            KEY_PASSWORD, ks.getEncryptedData(KEY_PASSWORD)));
                }
            }
        });

    }
}
