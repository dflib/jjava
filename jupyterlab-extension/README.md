# jjava-swing — interactive Swing for JupyterLab

A JupyterLab 4 extension that makes `displayInteractive(component)` in the
[JJava](https://github.com/dflib/jjava) Java kernel render a **live, interactive** Swing/AWT
component in a notebook cell: the kernel streams PNG frames of the component and the extension
forwards mouse/keyboard events back to it.

## How it works

1. The kernel emits an output of MIME type `application/vnd.jjava.swing.v1+json` carrying a `token`
   and the component's size. The extension's **mime renderer** mounts a `<canvas>` for that token.
2. The kernel opens a `jjava.swing.v1` **comm** (carrying the same token). The extension's **notebook
   plugin** has registered a handler for that comm target; it matches the comm to the canvas by token.
3. Frames arrive as comm messages with the PNG in the message buffers and are drawn to the canvas;
   DOM mouse/keyboard events are serialized to JSON and sent back over the comm.

Without this extension installed, the same cell falls back to a short text message (and, separately,
a plain `display(component)` still shows a static PNG snapshot — that needs no extension).

## Develop / install locally

Requires Node.js and a Python env with JupyterLab 4.

```bash
cd jupyterlab-extension
npm install
npm run build            # tsc + dev labextension build (needs `pip install jupyterlab`)
jupyter labextension develop --overwrite .
jupyter lab
```

Type-check only (no JupyterLab needed):

```bash
npm install
npm run typecheck
```

## Build a distributable wheel

```bash
pip install build
python -m build         # produces dist/jjava_swing-*.whl (runs the npm production build)
pip install dist/jjava_swing-*.whl
```

## Usage (in a Java notebook)

```java
import javax.swing.*;
import java.awt.*;

JButton b = new JButton("Click me");
b.addActionListener(e -> b.setText("Clicked at " + System.currentTimeMillis()));
displayInteractive(b);
```

### Without the extension

`displayInteractive(...)` is safe to use even if this extension isn't installed: the same output also
carries a static `image/png` snapshot, so a plain JupyterLab/Notebook/VS Code/console frontend just shows
a still image instead of the live canvas. (And `display(component)` always renders a static snapshot with
no extension or comm at all.) Notebooks that never touch Swing are completely unaffected — AWT is not
initialized until a component is actually rendered.

### JavaFX

JavaFX rides the same pipeline via `javafx.embed.swing.JFXPanel` (a Swing component that embeds an FX
scene). Pass a `JFXPanel` or a bare `javafx.scene.Node` (auto-wrapped) to `displayInteractive(...)`.
JavaFX is an optional, user-supplied dependency — pull it with `%maven` (platform classifier) and build
the scene on the FX thread:

```java
%maven org.openjfx:javafx-controls:jar:linux:21.0.2
%maven org.openjfx:javafx-swing:jar:linux:21.0.2

import javafx.embed.swing.JFXPanel;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

JFXPanel fx = new JFXPanel();                 // boots the FX toolkit
Platform.runLater(() -> {
    Button b = new Button("FX button");
    b.setOnAction(e -> b.setText("clicked"));
    fx.setScene(new Scene(new StackPane(b), 320, 200));
});
displayInteractive(fx);
```

See [examples/javafx-demo.ipynb](examples/javafx-demo.ipynb). JavaFX needs a display; on a headless
server it additionally needs Monocle (`-Dglass.platform=Monocle -Dmonocle.platform=Headless`).

## Known limitations

The component is hosted off-screen with no native peer, so input is dispatched synthetically. This
covers custom-painted panels, charts, buttons, text fields and tables, but **not** features that need
their own native windows or the real lightweight dispatcher: tooltips, popup menus, combo-box
dropdowns, drag-and-drop, and some focus-traversal edge cases. Mutate Swing state on the EDT
(`SwingUtilities.invokeLater`); repaints are captured and streamed regardless of which thread caused them.
