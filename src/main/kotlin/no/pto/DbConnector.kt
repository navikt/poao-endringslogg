package no.pto

import no.pto.env.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*

fun sha256(userId: String): String {
    val bytes = userId.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("", { str, it -> str + "%02x".format(it) })
}

object Seen : Table("seen") {
    val userId = varchar("user_id", 255)
    val documentId = uuid("document_id")
    val appId = varchar("app_id", 255)
    val openedLink = bool("opened_link").default(false) // TODO: vurder å slett felter
    val openedModal = bool("opened_modal").default(false) // TODO: vurder å slett felter
    val timeStamp = timestamp("time_stamp")
    override val primaryKey = PrimaryKey(userId, documentId)
}

object SeenForced: Table("seen_forced") {
    val userId = varchar("user_id", 255)
    val documentId = uuid("document_id")
    override val primaryKey = PrimaryKey(userId, documentId)
}

fun connectToDatabase() {
    val connectUrl = "jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_DATABASE?reWriteBatchedInserts=true?sslmode=require"

    Database.connect(
        connectUrl,
        driver = "org.postgresql.Driver",
        user = DB_USERNAME,
        password = DB_PASSWORD
    )
}

fun getSeenEntriesForUser(userId: String): List<UUID> = transaction {
    Seen.select { Seen.userId eq sha256(userId) }.map { it[Seen.documentId] }
}

fun getSeenForcedEntriesForUser(userId: String): List<UUID> = transaction {
    SeenForced.select { SeenForced.userId eq sha256(userId) }.map { it[SeenForced.documentId] }
}

fun insertSeenEntries(userId: String, appId: String, documentIds: List<UUID>) = transaction {
    Seen.batchInsert(documentIds, ignore = true) { docId ->
        this[Seen.userId] = sha256(userId)
        this[Seen.appId] = appId
        this[Seen.documentId] = docId
        this[Seen.timeStamp] = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    }
}

fun insertSeenForcedEntries(userId: String, documentIds: List<UUID>) = transaction {
    SeenForced.batchInsert(documentIds, ignore = true) { docId ->
        this[SeenForced.userId] = sha256(userId)
        this[SeenForced.documentId] = docId
    }
}
