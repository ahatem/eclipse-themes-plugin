package probe.gap;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

/**
 * workbench-css-gap-fixes runtime probe. For each coordinated family: open a shell with a separator label, a bare
 * ProgressBar and a ProgressMonitorPart, measure getter colors and rendered pixels with the shipped stylesheet
 * ("base"), then parse the candidate rules into the live CSS engine, re-apply and measure again ("candidate").
 */
public class Probe implements IStartup {

	static final String PREFIX = "com.github.eclipsethemes.workbench.";

	static Path out() { return Paths.get(System.getProperty("probe.out")); }

	static String tag() { return System.getProperty("probe.tag", "probe"); }

	static void log(String s) {
		try {
			Files.createDirectories(out());
			Files.writeString(out().resolve(tag() + ".log"), s + System.lineSeparator(), StandardOpenOption.CREATE,
					StandardOpenOption.APPEND);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static String candidate(String fam) {
		String r = "'#com-github-eclipsethemes-workbench-" + fam + "-";
		return "ProgressBar, ProgressIndicator > * { background-color: " + r + "background'; color: " + r
				+ "accent'; }\n" + "Label[style~='SWT.SEPARATOR'] { background-color: inherit; color: " + r
				+ "border'; }\n";
	}

	Label separator;
	ProgressBar bare;
	ProgressMonitorPart monitor;
	Composite page;

	@Override
	public void earlyStartup() {
		Display d = Display.getDefault();
		d.asyncExec(() -> d.timerExec(4000, () -> {
			try {
				run(d);
			} catch (Throwable t) {
				log("ERROR " + t);
				for (StackTraceElement e : t.getStackTrace()) log("  at " + e);
			} finally {
				log("DONE");
				PlatformUI.getWorkbench().close();
			}
		}));
	}

	void run(Display d) throws Exception {
		IThemeEngine eng = PlatformUI.getWorkbench().getService(IThemeEngine.class);
		log("THEME_BUNDLE " + Platform.getBundle("org.eclipse.e4.ui.css.swt.theme").getVersion());
		log("SWT " + SWT.getPlatform() + " " + SWT.getVersion() + " os.version=" + System.getProperty("os.version"));
		for (ITheme t : eng.getThemes()) {
			if (t.getId().startsWith(PREFIX)) log("THEME " + t.getId() + " label=" + t.getLabel());
		}
		for (String fam : List.of("dark", "light")) {
			eng.setTheme(PREFIX + fam, false);
			pump(d, 1500);
			log("ACTIVE " + eng.getActiveTheme().getId());
			for (String role : List.of("background", "surface", "accent", "border", "foreground")) {
				log("ROLE " + fam + "." + role + " " + JFaceResources.getColorRegistry().getRGB(PREFIX + fam + "." + role));
			}
			Shell s = build(d);
			eng.applyStyles(s, true);
			pump(d, 1000);
			measure(fam, "base", s);
			CSSEngine css = WidgetElement.getEngine(d);
			css.parseStyleSheet(new StringReader(candidate(fam)));
			css.applyStyles(s, true);
			s.redraw();
			s.update();
			pump(d, 1000);
			measure(fam, "candidate", s);
			s.dispose();
		}
	}

	Shell build(Display d) {
		Shell s = new Shell(d, SWT.SHELL_TRIM);
		s.setText("gap probe");
		s.setLayout(new GridLayout(1, false));
		page = new Composite(s, SWT.NONE);
		page.setLayout(new GridLayout(1, false));
		page.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		new Label(page, SWT.NONE).setText("Separator below");
		separator = new Label(page, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		bare = new ProgressBar(page, SWT.HORIZONTAL);
		bare.setSelection(30);
		bare.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		monitor = new ProgressMonitorPart(page, null, true);
		monitor.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		new Label(page, SWT.NONE).setText(" ");
		s.setBounds(60, 60, 520, 320);
		s.open();
		monitor.beginTask("Probing", 100);
		monitor.worked(30);
		return s;
	}

	void measure(String fam, String phase, Shell s) throws Exception {
		Rectangle sb = s.getBounds();
		Image im = new Image(s.getDisplay(), sb.width, sb.height);
		GC gc = new GC(im);
		boolean ok = org.eclipse.swt.internal.win32.OS.PrintWindow(s.handle, gc.handle, 2);
		gc.dispose();
		ImageData data = im.getImageData();
		ImageLoader l = new ImageLoader();
		l.data = new ImageData[] { data };
		l.save(out().resolve(tag() + "-" + fam + "-" + phase + ".png").toString(), SWT.IMAGE_PNG);
		im.dispose();
		log("CAPTURE " + fam + " " + phase + " printWindow=" + ok);
		sample(fam, phase, "page", page, data, sb, 0.98, 0.95);
		sample(fam, phase, "separator", separator, data, sb, 0.5, 0.0);
		sample(fam, phase, "separator+1", separator, data, sb, 0.5, 0.99);
		sample(fam, phase, "bare.track", bare, data, sb, 0.9, 0.5);
		sample(fam, phase, "bare.fill", bare, data, sb, 0.1, 0.5);
		List<ProgressBar> bars = new ArrayList<>();
		collect(monitor, bars);
		for (int i = 0; i < bars.size(); i++) {
			ProgressBar b = bars.get(i);
			if (!b.isVisible()) continue;
			sample(fam, phase, "monitor.bar" + i + ".track", b, data, sb, 0.9, 0.5);
			sample(fam, phase, "monitor.bar" + i + ".fill", b, data, sb, 0.05, 0.5);
		}
	}

	static void collect(Control c, List<ProgressBar> bars) {
		if (c instanceof ProgressBar pb) bars.add(pb);
		if (c instanceof Composite comp) for (Control k : comp.getChildren()) collect(k, bars);
	}

	static void sample(String fam, String phase, String name, Control w, ImageData data, Rectangle sb, double fx,
			double fy) {
		Point p = w.toDisplay(0, 0);
		Point size = w.getSize();
		int x = p.x - sb.x + (int) Math.min(size.x - 1, Math.max(0, Math.round(fx * (size.x - 1))));
		int y = p.y - sb.y + (int) Math.min(size.y - 1, Math.max(0, Math.round(fy * (size.y - 1))));
		RGB px = (x >= 0 && y >= 0 && x < data.width && y < data.height) ? data.palette.getRGB(data.getPixel(x, y))
				: null;
		log("SAMPLE " + fam + " " + phase + " " + name + " class=" + w.getClass().getSimpleName() + " size=" + size
				+ " bg=" + rgb(w.getBackground()) + " fg=" + rgb(w.getForeground()) + " pixel@" + x + "," + y + "="
				+ px);
	}

	static String rgb(Color c) { return c == null ? "null" : c.getRGB().toString(); }

	static void pump(Display d, long ms) {
		long end = System.currentTimeMillis() + ms;
		while (System.currentTimeMillis() < end) {
			if (!d.readAndDispatch()) {
				try { Thread.sleep(20); } catch (InterruptedException e) { return; }
			}
		}
	}
}
