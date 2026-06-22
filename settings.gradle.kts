pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AiChat"

include(":app")
include(":core:common")
include(":core:model")
include(":core:network")
include(":core:database")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":core:di")
include(":features:chat")
include(":features:chats")
include(":features:chat-create")
include(":features:models")
include(":features:settings")
include(":features:profile")
