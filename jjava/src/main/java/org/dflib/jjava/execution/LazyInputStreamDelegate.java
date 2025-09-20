package org.dflib.jjava.execution;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Supplier;

public class LazyInputStreamDelegate extends InputStream {
    private final Supplier<InputStream> readFrom;

    public LazyInputStreamDelegate(Supplier<InputStream> readFrom) {
        this.readFrom = readFrom;
    }

    @Override
    public int read() throws IOException {
        return this.readFrom.get().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.readFrom.get().read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return this.readFrom.get().read(b, off, len);
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        return this.readFrom.get().readAllBytes();
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        return this.readFrom.get().readNBytes(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return this.readFrom.get().skip(n);
    }

    @Override
    public int available() throws IOException {
        return this.readFrom.get().available();
    }

    @Override
    public void close() throws IOException {
        this.readFrom.get().close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        this.readFrom.get().mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        this.readFrom.get().reset();
    }

    @Override
    public boolean markSupported() {
        return this.readFrom.get().markSupported();
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        return this.readFrom.get().transferTo(out);
    }
}
