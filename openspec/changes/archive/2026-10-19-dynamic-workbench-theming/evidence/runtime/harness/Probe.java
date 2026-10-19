package spike.p8;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.*;
import org.osgi.framework.Bundle;

/**
 * Disposable R8a runtime-verification driver (not part of the product). Black-box: it drives the production
 * preference page through its widgets and observes the workbench through public SWT/JFace APIs.
 */
public class Probe implements IStartup {
  static final String NODE = "org.eclipse.e4.ui.css.swt.theme";
  static final String PAGE = "com.github.eclipsethemes.eclipse.preferences.EclipseThemesPreferencePage";
  static PreferenceDialog dialog;

  static Path out() { return Paths.get(System.getProperty("probe.out")); }
  static void log(String s) {
    try { Files.writeString(out().resolve(System.getProperty("probe.tag") + ".log"), s + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND); } catch (Exception e) { e.printStackTrace(); }
    System.out.println("R8A " + s);
  }
  static IThemeEngine engine() { return PlatformUI.getWorkbench().getService(IThemeEngine.class); }
  static Shell shell() { return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(); }
  static String active() { ITheme t = engine().getActiveTheme(); return t == null ? "<null>" : t.getId(); }
  static String pref(IScopeContext c) { return c.getNode(NODE).get("themeid", "<absent>"); }

  static void state(String label) {
    log("STATE " + label + " | active=" + active() + " | instance=" + pref(InstanceScope.INSTANCE) + " | config=" + pref(ConfigurationScope.INSTANCE)
        + " | default=" + pref(DefaultScope.INSTANCE) + " | osDark=" + Display.isSystemDarkTheme() + " | highContrast=" + Display.getDefault().getHighContrast());
    var own = InstanceScope.INSTANCE.getNode("com.github.eclipsethemes");
    try { StringBuilder sb = new StringBuilder(); for (String k : own.keys()) sb.append(k).append('=').append(own.get(k, "?")).append(' '); log("OWNPREFS " + sb); } catch (Exception e) { log("OWNPREFS-FAIL " + e); }
  }

  public void earlyStartup() {
    Thread watchdog = new Thread(() -> { try { Thread.sleep(270000); } catch (Exception e) {} log("TIMEOUT"); Runtime.getRuntime().halt(4); });
    watchdog.setDaemon(true); watchdog.start();
    Display.getDefault().asyncExec(() -> { try {
      log("ENV os=" + System.getProperty("os.name") + " " + System.getProperty("os.version") + " ws=" + SWT.getPlatform() + " java=" + System.getProperty("java.runtime.version")
          + " cssThemeBundle=" + Platform.getBundle(NODE).getVersion() + " platform=" + Platform.getBundle("org.eclipse.platform").getVersion()
          + " product=" + Platform.getBundle("com.github.eclipsethemes").getVersion() + " state=" + Platform.getBundle("com.github.eclipsethemes").getState());
      log("THEMES " + engine().getThemes().stream().map(t -> t.getId()).toList());
      state("boot");
      var intro = PlatformUI.getWorkbench().getIntroManager(); if (intro.getIntro() != null) intro.closeIntro(intro.getIntro());
      shell().setSize(1100, 850);
      var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      var perspective = PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId("org.eclipse.ui.resourcePerspective");
      if (perspective != null) page.setPerspective(perspective);
      page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      page.showView("spike.p8.view");
      page.openEditor(new Input(), "spike.p8.editor");
      page.zoomOut(); page.setEditorAreaVisible(true);
      run(0);
    } catch (Throwable t) { log("BOOT-FAIL " + t); PlatformUI.getWorkbench().close(); } });
  }

