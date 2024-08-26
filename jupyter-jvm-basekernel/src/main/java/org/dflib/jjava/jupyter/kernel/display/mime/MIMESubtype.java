package org.dflib.jjava.jupyter.kernel.display.mime;

public class MIMESubtype {
    public static class Tree {
        public static final Tree VENDOR = new Tree("vnd");
        public static final Tree PERSONAL = new Tree("prs");
        public static final Tree UNREGISTERED = new Tree("x");

        public static Tree of(String name) {
            switch (name.toLowerCase()) {
                case "vnd": return VENDOR;
                default:
                    return new Tree(name);
            }
        }

        private final String name;

        private Tree(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName() + ".";
        }
    }

    public enum Application {
        JSON,
        XML,

    }
}
