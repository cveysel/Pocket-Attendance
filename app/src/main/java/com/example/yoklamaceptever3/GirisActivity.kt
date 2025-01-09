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

class GirisActivity : AppCompatActivity() {
    lateinit var auth :FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_giris)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth =FirebaseAuth.getInstance()

        var email = findViewById<TextView>(R.id.text3)
        var parola = findViewById<TextView>(R.id.text4)
        var giris = findViewById<Button>(R.id.button3)
        var ekleme = findViewById<TextView>(R.id.ekleme)
        var sifirla = findViewById<TextView>(R.id.textView7)

        var current = auth.currentUser

        //sıfırlama butonuna tıklama
        sifirla.setOnClickListener{
            intent = Intent(this,ParolaSifirla::class.java)
            startActivity(intent)
        }

        //giriş butonuna tıklama
        giris.setOnClickListener{
            var girismail = email.text.toString()
            var girisparola = parola.text.toString()
            //ifadeler boş bırakılırsa
            if (TextUtils.isEmpty(girismail)){
                email.error = "Lütfen email adresi girin."
                return@setOnClickListener
            }
            else if (TextUtils.isEmpty(girisparola)){
                parola.error = "Lütfen parola girin."
                return@setOnClickListener
            }
            //giris doğrulama ve anasayfaya geçiş veya başarısız durumu
            auth.signInWithEmailAndPassword(girismail,girisparola)
                .addOnCompleteListener(this){
                    if (it.isSuccessful){
                        intent = Intent(applicationContext,AnaSayfa::class.java)
                        startActivity(intent)
                        finish() }
                }
        }
        // kayıt sayfasına gidiyor
        ekleme.setOnClickListener{
            intent = Intent(this,KayitActivity::class.java)
            startActivity(intent)
        }

    }
}