package com.discoverdev.nms.media;

public enum MediaType {
    PHOTO,
    VIDEO,
    DOCUMENT,
    // Voice notes recorded in chat. Carries durationMs so the player can draw
    // its progress bar before the audio itself has been fetched.
    AUDIO
}
