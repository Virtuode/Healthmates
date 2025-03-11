package com.corps.healthmate.models


class User {
    var name: String? = null
        private set
    var profileImageUrl: String? = null
        private set

    constructor()

    constructor(name: String?, profileImageUrl: String?) {
        this.name = name
        this.profileImageUrl = profileImageUrl
    }
}
