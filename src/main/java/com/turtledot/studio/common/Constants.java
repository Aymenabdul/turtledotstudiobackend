package com.turtledot.studio.common;

public final class Constants {
    private Constants() {}

    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";

    // Permission constants
    public static final String PERM_CLIENT_VIEW = "CLIENT_VIEW";
    public static final String PERM_CLIENT_EDIT = "CLIENT_EDIT";
    public static final String PERM_PHOTO_UPLOAD = "PHOTO_UPLOAD";
    public static final String PERM_PHOTO_DELETE = "PHOTO_DELETE";
    public static final String PERM_VIDEO_UPLOAD = "VIDEO_UPLOAD";
    public static final String PERM_VIDEO_DELETE = "VIDEO_DELETE";
    public static final String PERM_ALBUM_CREATE = "ALBUM_CREATE";
    public static final String PERM_ALBUM_EDIT = "ALBUM_EDIT";
    public static final String PERM_ALBUM_DELETE = "ALBUM_DELETE";
    public static final String PERM_MEDIA_DOWNLOAD = "MEDIA_DOWNLOAD";

    // Audit log actions
    public static final String ACTION_ADMIN_LOGIN = "ADMIN_LOGIN";
    public static final String ACTION_EMPLOYEE_LOGIN = "EMPLOYEE_LOGIN";
    public static final String ACTION_CLIENT_LOGIN = "CLIENT_LOGIN";
    public static final String ACTION_CREATE_CLIENT = "CREATE_CLIENT";
    public static final String ACTION_UPDATE_CLIENT = "UPDATE_CLIENT";
    public static final String ACTION_ARCHIVE_CLIENT = "ARCHIVE_CLIENT";
    public static final String ACTION_CREATE_EMPLOYEE = "CREATE_EMPLOYEE";
    public static final String ACTION_UPDATE_EMPLOYEE = "UPDATE_EMPLOYEE";
    public static final String ACTION_CREATE_EVENT = "CREATE_EVENT";
    public static final String ACTION_CREATE_ALBUM = "CREATE_ALBUM";
    public static final String ACTION_DELETE_ALBUM = "DELETE_ALBUM";
    public static final String ACTION_UPLOAD_MEDIA = "UPLOAD_MEDIA";
    public static final String ACTION_DELETE_MEDIA = "DELETE_MEDIA";
    public static final String ACTION_CREATE_PAYMENT = "CREATE_PAYMENT";
    public static final String ACTION_GENERATE_QR = "GENERATE_QR";
    public static final String ACTION_DOWNLOAD_MEDIA = "DOWNLOAD_MEDIA";
}
