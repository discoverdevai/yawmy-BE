package com.discoverdev.nms.chat;

public enum ChatThreadType {
    /** Two named users. Each side sees the other's name. */
    DIRECT,
    /** One room: its teachers, the guardians of its enrolled children, and every admin. */
    ROOM_GROUP,
    /** Every teacher and admin in the nursery. */
    STAFF_GROUP,
    /**
     * One teacher or parent talking to "the nursery", with every admin on the
     * other side. A nursery can have several admins, and a parent should not have
     * to guess which one to ask — so this is a single thread any admin can answer
     * rather than a separate DM per admin. It is keyed by the member, so admins
     * joining or leaving never splits the conversation in two.
     */
    ADMIN_SUPPORT
}
