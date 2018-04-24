package com.icmdigerati.wybe;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class LoginActivity extends AppCompatActivity {

    private ImageView ivBckg;
    private boolean isLoginPage = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ivBckg = findViewById(R.id.ivBckg);
        ivBckg.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(isLoginPage) {
                    ivBckg.setImageDrawable(getResources().getDrawable(R.drawable.registeractivity));
                    isLoginPage = false;
                }
                else {
                    ivBckg.setImageDrawable(getResources().getDrawable(R.drawable.loginactivity));
                    isLoginPage = true;
                }
            }
        });
    }
}
