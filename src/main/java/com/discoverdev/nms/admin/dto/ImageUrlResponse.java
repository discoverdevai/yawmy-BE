package com.discoverdev.nms.admin.dto;

/** Returned after an image upload: the path the app loads (with the auth header). */
public record ImageUrlResponse(String imageUrl) {}
