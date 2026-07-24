package com.discoverdev.nms.admin.dto;

import java.util.List;
import java.util.UUID;

/** Link a set of children to a parent. */
public record LinkChildrenRequest(List<UUID> childIds) {}
