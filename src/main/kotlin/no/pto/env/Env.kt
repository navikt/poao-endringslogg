package no.pto.env

import java.net.URLEncoder
import java.nio.charset.Charset

val DB_USERNAME: String = System.getenv("DB_USERNAME")
val DB_PASSWORD: String = System.getenv("DB_PASSWORD")
val DB_HOST: String = System.getenv("DB_HOST")
val DB_DATABASE: String = System.getenv("DB_DATABASE")
val DB_PORT: Int = System.getenv("DB_PORT").toInt()
val SANITY_PROJECT_ID: String = "li581mqu"

fun erIDev() = System.getenv("NAIS_CLUSTER_NAME") == "dev-gcp"
fun erIProd() = System.getenv("NAIS_CLUSTER_NAME") == "prod-gcp"

val systemmeldingPoaoQ1Query = URLEncoder.encode("*[_type=='alert_overiskten'][0...100][publisert_q1]", Charset.forName("utf-8"))
val systemmeldingPoaoProdQuery = URLEncoder.encode("*[_type=='alert_overiskten'][0...100][publisert]", Charset.forName("utf-8"))

val endringsloggPoaoQ1Query = URLEncoder.encode("*[_type=='afolg'][0...100]", Charset.forName("utf-8"))
val endringsloggPoaoProdQuery = URLEncoder.encode("*[_type=='afolg'][0...100][publisert]", Charset.forName("utf-8"))


fun getSystemmeldingPoaoQuery(): String = if (erIProd()) systemmeldingPoaoProdQuery else systemmeldingPoaoQ1Query
fun getEndringsloggPoaoQuery(): String = if (erIProd()) endringsloggPoaoProdQuery else endringsloggPoaoQ1Query
