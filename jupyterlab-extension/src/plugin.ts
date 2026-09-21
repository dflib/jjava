import { JupyterFrontEnd, JupyterFrontEndPlugin } from '@jupyterlab/application';
import { INotebookTracker, NotebookPanel } from '@jupyterlab/notebook';
import { Kernel, KernelMessage } from '@jupyterlab/services';
import { bindComm } from './registry';

const TARGET_NAME = 'jjava.swing.v1';

function registerOnKernel(kernel: Kernel.IKernelConnection | null | undefined): void {
  if (!kernel) {
    return;
  }
  kernel.registerCommTarget(TARGET_NAME, (comm, openMsg: KernelMessage.ICommOpenMsg) => {
    const data = openMsg.content.data as { token?: string };
    const token = data && data.token ? String(data.token) : null;
    if (token) {
      bindComm(token, comm);
    }
  });
}

function wireNotebook(panel: NotebookPanel): void {
  const sessionContext = panel.sessionContext;
  void sessionContext.ready.then(() => registerOnKernel(sessionContext.session?.kernel));
  sessionContext.kernelChanged.connect((_, args) => registerOnKernel(args.newValue));
}

/**
 * Registers the {@code jjava.swing.v1} comm target on every notebook kernel. The kernel opens this comm (carrying a
 * token) when {@code displayInteractive(...)} runs; we hand the comm to the canvas that the mime renderer created for
 * the same token.
 */
const plugin: JupyterFrontEndPlugin<void> = {
  id: 'jjava-swing:plugin',
  autoStart: true,
  requires: [INotebookTracker],
  activate: (_app: JupyterFrontEnd, tracker: INotebookTracker) => {
    tracker.forEach(wireNotebook);
    tracker.widgetAdded.connect((_, panel) => wireNotebook(panel));
  }
};

export default plugin;
