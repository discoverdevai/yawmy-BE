package com.discoverdev.nms.media.dto;

import java.util.List;

public record BulkUploadResponse(
    List<MediaItemResponse> uploaded,
    List<FailedUpload> failed
) {
    public record FailedUpload(String filename, String reason) {}
}
