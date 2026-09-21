import { Kernel, KernelMessage } from '@jupyterlab/services';

/**
 * Owns the <canvas> shown in a notebook output. It draws the PNG frames streamed by the kernel and forwards
 * mouse/keyboard events back over the comm.
 */
export class CanvasController {
  readonly element: HTMLDivElement;

  private readonly canvas: HTMLCanvasElement;
  private readonly ctx: CanvasRenderingContext2D;
  private comm: Kernel.IComm | null = null;
  private buttonsDown = 0;
  private pendingMove: MouseEvent | null = null;

  constructor(width: number, height: number) {
    this.element = document.createElement('div');
    this.element.className = 'jjava-swing';

    this.canvas = document.createElement('canvas');
    this.canvas.width = width;
    this.canvas.height = height;
    this.canvas.tabIndex = 0; // focusable, so it receives key events
    this.canvas.className = 'jjava-swing-canvas';
    this.element.appendChild(this.canvas);

    const ctx = this.canvas.getContext('2d');
    if (!ctx) {
      throw new Error('2D canvas context unavailable');
    }
    this.ctx = ctx;

    this.wireEvents();
  }

  /** Bind the kernel comm and start receiving frames. */
  attachComm(comm: Kernel.IComm): void {
    this.comm = comm;
    comm.onMsg = (msg: KernelMessage.ICommMsgMsg) => this.onCommMsg(msg);
    comm.onClose = () => {
      this.comm = null;
    };
  }

  dispose(): void {
    if (this.comm && !this.comm.isDisposed) {
      try {
        this.comm.close();
      } catch {
        /* ignore */
      }
    }
    this.comm = null;
  }

  private onCommMsg(msg: KernelMessage.ICommMsgMsg): void {
    const data = msg.content.data as { type?: string; w?: number; h?: number };
    if (!data || data.type !== 'frame' || !msg.buffers || msg.buffers.length === 0) {
      return;
    }
    const buffer = msg.buffers[0];
    const bytes = buffer instanceof ArrayBuffer ? buffer : buffer.buffer;
    this.drawFrame(bytes as ArrayBuffer, data.w ?? this.canvas.width, data.h ?? this.canvas.height);
  }

  private drawFrame(buffer: ArrayBuffer, w: number, h: number): void {
    const blob = new Blob([buffer], { type: 'image/png' });
    createImageBitmap(blob)
      .then(bitmap => {
        if (this.canvas.width !== w || this.canvas.height !== h) {
          this.canvas.width = w;
          this.canvas.height = h;
        }
        this.ctx.clearRect(0, 0, w, h);
        this.ctx.drawImage(bitmap, 0, 0);
        bitmap.close();
      })
      .catch(() => {
        /* ignore decode errors */
      });
  }

  private send(payload: Record<string, unknown>): void {
    if (this.comm && !this.comm.isDisposed) {
      // The comm wants a JSON value; our payloads are plain JSON objects.
      this.comm.send(payload as never);
    }
  }

  private flushMove(): void {
    const e = this.pendingMove;
    this.pendingMove = null;
    if (!e) {
      return;
    }
    const p = this.point(e);
    const action = this.buttonsDown > 0 ? 'drag' : 'move';
    this.send({ type: 'mouse', action, x: p.x, y: p.y, button: this.button(e), ...this.mods(e) });
  }

  private point(e: MouseEvent): { x: number; y: number } {
    const rect = this.canvas.getBoundingClientRect();
    // The canvas may be displayed at a different size than its backing store (e.g. CSS max-width scaling),
    // so map client coords back into the component's own pixel space.
    const sx = rect.width > 0 ? this.canvas.width / rect.width : 1;
    const sy = rect.height > 0 ? this.canvas.height / rect.height : 1;
    return { x: Math.round((e.clientX - rect.left) * sx), y: Math.round((e.clientY - rect.top) * sy) };
  }

  private mods(e: MouseEvent | KeyboardEvent): Record<string, boolean> {
    return { shift: e.shiftKey, ctrl: e.ctrlKey, alt: e.altKey, meta: e.metaKey };
  }

  /** Map a browser mouse button (0 left, 1 middle, 2 right) to AWT's (1, 2, 3). */
  private button(e: MouseEvent): number {
    return e.button === 1 ? 2 : e.button === 2 ? 3 : 1;
  }

  private wireEvents(): void {
    const c = this.canvas;

    c.addEventListener('mousedown', e => {
      c.focus();
      this.buttonsDown++;
      const p = this.point(e);
      this.send({ type: 'mouse', action: 'down', x: p.x, y: p.y, button: this.button(e), clickCount: e.detail || 1, ...this.mods(e) });
      e.preventDefault();
    });

    c.addEventListener('mouseup', e => {
      this.buttonsDown = Math.max(0, this.buttonsDown - 1);
      const p = this.point(e);
      this.send({ type: 'mouse', action: 'up', x: p.x, y: p.y, button: this.button(e), clickCount: e.detail || 1, ...this.mods(e) });
      e.preventDefault();
    });

    // Browsers fire mousemove 60-120x/s; coalesce to at most one message per animation frame (the latest position).
    c.addEventListener('mousemove', e => {
      const hadPending = this.pendingMove !== null;
      this.pendingMove = e;
      if (!hadPending) {
        requestAnimationFrame(() => this.flushMove());
      }
    });

    c.addEventListener(
      'wheel',
      e => {
        const p = this.point(e);
        this.send({ type: 'mouse', action: 'wheel', x: p.x, y: p.y, deltaY: e.deltaY, ...this.mods(e) });
        e.preventDefault();
      },
      { passive: false }
    );

    c.addEventListener('mouseleave', () => {
      this.buttonsDown = 0;
      this.flushMove();
    });

    c.addEventListener('keydown', e => {
      const printable = e.key.length === 1;
      this.send({ type: 'key', action: 'down', keyCode: e.keyCode, char: printable ? e.key : '', ...this.mods(e) });
      if (printable) {
        this.send({ type: 'key', action: 'press', char: e.key, ...this.mods(e) });
      }
      e.preventDefault();
    });

    c.addEventListener('keyup', e => {
      this.send({ type: 'key', action: 'up', keyCode: e.keyCode, char: e.key.length === 1 ? e.key : '', ...this.mods(e) });
      e.preventDefault();
    });

    c.addEventListener('contextmenu', e => e.preventDefault());
  }
}
