plugins {
    id 'java'
    id 'application'
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}

group = 'com.mybot'
version = '1.0-SNAPSHOT'

repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.google.ai.client.generativeai:generativeai:0.8.3'
    implementation 'org.telegram:telegrambots:6.9.0'
    implementation 'com.google.code.gson:gson:2.11.0'
    implementation 'ch.qos.logback:logback-classic:1.5.12'
}

application {
    mainClass = 'com.mybot.Main'
}

shadowJar {
    archiveBaseName = 'fitness-bot'
    archiveVersion = '1.0-SNAPSHOT'
    archiveClassifier = ''
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}