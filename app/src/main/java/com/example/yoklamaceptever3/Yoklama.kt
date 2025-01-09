package com.example.yoklamaceptever3
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.location.LocationManager
import android.location.LocationRequest
import android.provider.Settings
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class Yoklama : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private var currentUserUID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_yoklama)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        currentUserUID = FirebaseAuth.getInstance().currentUser?.uid

        val yoklamayaKatil = findViewById<Button>(R.id.button_participate)
        val yoklamaBaslat = findViewById<Button>(R.id.button10)
        val yoklamaBitir = findViewById<Button>(R.id.button14)
        val YoklamaDurumum = findViewById<Button>(R.id.YoklamaDurumum)

        val button_past_attendances = findViewById<Button>(R.id.button_past_attendances)


        val clickAnimation = AnimationUtils.loadAnimation(this, R.anim.anim_slide_down)
        yoklamaBitir.startAnimation(clickAnimation)
        yoklamaBaslat.startAnimation(clickAnimation)
        yoklamayaKatil.startAnimation(clickAnimation)
        button_past_attendances.startAnimation(clickAnimation)
        YoklamaDurumum.startAnimation(clickAnimation)


        YoklamaDurumum.setOnClickListener{
            loadKatilimDurumu()
        }

        yoklamaBaslat.setOnClickListener {
            if (checkAndRequestLocationPermission()) {
                currentUserUID?.let { uid ->
                    startAttendance(uid)
                }
            }
        }

        yoklamaBitir.setOnClickListener {
            if (checkAndRequestLocationPermission()) {
                currentUserUID?.let { uid ->
                    endAttendance(uid)
                }
            }
        }

        yoklamayaKatil.setOnClickListener {

            // Mock location
            checkMockLocationAndProceed(this) { isMock ->
                if (isMock) {
                    Toast.makeText(this, "Sahte konum tespit edildi. İşlem başarısız.", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, AnaSayfa::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish() }
                else {
                    if (checkAndRequestLocationPermission()) {
                        currentUserUID?.let { uid ->
                            joinAttendance(uid) }
                    } } } }


        button_past_attendances.setOnClickListener{
            loadDersler()
        }

    }

    private fun checkAndRequestLocationPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            false
        } else {
            true
        }
    }

    private fun startAttendance(uid: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("profile")
        databaseReference.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userID = snapshot.key
                    if (userID != null) {
                        db.collection("Classes").whereEqualTo("ownerId", userID).get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    val classList = mutableListOf<String>()
                                    val classIdList = mutableListOf<String>()

                                    for (document in documents) {
                                        val className =
                                            document.getString("className") ?: "Bilinmeyen Ders"
                                        classList.add(className)
                                        classIdList.add(document.id)
                                    }

                                    val builder = AlertDialog.Builder(this@Yoklama)
                                    builder.setTitle("Yoklama almak istediğiniz dersi seçin")
                                    builder.setItems(classList.toTypedArray()) { _, which ->
                                        val selectedClassId = classIdList[which]
                                        if (!isGpsEnabled()) {
                                            // GPS kapalıysa kullanıcıya uyarı ver
                                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                            startActivity(intent)
                                            return@setItems
                                        }

                                        db.collection("Classes").document(selectedClassId)
                                            .update("accession", true)
                                            .addOnSuccessListener {
                                                Toast.makeText(
                                                    this@Yoklama,
                                                    "${classList[which]} dersinin yoklaması başlatıldı.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                        getUserLocation { latitude, longitude ->
                                            Log.d("LocationDebug", "Latitude: $latitude, Longitude: $longitude")
                                            val locationData = hashMapOf(
                                                "latitude" to latitude,
                                                "longitude" to longitude,
                                                "timestamp" to System.currentTimeMillis()
                                            )
                                            db.collection("Classes").document(selectedClassId)
                                                .collection("Locations").document(userID)
                                                .set(locationData)
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        this@Yoklama,
                                                        "${classList[which]} için güncel konum kaydedildi",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e(
                                                        "Firestore",
                                                        "Güncel konum kaydetme hatası: ${e.message}"
                                                    )
                                                    Toast.makeText(
                                                        this@Yoklama,
                                                        "Güncel konum kaydedilirken hata oluştu",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                    }

                                    builder.setNegativeButton("Kapat") { dialog, _ -> dialog.dismiss() }
                                    builder.create().show()
                                } else {
                                    Toast.makeText(
                                        this@Yoklama,
                                        "Hiç ders bulunamadı",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firestore", "Veri alma hatası: ${exception.message}")
                                Toast.makeText(
                                    this@Yoklama,
                                    "Veri alınırken hata oluştu",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        Toast.makeText(
                            this@Yoklama,
                            "Kullanıcı ID'si bulunamadı",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this@Yoklama, "Veri bulunamadı", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Veri okuma hatası: ${error.message}")
                Toast.makeText(this@Yoklama, "Veri okunurken hata oluştu", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun endAttendance(uid: String) {
        db.collection("Classes")
            .whereEqualTo("ownerId", uid)
            .whereEqualTo("accession", true)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val classList = mutableListOf<String>()
                    val classIdList = mutableListOf<String>()

                    for (document in documents) {
                        val className = document.getString("className") ?: "Bilinmeyen Ders"
                        classList.add(className)
                        classIdList.add(document.id)
                    }

                    val builder = AlertDialog.Builder(this@Yoklama)
                    builder.setTitle("Açık Olan Dersler")
                    builder.setItems(classList.toTypedArray()) { _, which ->
                        val selectedClassId = classIdList[which]

                        // Ders erişimini kapat
                        db.collection("Classes").document(selectedClassId)
                            .update("accession", false)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@Yoklama,
                                    "${classList[which]} dersi kapatıldı",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Kurucunun konum verisini sil
                                db.collection("Classes").document(selectedClassId)
                                    .collection("Locations").document(uid)
                                    .delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this@Yoklama,
                                            "Kurucunun konum verisi silindi.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(
                                            "Firestore",
                                            "Konum verisi silme hatası: ${e.message}"
                                        )
                                        Toast.makeText(
                                            this@Yoklama,
                                            "Konum verisi silinirken hata oluştu",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "accession güncelleme hatası: ${e.message}")
                                Toast.makeText(
                                    this@Yoklama,
                                    "Ders kapatılamadı",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    builder.setNegativeButton("Kapat") { dialog, _ -> dialog.dismiss() }
                    builder.create().show()
                } else {
                    Toast.makeText(this@Yoklama, "Açık ders bulunamadı", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Veri alma hatası: ${exception.message}")
                Toast.makeText(this@Yoklama, "Veri alınırken hata oluştu", Toast.LENGTH_SHORT)
                    .show()
            }
    }
    private fun joinAttendance(uid: String) {
        db.collection("Classes")
            .whereEqualTo("accession", true)
            .whereArrayContains("members", uid)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val classList = mutableListOf<String>()
                    val classIdList = mutableListOf<String>()

                    for (document in documents) {
                        val className = document.getString("className") ?: "Bilinmeyen Ders"
                        classList.add(className)
                        classIdList.add(document.id)
                    }

                    val builder = AlertDialog.Builder(this@Yoklama)
                    builder.setTitle("Katılmak istediğiniz dersi seçin")
                    builder.setItems(classList.toTypedArray()) { _, which ->
                        val selectedClassId = classIdList[which]

                        if (!isGpsEnabled()) {
                            // GPS kapalıysa kullanıcıya uyarı ver
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            startActivity(intent)
                            return@setItems
                        }

                        // Ders sahibinin konum bilgilerini çekiyoruz
                        db.collection("Classes").document(selectedClassId)
                            .get()
                            .addOnSuccessListener { classDocument ->
                                val ownerId = classDocument.getString("ownerId")

                                db.collection("Classes").document(selectedClassId)
                                    .collection("Locations").document(ownerId ?: "Kurucu")
                                    .get()
                                    .addOnSuccessListener { locationDocument ->
                                        val ownerLatitude = locationDocument.getDouble("latitude")
                                        val ownerLongitude = locationDocument.getDouble("longitude")

                                        if (ownerLatitude != null && ownerLongitude != null) {
                                            // Kullanıcının konumunu al ve mesafe kontrolü yap
                                            getUserLocation { latitude, longitude ->
                                                val distance = FloatArray(1)
                                                android.location.Location.distanceBetween(
                                                    ownerLatitude,
                                                    ownerLongitude,
                                                    latitude,
                                                    longitude,
                                                    distance
                                                )

                                                val distanceInMeters = distance[0]
                                                val toleranceDistance = 15 // Mesafe sınırı (metre cinsinden)
                                                if (distanceInMeters <= toleranceDistance) {
                                                    // Katılım verilerini tarih bazında kaydetme işlemi
                                                    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                                                    // Katılımcı verisi
                                                    val participantData = hashMapOf(
                                                        "userId" to uid,
                                                        "latitude" to latitude,
                                                        "longitude" to longitude,
                                                        "timestamp" to System.currentTimeMillis()
                                                    )

                                                    // Yoklamalar koleksiyonuna katılım ekleme
                                                    val yoklamaRef = db.collection("Classes")
                                                        .document(selectedClassId)
                                                        .collection("Yoklamalar")
                                                        .document(todayDate) // Tarihi üst belge olarak kullanıyoruz

                                                    yoklamaRef.get()
                                                        .addOnSuccessListener { yoklamaDoc ->
                                                            if (yoklamaDoc.exists()) {
                                                                // Mevcut yoklamaya katılımcıyı ekle
                                                                yoklamaRef.update("participants", FieldValue.arrayUnion(participantData))
                                                                    .addOnSuccessListener {
                                                                        Toast.makeText(
                                                                            this@Yoklama,
                                                                            "${classList[which]} için katılım başarılı",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                                    .addOnFailureListener { e ->
                                                                        Log.e("Firestore", "Katılım ekleme hatası: ${e.message}")
                                                                        Toast.makeText(
                                                                            this@Yoklama,
                                                                            "Katılım kaydedilirken hata oluştu",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                            } else {
                                                                // Yeni yoklama oluştur ve katılımcıyı ekle
                                                                val yoklamaData = hashMapOf(
                                                                    "date" to todayDate,
                                                                    "participants" to listOf(participantData)
                                                                )
                                                                yoklamaRef.set(yoklamaData)
                                                                    .addOnSuccessListener {
                                                                        Toast.makeText(
                                                                            this@Yoklama,
                                                                            "${classList[which]} için katılım başarılı",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                                    .addOnFailureListener { e ->
                                                                        Log.e("Firestore", "Yoklama kaydetme hatası: ${e.message}")
                                                                        Toast.makeText(
                                                                            this@Yoklama,
                                                                            "Yoklama kaydedilirken hata oluştu",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                            }
                                                        }
                                                } else {
                                                    // Kullanıcı belirlenen mesafeden uzakta
                                                    Toast.makeText(
                                                        this@Yoklama,
                                                        "Katılım sınırları dışında konumda bulunuyorsunuz",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(
                                                this@Yoklama,
                                                "Dersin konum bilgileri alınamıyor",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firestore", "Konum verisi alma hatası: ${e.message}")
                                        Toast.makeText(
                                            this@Yoklama,
                                            "Konum verisi alınırken hata oluştu",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Ders verisi alma hatası: ${e.message}")
                                Toast.makeText(
                                    this@Yoklama,
                                    "Ders verisi alınırken hata oluştu",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    builder.setNegativeButton("Kapat") { dialog, _ -> dialog.dismiss() }
                    builder.create().show()
                } else {
                    Toast.makeText(this@Yoklama, "Açık ders bulunamadı", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Veri alma hatası: ${exception.message}")
                Toast.makeText(this@Yoklama, "Veri alınırken hata oluştu", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }


    private fun getUserLocation(onLocationRetrieved: (Double, Double) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    onLocationRetrieved(it.latitude, it.longitude)
                }
            }.addOnFailureListener { e ->
                Log.e("Location", "Konum alınamadı: ${e.message}")
                Toast.makeText(this@Yoklama, "Konum alınırken hata oluştu", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(this@Yoklama, "Konum izni verilmemiş", Toast.LENGTH_SHORT).show()
        }
    }
    //***************************************************************************************************************************************

    fun loadDersler() {
        val db = FirebaseFirestore.getInstance()
        val currentUserUID = FirebaseAuth.getInstance().currentUser?.uid
        val dersList = mutableListOf<Pair<String, String>>() // Ders ID ve Adı

        if (currentUserUID == null) {
            Log.e("Firebase", "Kullanıcı kimliği alınamadı.")
            return
        }

        db.collection("Classes")
            .whereEqualTo("ownerId", currentUserUID)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { document ->
                    val dersId = document.id
                    val dersAdi = document.getString("className") ?: "Bilinmiyor"
                    dersList.add(Pair(dersId, dersAdi))
                }

                if (dersList.isNotEmpty()) {
                    showDersSecimDialog(dersList)
                } else {
                    Toast.makeText(this, "Hiç dersiniz bulunmuyor.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Dersler yüklenemedi: ${exception.message}")
                Toast.makeText(this, "Dersler yüklenirken bir hata oluştu.", Toast.LENGTH_SHORT).show()
            }
    }

    fun showDersSecimDialog(dersList: List<Pair<String, String>>) {
        val dersAdlari = dersList.map { it.second }.toTypedArray()

        AlertDialog.Builder(this).apply {
            setTitle("Bir ders seçin")
            setItems(dersAdlari) { _, which ->
                val selectedDers = dersList[which]
                showTarihSecim(selectedDers.first)
            }
            setNegativeButton("İptal") { dialog, _ -> dialog.dismiss() }
            create().show()
        }
    }

    fun showTarihSecim(dersId: String) {
        val db = FirebaseFirestore.getInstance()
        val tarihList = mutableListOf<String>()

        db.collection("Classes").document(dersId).collection("Yoklamalar")
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { document ->
                    tarihList.add(document.id)
                }

                if (tarihList.isNotEmpty()) {
                    showTarihSecimDialog(dersId, tarihList)
                } else {
                    Toast.makeText(this, "Bu ders için kayıtlı tarih yok.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Tarihler yüklenemedi: ${exception.message}")
                Toast.makeText(this, "Tarihler yüklenirken bir hata oluştu.", Toast.LENGTH_SHORT).show()
            }
    }

    fun showTarihSecimDialog(dersId: String, tarihList: List<String>) {
        val tarihler = tarihList.toTypedArray()

        AlertDialog.Builder(this).apply {
            setTitle("Bir tarih seçin")
            setItems(tarihler) { _, which ->
                val selectedTarih = tarihList[which]
                fetchKatilanVeKatilmayanlar(dersId, selectedTarih)
            }
            setNegativeButton("İptal") { dialog, _ -> dialog.dismiss() }
            create().show()
        }
    }

    fun fetchKatilanVeKatilmayanlar(dersId: String, tarih: String) {
        val db = FirebaseFirestore.getInstance()
        val realtimeDb = FirebaseDatabase.getInstance().getReference("profile")
        val katilanIds = mutableListOf<String>()
        val katilmayanIds = mutableListOf<String>()

        db.collection("Classes").document(dersId).collection("Yoklamalar").document(tarih)
            .get()
            .addOnSuccessListener { yoklamaSnapshot ->
                val participants = yoklamaSnapshot.get("participants") as? List<Map<String, Any>> ?: emptyList()
                val katilanlar = participants.map { it["userId"].toString() }

                db.collection("Classes").document(dersId)
                    .get()
                    .addOnSuccessListener { classSnapshot ->
                        val members = classSnapshot.get("members") as? List<String> ?: emptyList()

                        members.forEach { memberId ->
                            if (katilanlar.contains(memberId)) {
                                katilanIds.add(memberId)
                            } else {
                                katilmayanIds.add(memberId)
                            }
                        }

                        fetchUserNamesAndShowDialog(realtimeDb, katilanIds, katilmayanIds, tarih)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Katılım verileri alınamadı: ${exception.message}")
            }
    }

    fun fetchUserNamesAndShowDialog(
        realtimeDb: DatabaseReference,
        katilanIds: List<String>,
        katilmayanIds: List<String>,
        tarih: String
    ) {
        val katilanIsimler = mutableListOf<String>()
        val katilmayanIsimler = mutableListOf<String>()
        val totalFetchCount = katilanIds.size + katilmayanIds.size
        var completedFetchCount = 0

        val checkAndShowDialog = {
            completedFetchCount++
            if (completedFetchCount == totalFetchCount) {
                showYoklamaDialog(katilanIsimler, katilmayanIsimler, tarih)
            }
        }

        katilanIds.forEach { userId ->
            realtimeDb.child(userId).get()
                .addOnSuccessListener { snapshot ->
                    katilanIsimler.add(snapshot.child("adivesoyadi").value?.toString() ?: "Bilinmiyor")
                    checkAndShowDialog()
                }
                .addOnFailureListener { checkAndShowDialog() }
        }

        katilmayanIds.forEach { userId ->
            realtimeDb.child(userId).get()
                .addOnSuccessListener { snapshot ->
                    katilmayanIsimler.add(snapshot.child("adivesoyadi").value?.toString() ?: "Bilinmiyor")
                    checkAndShowDialog()
                }
                .addOnFailureListener { checkAndShowDialog() }
        }
    }

    fun showYoklamaDialog(
        katilanIsimler: List<String>,
        katilmayanIsimler: List<String>,
        tarih: String
    ) {
        val builder = AlertDialog.Builder(this)
        val katilanlarText = "Katılanlar:\n" + katilanIsimler.joinToString("\n")
        val katilmayanlarText = "Katılmayanlar:\n" + katilmayanIsimler.joinToString("\n")
        val message = "$katilanlarText\n\n$katilmayanlarText"

        builder.setTitle("Yoklama Sonuçları ($tarih)")
            .setMessage(message)
            .setPositiveButton("Tamam") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

//******************************************************************************************************************

    fun loadKatilimDurumu() {
        val db = FirebaseFirestore.getInstance()
        val currentUserUID = FirebaseAuth.getInstance().currentUser?.uid
        val dersKatilimList = mutableListOf<Pair<String, String>>() // Ders adı ve oranı

        if (currentUserUID == null) {
            Log.e("Firebase", "Kullanıcı kimliği alınamadı.")
            return
        }

        // Kullanıcının üyesi olduğu sınıfları sorgula
        db.collection("Classes")
            .whereArrayContains("members", currentUserUID)
            .get()
            .addOnSuccessListener { classesSnapshot ->
                val totalDersCount = classesSnapshot.documents.size
                if (totalDersCount == 0) {
                    Toast.makeText(this, "Hiç üye olduğunuz ders bulunamadı.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                var processedDersCount = 0

                for (ders in classesSnapshot.documents) {
                    val dersId = ders.id
                    val dersAdi = ders.getString("className") ?: "Bilinmiyor"

                    db.collection("Classes").document(dersId).collection("Yoklamalar")
                        .get()
                        .addOnSuccessListener { yoklamalarSnapshot ->
                            val toplamYoklama = yoklamalarSnapshot.documents.size
                            var katilimSayisi = 0

                            yoklamalarSnapshot.documents.forEach { yoklama ->
                                val participants = yoklama.get("participants") as? List<Map<String, Any>> ?: emptyList()
                                if (participants.any { it["userId"] == currentUserUID }) {
                                    katilimSayisi++
                                }
                            }

                            val oran = if (toplamYoklama > 0) {
                                "${katilimSayisi}/${toplamYoklama}"
                            } else {
                                "Katılım yok"
                            }
                            dersKatilimList.add(Pair(dersAdi, oran))

                            processedDersCount++
                            if (processedDersCount == totalDersCount) {
                                showKatilimDurumuDialog(dersKatilimList)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("Firebase", "Yoklamalar alınamadı: ${exception.message}")
                            processedDersCount++
                            if (processedDersCount == totalDersCount) {
                                showKatilimDurumuDialog(dersKatilimList)
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Dersler alınamadı: ${exception.message}")
                Toast.makeText(this, "Dersler yüklenirken bir hata oluştu.", Toast.LENGTH_SHORT).show()
            }
    }

    fun showKatilimDurumuDialog(dersKatilimList: List<Pair<String, String>>) {
        val builder = AlertDialog.Builder(this)
        val message = dersKatilimList.joinToString("\n") { "${it.first}: ${it.second}" }

        builder.setTitle("Katılım Durumu")
            .setMessage(message)
            .setPositiveButton("Tamam") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun checkMockLocationAndProceed(
        context: Context,
        callback: (Boolean) -> Unit
    ) {
        val LOCATION_PERMISSION_REQUEST_CODE = 1001

        // İzin kontrolü
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            callback(false) // İzin verilmedi
            return
        }
        //konum hizmetlerine erişim için bir andoid apisi
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Kullanıcıdan konum al
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && location.isFromMockProvider) {
                callback(true)
            } else {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }
}
