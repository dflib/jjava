import { Kernel } from '@jupyterlab/services';
import { CanvasController } from './canvas';

/**
 * Rendezvous between the mime renderer (which creates the canvas for a given token) and the comm plugin (which receives
 * the kernel-opened comm for that token). The two can arrive in either order, so we hold whichever shows up first until
 * its partner appears.
 */

const canvases = new Map<string, CanvasController>();
const pendingComms = new Map<string, Kernel.IComm>();

export function registerCanvas(token: string, controller: CanvasController): void {
  canvases.set(token, controller);
  const comm = pendingComms.get(token);
  if (comm) {
    pendingComms.delete(token);
    controller.attachComm(comm);
  }
}

export function removeCanvas(token: string): void {
  canvases.delete(token);
  pendingComms.delete(token);
}

export function bindComm(token: string, comm: Kernel.IComm): void {
  const controller = canvases.get(token);
  if (controller) {
    controller.attachComm(comm);
  } else {
    pendingComms.set(token, comm);
  }
}
