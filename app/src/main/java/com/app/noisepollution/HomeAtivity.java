package com.app.noisepollution;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HomeAtivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        Button btn_main = findViewById(R.id.btn);
        btn_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeAtivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}


//    Button btn = findViewById(R.id.btn_back);
//        btn.setOnClickListener(new View.OnClickListener() {
//@Override
//public void onClick(View view) {
////                thoát khỏi ativity trở về main
//        finish();
//
//        }
//        });