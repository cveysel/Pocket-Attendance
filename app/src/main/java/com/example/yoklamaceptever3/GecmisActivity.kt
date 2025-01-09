import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.yoklamaceptever3.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GecmisActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var listView: ListView
    private lateinit var dersList: MutableList<Class>
    private lateinit var dersAdiList: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gecmis)

        // Firebase ve ListView referanslarını alalım
        db = FirebaseFirestore.getInstance()
        listView = findViewById(R.id.lessonListView)
        dersList = mutableListOf()
        dersAdiList = mutableListOf()

        // Dersleri yükleyelim
        gecmis()
    }

    // Kullanıcının kurduğu dersleri çekip ListView'de gösterme
    fun gecmis() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        db.collection("Classes")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                dersList.clear()
                dersAdiList.clear()

                for (document in querySnapshot.documents) {
                    val dersId = document.id
                    val dersAdi = document.getString("className") ?: "Bilinmiyor"
                    dersList.add(Class(dersId, dersAdi))
                    dersAdiList.add(dersAdi)
                }

                // Ders isimlerini göstermek için ArrayAdapter kullanıyoruz
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    dersAdiList
                )
                listView.adapter = adapter

                // ListView'deki öğeye tıklandığında
                listView.setOnItemClickListener { _, _, position, _ ->
                    val selectedClass = dersList[position]
                    Toast.makeText(this, "Seçilen Ders: ${selectedClass.className}", Toast.LENGTH_SHORT).show()

                    // Seçilen dersin yoklama tarihlerini alalım
                    getYoklamaTarihleri(selectedClass.classId)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Dersler alınamadı: ${exception.message}")
                Toast.makeText(this, "Dersler alınamadı", Toast.LENGTH_SHORT).show()
            }
    }

    // Seçilen derse göre yoklama tarihlerini listeleme
    private fun getYoklamaTarihleri(dersId: String) {
        db.collection("Classes").document(dersId).collection("Yoklamalar")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val tarihList = mutableListOf<String>()
                for (document in querySnapshot.documents) {
                    tarihList.add(document.id) // Yoklama tarihi olarak document ID'yi kullanıyoruz
                }

                if (tarihList.isNotEmpty()) {
                    Toast.makeText(this, "Yoklama Tarihleri: $tarihList", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Yoklama tarihi bulunamadı", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Tarihler alınamadı: ${exception.message}")
                Toast.makeText(this, "Tarihler alınamadı", Toast.LENGTH_SHORT).show()
            }
    }

    data class Class(
        val classId: String,
        val className: String
    )
}
