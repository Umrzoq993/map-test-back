package com.agri.mapapp.common;

import lombok.Getter;

public final class ImageSniffer {
    private ImageSniffer() {}

    @Getter
    public enum Type {
        JPEG("image/jpeg", ".jpg"),
        PNG("image/png", ".png"),
        GIF("image/gif", ".gif"),
        WEBP("image/webp", ".webp");
        private final String contentType; private final String ext;
        Type(String ct, String e){ this.contentType=ct; this.ext=e; }
    }

    public static Type sniff(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;
        // JPEG FF D8
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) return Type.JPEG;
        // PNG 89 50 4E 47 0D 0A 1A 0A
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1]=='P' && bytes[2]=='N' && bytes[3]=='G') return Type.PNG;
        // GIF 47 49 46 38
        if (bytes[0]=='G' && bytes[1]=='I' && bytes[2]=='F' && bytes[3]=='8') return Type.GIF;
        // WEBP: RIFF....WEBP
        if (bytes[0]=='R' && bytes[1]=='I' && bytes[2]=='F' && bytes[3]=='F' && bytes[8]=='W' && bytes[9]=='E' && bytes[10]=='B' && bytes[11]=='P') return Type.WEBP;
        return null;
    }
}

