package com.example.yoklamaceptever3

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class ParolaSifirla : AppCompatActivity() {
    private lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_parola_sifirla)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        var sifirla = findViewById<Button>(R.id.button2)
        var textmail = findViewById<TextView>(R.id.text6)
        var giris = findViewById<Button>(R.id.button5)
        var sifirlamatxt = findViewById<TextView>(R.id.textView8)

        sifirla.setOnClickListener{
            var emailsifirla = textmail.text.toString().trim()
            if (TextUtils.isEmpty(emailsifirla)){
                textmail.error ="Lütfen Emailinizi girin."
            }
            else{
                auth.sendPasswordResetEmail(emailsifirla)
                    .addOnCompleteListener(this) { sifirlama ->
                        if(sifirlama.isSuccessful){
                            sifirlamatxt.text = "Email adresinize sıfırlama bağlantısı gönderildi."
                        }
                        else{
                            sifirlamatxt.text ="Sıfırlama işlemi başarısız."
                        }

                    }
            }
        }
        giris.setOnClickListener{
            intent = Intent(this,GirisActivity::class.java)
            startActivity(intent)
        }

    }
}