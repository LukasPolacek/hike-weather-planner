package cz.polacek.hikeweather.feature.backup

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import cz.polacek.hikeweather.core.prefs.UserPreferences
import cz.polacek.hikeweather.feature.trips.data.TripRepository
import kotlinx.coroutines.tasks.await

/**
 * Rucni zaloha a obnoveni vyletu ve Firestore
 * struktura: users/{uid} (jmeno, cas zalohy) a users/{uid}/trips/{idVyletu}
 * pravidla Firestore povoli cist a zapisovat jen vlastni data prihlaseneho uzivatele
 */
class BackupRepository(
    private val tripRepository: TripRepository,
    private val preferences: UserPreferences
) {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private fun userDocument() = firestore.collection("users").document(
        auth.currentUser?.uid ?: error("Nejste přihlášeni")
    )

    /** nahraje vsechny lokalni vylety a smaze z cloudu ty, ktere uz lokalne nejsou; vraci pocet vyletu */
    suspend fun backup(): Result<Int> = runCatching {
        val user = userDocument()
        val tripsCollection = user.collection("trips")
        val trips = tripRepository.getTrips()
        val localIds = trips.map { it.id }.toSet()
        val cloudIds = tripsCollection.get().await().documents.map { it.id }
        val now = System.currentTimeMillis()

        val batch = firestore.batch()
        trips.forEach { batch.set(tripsCollection.document(it.id), TripBackupMapper.toMap(it)) }
        cloudIds.filterNot { it in localIds }.forEach { batch.delete(tripsCollection.document(it)) }
        batch.set(user, mapOf("name" to preferences.userName.value, "backedUpAt" to now, "tripCount" to trips.size))
        batch.commit().await()

        preferences.setLastBackupAt(now)
        trips.size
    }

    /** stahne vylety ze zalohy a sloucit je s lokalnimi (stejne id se prepise); vraci pocet vyletu */
    suspend fun restore(): Result<Int> = runCatching {
        val user = userDocument()
        val documents = user.collection("trips").get().await().documents
        val trips = documents.mapNotNull { doc -> doc.data?.let { TripBackupMapper.fromMap(doc.id, it) } }
        tripRepository.saveAll(trips)

        // jmeno ze zalohy, pokud ho uzivatel jeste nema
        val info = user.get().await()
        info.getString("name")?.takeIf { it.isNotBlank() && preferences.userName.value.isNullOrBlank() }
            ?.let { preferences.setUserName(it) }
        info.getLong("backedUpAt")?.let { preferences.setLastBackupAt(it) }
        trips.size
    }
}
