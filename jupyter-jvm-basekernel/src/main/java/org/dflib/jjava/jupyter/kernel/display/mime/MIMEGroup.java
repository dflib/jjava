package org.dflib.jjava.jupyter.kernel.display.mime;

import java.util.Objects;

public class MIMEGroup {
    public enum Type {
        APPLICATION,
        AUDIO,
        EXAMPLE,
        FONT,
        IMAGE,
        MESSAGE,
        MODEL,
        MULTIPART,
        TEXT,
        VIDEO,
        OTHER;

        private final String groupName;

        Type() {
            this.groupName = this.name().toLowerCase();
        }

        public String groupName() {
            return this.groupName;
        }

        @Override
        public String toString() {
            return this.groupName;
        }
    }

    public static final MIMEGroup APPLICATION = new MIMEGroup(Type.APPLICATION);
    public static final MIMEGroup AUDIO = new MIMEGroup(Type.AUDIO);
    public static final MIMEGroup EXAMPLE = new MIMEGroup(Type.EXAMPLE);
    public static final MIMEGroup FONT = new MIMEGroup(Type.FONT);
    public static final MIMEGroup IMAGE = new MIMEGroup(Type.IMAGE);
    public static final MIMEGroup MESSAGE = new MIMEGroup(Type.MESSAGE);
    public static final MIMEGroup MODEL = new MIMEGroup(Type.MODEL);
    public static final MIMEGroup MULTIPART = new MIMEGroup(Type.MULTIPART);
    public static final MIMEGroup TEXT = new MIMEGroup(Type.TEXT);
    public static final MIMEGroup VIDEO = new MIMEGroup(Type.VIDEO);

    public static MIMEGroup of(String name) {
        switch (name.toLowerCase()) {
            case "application":
                return APPLICATION;
            case "audio":
                return AUDIO;
            case "example":
                return EXAMPLE;
            case "font":
                return FONT;
            case "image":
                return IMAGE;
            case "message":
                return MESSAGE;
            case "model":
                return MODEL;
            case "multipart":
                return MULTIPART;
            case "text":
                return TEXT;
            case "video":
                return VIDEO;
            default:
                return new MIMEGroup(name);
        }
    }

    private final String name;
    private final Type type;

    private MIMEGroup(Type type) {
        this.name = type.groupName();
        this.type = type;
    }

    private MIMEGroup(String other) {
        this.name = other;
        this.type = Type.OTHER;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MIMEGroup mimeGroup = (MIMEGroup) o;
        return (this.type == mimeGroup.type && this.type != Type.OTHER)
                || Objects.equals(this.name, mimeGroup.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
