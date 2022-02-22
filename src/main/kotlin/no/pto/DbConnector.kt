package no.pto

import no.pto.env.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.JavaLocalDateColumnType
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
    val openedLink = bool("opened_link").default(false)
    val openedModal = bool("opened_modal").default(false)
    val timeStamp = timestamp("time_stamp")
    override val primaryKey = PrimaryKey(userId, documentId)
}

object SeenForced: Table("seen_forced") {
    val userId = varchar("user_id", 255)
    val documentId = uuid("document_id")
    override val primaryKey = PrimaryKey(userId, documentId)
}

object UserSession : Table("user_session") {
    val userId = varchar("user_id", 255)
    val appId = varchar("app_id", 255)
    val duration = integer("duration")
    val unseenFields = integer("unseen_fields")
    val timeStamp = timestamp("time_stamp")
    override val primaryKey = PrimaryKey(userId, timeStamp)
}

fun connectToDatabase() {

    val connectUrl: String = "jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_DATABASE?reWriteBatchedInserts=true?sslmode=require"

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


fun setModalOpen(docId: String) = transaction {
    Seen.update({ Seen.documentId eq UUID.fromString(docId) }) {
        it[Seen.openedModal] = true
    }
}

fun setLinkClicked(docId: String) = transaction {
    Seen.update({ Seen.documentId eq UUID.fromString(docId) }) {
        it[Seen.openedLink] = true
    }
}

fun insertSessionDuration(session: SessionDuration) = transaction {

    UserSession.insert {
        it[UserSession.userId] = sha256(session.userId)
        it[UserSession.appId] = session.appId
        it[UserSession.duration] = session.duration
        it[UserSession.unseenFields] = session.unseenFields
        it[UserSession.timeStamp] = Instant.now().truncatedTo(ChronoUnit.SECONDS)
    }
}

fun getAllEntriesInSeen() = transaction {
    Seen.selectAll()
        .map { SeenDataClass(it[Seen.userId], it[Seen.documentId].toString(),it[Seen.openedLink],it[Seen.openedModal], it[Seen.timeStamp].toString()) }
}

fun getAllEntriesInUserSessions(appId: String) = transaction {
    UserSession.select{ UserSession.appId eq appId }
        .map { UserSessionClass(it[UserSession.userId], it[UserSession.appId], it[UserSession.duration], it[UserSession.unseenFields], it[UserSession.timeStamp].toString())}
}

fun getSeenEntriesForDocId(docId: String) = transaction {
    Seen.select { Seen.documentId eq UUID.fromString(docId) }
        .map { SeenDataClass(it[Seen.userId], it[Seen.documentId].toString(),it[Seen.openedLink],it[Seen.openedModal], it[Seen.timeStamp].toString()) }
}
fun getSeenEntriesForAppId(appId: String) = transaction {
    Seen.select { Seen.appId eq appId }
        .map { SeenDataClass(it[Seen.userId], it[Seen.documentId].toString(),it[Seen.openedLink],it[Seen.openedModal], it[Seen.timeStamp].toString()) }
}

/*
The sql equivalent of the Exposed query:
SELECT DISTINCT CAST(user_session.time_stamp AS DATE), COUNT(user_session.user_id)
FROM user_session WHERE (user_session.app_id = appId) AND (user_session.duration > moreThanMs)
GROUP BY CAST(user_session.time_stamp AS DATE)
ORDER BY CAST(user_session.time_stamp AS DATE) ASC
 */
fun getUniqueVisitorsPerDayForAppId(appId: String, moreThanMs: Int, lessThanMs: Int) = transaction {
    UserSession
        .slice(UserSession.timeStamp.castTo<LocalDate>(JavaLocalDateColumnType()), UserSession.userId.count())
        .select { (UserSession.appId eq appId) and (UserSession.duration greaterEq moreThanMs) and (UserSession.duration lessEq lessThanMs ) }
        .groupBy(UserSession.timeStamp.castTo<LocalDate>(JavaLocalDateColumnType()))
        .orderBy(UserSession.timeStamp.castTo<LocalDate>(JavaLocalDateColumnType()) to SortOrder.ASC)
        .withDistinct().map {
            UniqueSessionsPerDay(it[UserSession.timeStamp.castTo<LocalDate>(JavaLocalDateColumnType())].toString(), it[UserSession.userId.count()]) // .substring(0,10)
        }
}
