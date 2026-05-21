package com.example.chatterapp.data

object NetworkConfig {
    private const val PROTOKOL = "https://"
    private const val DOMEN = "nikiclab01.tailfd4e2c.ts.net"
    // DODAT JE /php/ FOLDER KOJI SE VIDI NA TVOJOJ SLICI
    private const val PUTANJA = "/php/chatter-app-3.0/"

    const val BASE_URL = PROTOKOL + DOMEN + PUTANJA

    // Funkcije koje spajaju bazu sa tvojim PHP skriptama sa slike
    fun getGroupsUrl(username: String): String = "${BASE_URL}api_groups.php?action=list&username=$username"
    fun getChatUrl(groupId: Int): String = "${BASE_URL}api_chat.php?group_id=$groupId"

    // NAPUŠTANJE I BRISANJE GRUPE
    // apiji su u api_group.

    // Funkcija za dobijanje tačnog URL-a za označavanje poruka kao pročitanih
    fun getSeenUrl(): String = "${BASE_URL}api_seen.php"

    // Rute za usere u grupama i kikovanje
    fun getMembersUrl(groupId: Int): String = "${BASE_URL}api_groups.php?action=members&group_id=$groupId"
    fun getMembersApiUrl(): String = "${BASE_URL}api_groups.php"

    // dodavanje ljudi u grupe
    fun getAddMemberApiUrl(): String = "${BASE_URL}api_groups.php"

    // Ruta za sugestiju dodavanja korisnika:

    fun getSearchUsersUrl(groupId: Int, query: String): String = "${BASE_URL}api_groups.php?action=search_users&group_id=$groupId&query=$query"


}