  static void dismissDialogs() {
    for (Shell s : Display.getDefault().getShells()) {
      if (s.isDisposed() || s == shell() || (dialog != null && s == dialog.getShell())) continue;
      String t = s.getText();
      if (t.contains("Defender")) { log("DEFENDER dialog cancelled"); s.close(); }
      else if (t.contains("Theme") || t.contains("Restart")) { log("RESTART-DIALOG title=" + t + " buttons=" + buttons(s)); click(s, "Don't Restart"); if (!s.isDisposed()) click(s, "No"); if (!s.isDisposed()) s.close(); }
      else if (!t.isEmpty() && !t.contains("limbo") && !t.contains("Updater")) log("OTHER-SHELL " + t + " buttons=" + buttons(s));
    }
  }
  static List<String> buttons(Control c) { List<String> l = new ArrayList<>(); if (c instanceof Button b) l.add(b.getText()); if (c instanceof Composite p) for (Control x : p.getChildren()) l.addAll(buttons(x)); return l; }
  static boolean click(Control c, String text) {
    if (c instanceof Button b && b.getText().replace("&", "").toLowerCase().contains(text.toLowerCase())) { b.notifyListeners(SWT.Selection, new Event()); return true; }
    if (c instanceof Composite p) for (Control x : p.getChildren()) if (click(x, text)) return true;
    return false;
  }
  static boolean clickExact(Control c, String text) {
    if (c instanceof Button b && b.getText().replace("&", "").equals(text)) { b.notifyListeners(SWT.Selection, new Event()); return true; }
    if (c instanceof Composite p) for (Control x : p.getChildren()) if (clickExact(x, text)) return true;
    return false;
  }

  static String[] cached;
  static String[] steps() {
    if (cached == null) { try { cached = Files.readString(Paths.get(System.getProperty("probe.stepsfile"))).replaceAll("\\R", "").split(";"); } catch (Exception e) { throw new RuntimeException(e); } }
    return cached;
  }
  static void run(int i) {
    dismissDialogs();
    String[] steps = steps();
    if (i >= steps.length) { log("DONE"); PlatformUI.getWorkbench().close(); return; }
    String[] f = steps[i].split("\\|", -1); int delay = 900;
    log("STEP " + steps[i]);
    try { switch (f[0]) {
      case "state": state(f[1]); break;
      case "surfaces": surfaces(f[1]); break;
      case "shot": shot(f[1], (dialog == null || (f.length > 2 && f[2].equals("main"))) ? shell() : dialog.getShell()); break;
      case "wait": delay = Integer.parseInt(f[1]); break;
      case "regs": { var reg = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry(); StringBuilder sb = new StringBuilder();
        for (String fam : new String[] { "dark", "light" }) for (String r : new String[] { "background", "foreground", "surface", "raised", "border", "muted", "selection" }) {
          String id = "com.github.eclipsethemes.workbench." + fam + "." + r; var rgb = reg.getRGB(id); sb.append(fam.charAt(0)).append('.').append(r).append('=').append(rgb == null ? "null" : String.format("#%02x%02x%02x", rgb.red, rgb.green, rgb.blue)).append(' '); }
        log("REGS " + f[1] + " " + sb); break; }
      case "colorspage": { // is any plugin color visible on Colors and Fonts?
        var defs = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme(); log("COLORS-AND-FONTS current=" + defs.getId()); break; }
      case "import": { Bundle b = Platform.getBundle("com.github.eclipsethemes"); Class<?> c = b.loadClass("com.github.eclipsethemes.EclipseThemes");
        Object inst = c.getMethod("instance").invoke(null); Object mgr = c.getMethod("getManager").invoke(inst);
        Object outcome = mgr.getClass().getMethod("importTheme", File.class).invoke(mgr, new File(f[1])); log("IMPORT " + outcome); break; }
      case "watch": { var reg = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
        reg.addListener(ev -> { if (ev.getProperty().startsWith("com.github.eclipsethemes.workbench.dark.background")) { var rgb = reg.getRGB(ev.getProperty()); StringBuilder sb = new StringBuilder("WATCH change " + ev.getProperty() + " -> " + rgb + " thread=" + Thread.currentThread().getName());
            for (StackTraceElement e : new Throwable().getStackTrace()) sb.append(" | at ").append(e); log(sb.toString()); } });
        log("WATCH installed registry=" + System.identityHashCode(reg) + " theme=" + PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getId()); break; }
      case "prefs": prefsOpen(); break;
      case "pick": pick(dialog.getShell(), f[1]); break;
      case "check": check(dialog.getShell(), f[1].equals("1")); break;
      case "labels": labels(dialog.getShell()); break;
      case "apply": { long t0 = System.nanoTime(); boolean c = clickExact(dialog.getShell(), "Apply"); log("APPLY-CLICK found=" + c + " ms=" + (System.nanoTime() - t0) / 1000000); break; }
      case "okclose": { long t0 = System.nanoTime(); boolean c = click(dialog.getShell(), "Apply and Close"); log("OKCLOSE-CLICK found=" + c + " ms=" + (System.nanoTime() - t0) / 1000000); dialog = null; break; }
      case "pcancel": { boolean c = clickExact(dialog.getShell(), "Cancel"); log("CANCEL-CLICK found=" + c); dialog = null; break; }
      case "appearance": appearance(); break;
      case "choose": choose(dialog.getShell(), f[1]); break;
      case "aok": { Display.getDefault().timerExec(1200, Probe::dismissDialogs); boolean c = click(dialog.getShell(), "Apply and Close"); log("A-OK-CLICK found=" + c); state("after-appearance-ok"); dialog = null; break; }
      case "open-appearance-button": { boolean c = clickExact(dialog.getShell(), "Open Appearance..."); log("OPEN-APPEARANCE-BUTTON found=" + c + " page=" + dialog.getSelectedPage().getClass().getName()); break; }
    } } catch (Throwable t) { log("THREW " + steps[i] + " " + t.getClass().getName() + ": " + t.getMessage()); state("after-throw"); }
    Display.getDefault().timerExec(delay, () -> run(i + 1));
  }

