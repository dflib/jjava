import { IRenderMime } from '@jupyterlab/rendermime-interfaces';
import { Widget } from '@lumino/widgets';
import { CanvasController } from './canvas';
import { registerCanvas, removeCanvas } from './registry';

export const MIME_TYPE = 'application/vnd.jjava.swing.v1+json';

interface ISwingModel {
  token: string;
  width?: number;
  height?: number;
}

/** The output widget: it mounts a canvas and registers it by token so the comm plugin can attach the kernel comm. */
class SwingOutput extends Widget implements IRenderMime.IRenderer {
  private token: string | null = null;
  private controller: CanvasController | null = null;

  constructor() {
    super();
    this.addClass('jjava-swing-output');
  }

  async renderModel(model: IRenderMime.IMimeModel): Promise<void> {
    if (this.controller) {
      return; // already rendered once
    }
    const data = model.data[MIME_TYPE] as unknown as ISwingModel | undefined;
    if (!data || !data.token) {
      return;
    }
    this.token = String(data.token);
    const width = Number(data.width) || 300;
    const height = Number(data.height) || 150;

    this.controller = new CanvasController(width, height);
    this.node.appendChild(this.controller.element);
    registerCanvas(this.token, this.controller);
  }

  dispose(): void {
    if (this.token) {
      removeCanvas(this.token);
    }
    if (this.controller) {
      this.controller.dispose();
    }
    super.dispose();
  }
}

const rendererFactory: IRenderMime.IRendererFactory = {
  safe: false,
  mimeTypes: [MIME_TYPE],
  createRenderer: () => new SwingOutput()
};

const extension: IRenderMime.IExtension = {
  id: 'jjava-swing:mime',
  rendererFactory,
  rank: 0,
  dataType: 'json'
};

export default extension;
