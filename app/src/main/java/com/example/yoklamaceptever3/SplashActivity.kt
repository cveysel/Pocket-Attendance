package com.example.yoklamaceptever3

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Fake GPS kontrolü
        val isMockEnabled = isMockLocationEnabled(this)
        if (isMockEnabled) {
            Toast.makeText(this, "Sahte konum tespit edildi!", Toast.LENGTH_LONG).show()
            // Sahte konum bulunduysa ana ekrana geçme, kullanıcıyı uyar
            Handler(Looper.getMainLooper()).postDelayed({
                finish() // Uygulamayı kapat
            }, 3000)
        } else {
            // Sahte konum yok, giriş ekranına geç
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, GirisActivity::class.java)
                startActivity(intent)
                finish()
            }, 3000)
        }
    }
    private fun isMockLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)

        for (provider in providers) {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null && location.isFromMockProvider) {
                return true
            }
        }
        return false
    }
}