  static void prefsOpen() {
    dialog = PreferencesUtil.createPreferenceDialogOn(shell(), PAGE, null, null);
    log("PREFS pageIdValid=" + (dialog != null));
    if (dialog == null) return;
    dialog.setBlockOnOpen(false); dialog.open();
    log("PREFS selectedPage=" + dialog.getSelectedPage().getClass().getName());
  }
  static boolean pick(Control c, String name) {
    if (c instanceof Table t && t.getItemCount() > 0 && !(t.getParent() instanceof Tree)) {
      for (int j = 0; j < t.getItemCount(); j++) {
        String txt = t.getItem(j).getText();
        if (txt.replace("● ", "").replace(" *", "").equals(name)) { t.setSelection(j); Event e = new Event(); e.item = t.getItem(j); t.notifyListeners(SWT.Selection, e); log("PICK " + name + " ok"); return true; }
      }
    }
    if (c instanceof Composite p) for (Control x : p.getChildren()) if (pick(x, name)) return true;
    return false;
  }
  static void check(Control c, boolean v) {
    if (c instanceof Button b && (b.getStyle() & SWT.CHECK) != 0 && b.getText().startsWith("Also theme the Eclipse workbench")) {
      log("CHECKBOX label='" + b.getText() + "' enabled=" + b.getEnabled() + " before=" + b.getSelection()); if (b.getEnabled()) b.setSelection(v); log("CHECKBOX after=" + b.getSelection());
    }
    if (c instanceof Composite p) for (Control x : p.getChildren()) check(x, v);
  }
  static void labels(Control c) {
    if (c instanceof Label l && !l.getText().isEmpty() && (l.getParent() instanceof Group g && g.getText().equals("Workbench") || l.getText().startsWith("✓") || l.getText().contains("Editor colors") || l.getText().contains("Applied") || l.getText().contains("Workbench ")))
      log("LABEL " + l.getText().replaceAll("\\R", " / "));
    if (c instanceof Button b && (b.getStyle() & SWT.CHECK) != 0) log("CHECKBOX-STATE '" + b.getText() + "' enabled=" + b.getEnabled() + " selected=" + b.getSelection());
    if (c instanceof Composite p) for (Control x : p.getChildren()) labels(x);
  }

  static void appearance() {
    dialog = PreferencesUtil.createPreferenceDialogOn(shell(), "org.eclipse.ui.preferencePages.Views", null, null);
    log("APPEARANCE pageIdValid=" + (dialog != null));
    if (dialog == null) return;
    dialog.setBlockOnOpen(false); dialog.open();
    combos(dialog.getShell());
  }
  static void combos(Control c) { if (c instanceof Combo v) log("COMBO items=" + Arrays.toString(v.getItems()) + " selected=" + v.getText()); if (c instanceof Composite p) for (Control x : p.getChildren()) combos(x); }
  static void choose(Control c, String name) {
    if (c instanceof Combo v) for (int j = 0; j < v.getItemCount(); j++) if (v.getItem(j).equals(name)) { v.select(j); v.notifyListeners(SWT.Selection, new Event()); log("COMBO choose=" + name + " actual=" + v.getText()); }
    if (c instanceof Composite p) for (Control x : p.getChildren()) choose(x, name);
  }

  static String rgb(Color c) { return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()); }
  static void surfaces(String label) {
    Map<String, String> seen = new LinkedHashMap<>();
    walk(shell(), seen);
    for (var en : seen.entrySet()) log("SURFACE " + label + " " + en.getKey() + " " + en.getValue());
  }
  static void walk(Control c, Map<String, String> seen) {
    if (c.isDisposed() || !c.isVisible()) return;
    String n = c.getClass().getSimpleName(); Object id = c.getData("org.eclipse.e4.ui.css.id");
    String key = n + (id == null ? "" : "#" + id);
    boolean interesting = c instanceof Shell || c instanceof CTabFolder || c instanceof ToolBar || c instanceof Tree || c instanceof Table || c instanceof StyledText || (id != null && String.valueOf(id).contains("StatusLine"));
    if (interesting && !seen.containsKey(key)) seen.put(key, "bg=" + rgb(c.getBackground()) + " fg=" + rgb(c.getForeground()) + (c instanceof CTabFolder t ? " tabSelBg=" + rgb(t.getSelectionBackground()) : ""));
    if (c instanceof Composite p) for (Control x : p.getChildren()) walk(x, seen);
  }
  static void shot(String name, Shell s) { s.setVisible(true); s.setMinimized(false); s.forceActive(); Display.getDefault().timerExec(500, () -> capture(name, s)); }
  static void capture(String name, Shell s) {
    Image im = new Image(s.getDisplay(), s.getSize().x, s.getSize().y); GC gc = new GC(im);
    try { log("PRINT " + s.print(gc)); ImageLoader l = new ImageLoader(); l.data = new ImageData[] { im.getImageData() };
      l.save(out().resolve(System.getProperty("probe.tag") + "-" + name + ".png").toString(), SWT.IMAGE_PNG); log("SHOT " + name); }
    finally { gc.dispose(); im.dispose(); }
  }

  public static class View extends ViewPart {
    public void createPartControl(Composite parent) {
      parent.setLayout(new GridLayout(2, true));
      Tree tree = new Tree(parent, SWT.BORDER); tree.setLayoutData(new GridData(250, 90)); new TreeItem(tree, 0).setText("Probe tree");
      Table table = new Table(parent, SWT.BORDER); table.setLayoutData(new GridData(250, 90)); new TableItem(table, 0).setText("Probe table");
      ToolBar bar = new ToolBar(parent, SWT.FLAT); new ToolItem(bar, SWT.PUSH).setText("Probe toolbar");
      new Button(parent, SWT.PUSH).setText("Native button");
    }
    public void setFocus() {}
  }
  public static class Editor extends EditorPart {
    public void init(IEditorSite s, IEditorInput i) { setSite(s); setInput(i); setPartName("R8a editor"); }
    public void createPartControl(Composite p) { p.setLayout(new FillLayout()); new StyledText(p, SWT.MULTI).setText("R8a editor text"); }
    public void doSave(IProgressMonitor m) {} public void doSaveAs() {} public boolean isDirty() { return false; } public boolean isSaveAsAllowed() { return false; } public void setFocus() {}
  }
  static class Input implements IEditorInput {
    public boolean exists() { return false; } public ImageDescriptor getImageDescriptor() { return null; } public String getName() { return "R8a"; }
    public IPersistableElement getPersistable() { return null; } public String getToolTipText() { return "Disposable research"; } public <T> T getAdapter(Class<T> c) { return null; }
  }
}
