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
      case "pexp": pexp(Integer.parseInt(f[1]), shell()); break;
      case "pwatch": watch(); break;
      case "eprefs": eprefs(f[1]); break;
      case "pix": pix(f[1], shell()); break;
      case "shot": shot(f[1], (dialog == null || (f.length > 2 && f[2].equals("main"))) ? shell() : dialog.getShell()); break;
      case "wait": delay = Integer.parseInt(f[1]); break;
      case "regs": { var reg = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry(); StringBuilder sb = new StringBuilder();
        for (String fam : new String[] { "dark", "light" }) for (String r : new String[] { "background", "foreground", "surface", "raised", "border", "muted", "selection", "accent" }) {
          String id = "com.github.eclipsethemes.workbench." + fam + "." + r; var rgb = reg.getRGB(id); sb.append(fam.charAt(0)).append('.').append(r).append('=').append(rgb == null ? "null" : String.format("#%02x%02x%02x", rgb.red, rgb.green, rgb.blue)).append(' '); }
        log("REGS " + f[1] + " " + sb); break; }
      case "jfc": { var reg = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry(); StringBuilder sb = new StringBuilder("JFC " + f[1]);
        for (String k : new String[] { "HYPERLINK_COLOR", "ACTIVE_HYPERLINK_COLOR", "ERROR_COLOR", "DECORATIONS_COLOR", "QUALIFIER_COLOR", "COUNTER_COLOR", "CONTENT_ASSIST_BACKGROUND_COLOR", "CONTENT_ASSIST_FOREGROUND_COLOR", "org.eclipse.ui.workbench.HOVER_BACKGROUND", "org.eclipse.ui.workbench.HOVER_FOREGROUND", "org.eclipse.ui.workbench.INFORMATION_BACKGROUND", "org.eclipse.egit.ui.IgnoredResourceForegroundColor", "org.eclipse.egit.ui.UncommittedChangeForegroundColor" }) {
          var rgb = reg.getRGB(k); if (rgb == null) rgb = org.eclipse.jface.resource.JFaceResources.getColorRegistry().getRGB(k); sb.append(' ').append(k.replace("org.eclipse.ui.workbench.", "").replace("org.eclipse.egit.ui.", "egit.")).append('=').append(rgb == null ? "null" : String.format("#%02x%02x%02x", rgb.red, rgb.green, rgb.blue)); }
        log(sb.toString()); break; }
      case "colorspage": { // is any plugin color visible on Colors and Fonts?
        var defs = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme(); log("COLORS-AND-FONTS current=" + defs.getId()); break; }
      case "import": { Bundle b = Platform.getBundle("com.github.eclipsethemes"); Class<?> c = b.loadClass("com.github.eclipsethemes.EclipseThemes");
        Object inst = c.getMethod("instance").invoke(null); Object mgr = c.getMethod("getManager").invoke(inst);
        Object outcome = mgr.getClass().getMethod("importTheme", File.class).invoke(mgr, new File(f[1])); log("IMPORT " + outcome); break; }
      case "watch": { var reg = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
        reg.addListener(ev -> { if (ev.getProperty().startsWith("com.github.eclipsethemes.workbench.dark.background")) { var rgb = reg.getRGB(ev.getProperty()); StringBuilder sb = new StringBuilder("WATCH change " + ev.getProperty() + " -> " + rgb + " thread=" + Thread.currentThread().getName());
            for (StackTraceElement e : new Throwable().getStackTrace()) sb.append(" | at ").append(e); log(sb.toString()); } });
        log("WATCH installed registry=" + System.identityHashCode(reg) + " theme=" + PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getId()); break; }
      case "cdump": { StringBuilder sb = new StringBuilder("CDUMP " + f[1] + " themeId=" + PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getId());
        for (org.eclipse.jface.resource.ColorRegistry r : new org.eclipse.jface.resource.ColorRegistry[] { PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry(), org.eclipse.jface.resource.JFaceResources.getColorRegistry() })
          for (String k : new java.util.TreeSet<>(r.getKeySet())) { var rgb = r.getRGB(k); if (rgb != null && String.format("%02x%02x%02x", rgb.red, rgb.green, rgb.blue).equals("787878")) sb.append(" ").append(k); }
        log(sb.toString()); break; }
      case "ctl": ctl(); break;
      case "ctlclose": { if (ctlShell != null) { ctlShell.close(); ctlShell = null; } break; }
      case "ctlshot": screen(f[1], ctlShell); break;
      case "ctlaudit": audit(f[1], ctlShell); break;
      case "prefs": prefsOpen(); break;
      case "prefspage": { dialog = PreferencesUtil.createPreferenceDialogOn(shell(), f[1], null, null); dialog.setBlockOnOpen(false); dialog.open(); log("PREFSPAGE " + f[1]); break; }
      case "closeprefs": { if (dialog != null) { dialog.close(); dialog = null; } break; }
      case "stock": { long t0 = System.nanoTime(); var th = engine().getThemes().stream().filter(t -> t.getId().equals(f[1])).findFirst().orElse(null); log("STOCK " + f[1] + " found=" + (th != null)); if (th != null) engine().setTheme(th, true); log("STOCK-SET ms=" + (System.nanoTime() - t0) / 1000000 + " active=" + active()); break; }
      case "view": { var pg = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(); try { pg.showView(f[1]); log("VIEW " + f[1]); } catch (Exception e) { log("VIEW-FAIL " + f[1] + " " + e); } break; }
      case "persp": { var pr = PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(f[1]); PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().setPerspective(pr); log("PERSP " + f[1] + " " + (pr != null)); break; }
      case "project": project(); break;
      case "screen": screen(f[1], (dialog != null && f.length > 2 && f[2].equals("dialog")) ? dialog.getShell() : shell()); break;
      case "audit": audit(f[1], (dialog != null && f.length > 2 && f[2].equals("dialog")) ? dialog.getShell() : shell()); break;
      case "tabs": tabs(f[1], shell()); break;
      case "front": { Shell s = (dialog != null && f.length > 2 && f[2].equals("dialog")) ? dialog.getShell() : shell(); s.forceActive(); break; }
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

  static Shell ctlShell;
  static void ctl() {
    Shell sh = new Shell(Display.getDefault(), SWT.SHELL_TRIM); ctlShell = sh; sh.setText("Control gallery"); sh.setLayout(new GridLayout(3, false));
    new Label(sh, 0).setText("Label"); Text t = new Text(sh, SWT.BORDER); t.setText("Text"); Text tr = new Text(sh, SWT.BORDER | SWT.READ_ONLY); tr.setText("Read only text");
    new Label(sh, 0).setText("Combo"); Combo c1 = new Combo(sh, SWT.DROP_DOWN); c1.setItems("one", "two"); c1.setText("editable"); Combo c2 = new Combo(sh, SWT.DROP_DOWN | SWT.READ_ONLY); c2.setItems("one", "two"); c2.select(0);
    new Label(sh, 0).setText("Combo disabled"); Combo c3 = new Combo(sh, SWT.DROP_DOWN); c3.setText("disabled"); c3.setEnabled(false); Combo c4 = new Combo(sh, SWT.DROP_DOWN | SWT.READ_ONLY); c4.setItems("x"); c4.select(0); c4.setEnabled(false);
    new Label(sh, 0).setText("CCombo"); CCombo cc1 = new CCombo(sh, SWT.BORDER); cc1.setItems(new String[] { "one", "two" }); cc1.setText("editable"); CCombo cc2 = new CCombo(sh, SWT.BORDER | SWT.READ_ONLY); cc2.setItems(new String[] { "one", "two" }); cc2.select(0);
    new Label(sh, 0).setText("CCombo disabled"); CCombo cc3 = new CCombo(sh, SWT.BORDER); cc3.setText("disabled"); cc3.setEnabled(false); new Label(sh, 0);
    new Label(sh, 0).setText("Spinner / List"); Spinner sp = new Spinner(sh, SWT.BORDER); sp.setSelection(5); org.eclipse.swt.widgets.List li = new org.eclipse.swt.widgets.List(sh, SWT.BORDER); li.setItems("list a", "list b", "list c");
    Group g = new Group(sh, 0); g.setText("Group"); g.setLayout(new GridLayout(2, false)); g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
    new Button(g, SWT.PUSH).setText("Push"); Button dis = new Button(g, SWT.PUSH); dis.setText("Disabled"); dis.setEnabled(false);
    Button ck = new Button(g, SWT.CHECK); ck.setText("Check"); ck.setSelection(true); Button ckd = new Button(g, SWT.CHECK); ckd.setText("Check disabled"); ckd.setEnabled(false);
    new Button(g, SWT.RADIO).setText("Radio"); Button rs = new Button(g, SWT.RADIO); rs.setText("Radio selected"); rs.setSelection(true);
    Link lk = new Link(sh, 0); lk.setText("A <a>link</a> in text"); lk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
    CTabFolder cf = new CTabFolder(sh, SWT.BORDER); cf.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1)); for (int i = 0; i < 3; i++) { CTabItem it = new CTabItem(cf, SWT.CLOSE); it.setText("CTab " + i); Label l = new Label(cf, 0); l.setText("body " + i); it.setControl(l); } cf.setSelection(1);
    TabFolder tf = new TabFolder(sh, 0); tf.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1)); for (int i = 0; i < 2; i++) { TabItem it = new TabItem(tf, 0); it.setText("Tab " + i); Label l = new Label(tf, 0); l.setText("native tab body " + i); it.setControl(l); }
    Table tb = new Table(sh, SWT.BORDER | SWT.FULL_SELECTION); tb.setHeaderVisible(true); new TableColumn(tb, 0).setText("Name"); new TableColumn(tb, 0).setText("Value"); tb.getColumn(0).setWidth(120); tb.getColumn(1).setWidth(120);
    for (int i = 0; i < 3; i++) { TableItem ti = new TableItem(tb, 0); ti.setText(new String[] { "row " + i, "v" + i }); } tb.select(1); tb.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    Tree tre = new Tree(sh, SWT.BORDER); for (int i = 0; i < 3; i++) { TreeItem ti = new TreeItem(tre, 0); ti.setText("node " + i); new TreeItem(ti, 0).setText("child"); } tre.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    ToolBar bar = new ToolBar(sh, SWT.FLAT); new ToolItem(bar, SWT.PUSH).setText("Tool"); ToolItem tc = new ToolItem(bar, SWT.CHECK); tc.setText("Toggled"); tc.setSelection(true);
    ProgressBar pb = new ProgressBar(sh, SWT.HORIZONTAL); pb.setSelection(40); Scale sc = new Scale(sh, SWT.HORIZONTAL);
    sh.setSize(640, 760); sh.setLocation(200, 20); sh.open(); sh.forceActive();
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


  static void project() throws Exception {
    var ws = org.eclipse.core.resources.ResourcesPlugin.getWorkspace();
    var proj = ws.getRoot().getProject("Demo");
    if (!proj.exists()) { proj.create(null); }
    if (!proj.isOpen()) proj.open(null);
    var f = proj.getFile("Demo.java");
    if (!f.exists()) f.create(new java.io.ByteArrayInputStream(String.join(System.lineSeparator(), "package demo;", "", "public class Demo {", "	// a comment", "	public static void main(String[] args) {", "		System.out.println(\"hello\");", "	}", "}", "").getBytes(StandardCharsets.UTF_8)), true, null);
    var pg = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    org.eclipse.ui.ide.IDE.openEditor(pg, f);
    log("PROJECT opened Demo.java");
  }

  static double lum(Color c) { return 0.2126 * lin(c.getRed()) + 0.7152 * lin(c.getGreen()) + 0.0722 * lin(c.getBlue()); }
  static double lin(int v) { double c = v / 255.0; return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4); }
  static double contrast(Color a, Color b) { double x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); }

  static void audit(String label, Shell root) {
    Map<String, Integer> counts = new TreeMap<>(); Map<String, Integer> low = new TreeMap<>(); int[] total = new int[1];
    auditWalk(root, counts, low, total);
    log("AUDIT " + label + " shell='" + root.getText() + "' bg=" + rgb(root.getBackground()) + " fg=" + rgb(root.getForeground()) + " controls=" + total[0] + " lowContrastKinds=" + low.size());
    for (var e : low.entrySet()) log("AUDIT-LOW " + label + " " + e.getKey() + " x" + e.getValue());
  }
  static void auditWalk(Control c, Map<String, Integer> counts, Map<String, Integer> low, int[] total) {
    if (c.isDisposed() || !c.isVisible()) return;
    total[0]++;
    boolean textual = c instanceof Label || c instanceof Button || c instanceof Link || c instanceof Text || c instanceof Combo || c instanceof CCombo || c instanceof Tree || c instanceof Table || c instanceof StyledText || c instanceof CLabel || c instanceof Group || c instanceof CTabFolder;
    if (textual) {
      Color bg = c.getBackground(), fg = c.getForeground(); double r = contrast(bg, fg);
      if (r < 3.0) { String k = c.getClass().getName().replace("org.eclipse.swt.", "") + " fg=" + rgb(fg) + " bg=" + rgb(bg) + String.format(" cr=%.2f", r); low.merge(k, 1, Integer::sum); }
    }
    if (c instanceof Composite p) for (Control x : p.getChildren()) auditWalk(x, counts, low, total);
  }

  static void watch() {
    for (String n : new String[] { "org.eclipse.ui.editors", "org.eclipse.jdt.ui", "org.eclipse.ui.workbench", "org.eclipse.e4.ui.css.swt.theme", "org.eclipse.ui.editors.sync" }) {
      IEclipsePreferences node = InstanceScope.INSTANCE.getNode(n);
      node.addPreferenceChangeListener(ev -> {
        StringBuilder who = new StringBuilder();
        for (StackTraceElement e : new Throwable().getStackTrace()) { String c = e.getClassName(); if (c.contains("eclipsethemes") || c.contains("ThemeEngine") || c.contains("CSS") || c.contains("css") || c.contains("Theme")) { who.append(c.substring(c.lastIndexOf('.') + 1)).append('.').append(e.getMethodName()).append(' '); if (who.length() > 160) break; } }
        String k = ev.getKey();
        if (k != null && (k.contains("Color") || k.contains("color") || k.contains("ackground") || k.contains("oreground") || k.equals("themeid") || k.contains("SystemDefault") || k.contains("Selection") || k.contains("currentLine")))
          log("PREFCHG t=" + (System.nanoTime() / 1000000 % 1000000) + " " + n + "/" + k + " " + ev.getOldValue() + " -> " + ev.getNewValue() + " by " + who);
      });
    }
    log("WATCH on");
  }
  static void eprefs(String label) {
    StringBuilder sb = new StringBuilder("EPREFS " + label + " ");
    String[][] keys = { { "org.eclipse.ui.editors", "AbstractTextEditor.Color.Background" }, { "org.eclipse.ui.editors", "AbstractTextEditor.Color.Background.SystemDefault" }, { "org.eclipse.ui.editors", "AbstractTextEditor.Color.Foreground" }, { "org.eclipse.ui.editors", "AbstractTextEditor.Color.Foreground.SystemDefault" }, { "org.eclipse.ui.editors", "currentLineColor" }, { "org.eclipse.jdt.ui", "java_keyword" } };
    for (String[] k : keys) sb.append(k[1].replace("AbstractTextEditor.Color.", "")).append('=').append(InstanceScope.INSTANCE.getNode(k[0]).get(k[1], "<absent>")).append(' ');
    log(sb.toString());
    // observed: first StyledText in the active editor
    try { var ed = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor(); log("EPREFS-editor " + (ed == null ? "none" : ed.getTitle())); } catch (Throwable e) { log("EPREFS err " + e); }
    for (StyledText st : styledTexts(shell())) log("EPREFS-styled bg=" + rgb(st.getBackground()) + " fg=" + rgb(st.getForeground()) + " len=" + st.getCharCount());
  }
  static List<StyledText> styledTexts(Control c) {
    List<StyledText> r = new ArrayList<>(); if (c.isDisposed() || !c.isVisible()) return r;
    if (c instanceof StyledText st) r.add(st);
    if (c instanceof Composite p) for (Control x : p.getChildren()) r.addAll(styledTexts(x));
    return r;
  }
  static void pix(String label, Shell sh) {
    Tree t = findDemo(sh); if (t == null) { log("PIX no tree"); return; }
    Image im = new Image(t.getDisplay(), t.getSize().x, t.getSize().y); GC gc = new GC(im);
    try { t.print(gc); ImageData d = im.getImageData(); Rectangle b = t.getItem(0).getBounds();
      Color bg = t.getBackground(); int bgp = d.getPixel(Math.min(d.width - 1, b.x + b.width + 40), b.y + 2); RGB bgrgb = d.palette.getRGB(bgp);
      double best = -1; RGB far = bgrgb;
      for (int x = b.x + 24; x < Math.min(d.width, b.x + b.width); x++) for (int y = b.y; y < Math.min(d.height, b.y + b.height); y++) {
        RGB r = d.palette.getRGB(d.getPixel(x, y)); double dist = Math.abs(r.red - bgrgb.red) + Math.abs(r.green - bgrgb.green) + Math.abs(r.blue - bgrgb.blue);
        if (dist > best) { best = dist; far = r; } }
      log("PIX " + label + " bgpix=" + String.format("#%02x%02x%02x", bgrgb.red, bgrgb.green, bgrgb.blue) + " textFarthest=" + String.format("#%02x%02x%02x", far.red, far.green, far.blue) + " treeFg=" + rgb(t.getForeground()));
    } finally { gc.dispose(); im.dispose(); }
  }
  static Tree findDemo(Control c) {
    if (c.isDisposed() || !c.isVisible()) return null;
    if (c instanceof Tree t && t.getItemCount() > 0 && "Demo".equals(t.getItem(0).getText()) && t.getItem(0).getImage() != null && t.getParent().getParent() != null) { if (t.getItemCount() == 1) return t; }
    if (c instanceof Composite p) for (Control x : p.getChildren()) { Tree r = findDemo(x); if (r != null) return r; }
    return null;
  }
  static void pexp(int k, Shell sh) {
    Tree t = findDemo(sh); if (t == null) { log("PEXP no tree"); return; }
    StringBuilder sb = new StringBuilder("PEXP k=" + k + " style=" + Integer.toHexString(t.getStyle()) + " cls=" + t.getData("org.eclipse.e4.ui.css.CssClassName") + " id=" + t.getData("org.eclipse.e4.ui.css.id") + " chain=");
    for (Composite p = t.getParent(); p != null; p = p.getParent()) sb.append(p.getClass().getSimpleName()).append(" bg=").append(rgb(p.getBackground())).append(" > ");
    sb.append(" item.fg=").append(rgb(t.getItem(0).getForeground())).append(" font=").append(t.getItem(0).getFont().getFontData()[0]);
    log(sb.toString());
    if (k == 3) { try {
      Object part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView("org.eclipse.jdt.ui.PackageExplorer");
      Object v = part.getClass().getMethod("getTreeViewer").invoke(part);
      Object lp = v.getClass().getMethod("getLabelProvider").invoke(v);
      log("PEXP3 lp=" + lp.getClass().getName());
      Object el = t.getItem(0).getData(); log("PEXP3 element=" + el);
      Object sp = lp.getClass().getMethod("getStyledStringProvider").invoke(lp);
      Object ss = sp.getClass().getMethod("getStyledText", Object.class).invoke(sp, el);
      log("PEXP3 sp=" + sp.getClass().getName() + " ss=" + ss);
      for (Object r : (Object[]) ss.getClass().getMethod("getStyleRanges").invoke(ss)) log("PEXP3 range " + r);
      log("PEXP3 ownerDraw=" + lp.getClass().getMethod("isOwnerDrawEnabled").invoke(lp));
    } catch (Throwable e) { log("PEXP3 err " + e); } }
    if (k == 11) { Listener l = e -> { if (e.widget == t) log("PAINTITEM gcFg=" + rgb(e.gc.getForeground()) + " gcBg=" + rgb(e.gc.getBackground()) + " detail=" + e.detail + " item=" + ((TreeItem) e.item).getText()); };
      t.getDisplay().addFilter(SWT.PaintItem, l); t.redraw(); t.update(); t.getDisplay().removeFilter(SWT.PaintItem, l); }
    if (k == 10) { log("TDUMP lines=" + t.getLinesVisible() + " hdr=" + t.getHeaderVisible() + " fg=" + rgb(t.getForeground()) + " bg=" + rgb(t.getBackground()) + " hdrFg=" + rgb(t.getHeaderForeground()) + " hdrBg=" + rgb(t.getHeaderBackground()) + " enabled=" + t.getEnabled() + " parentMode=" + t.getParent().getBackgroundMode() + " bgImg=" + t.getBackgroundImage() + " itemFont=" + t.getItem(0).getFont().getFontData()[0].getName() + " itemBg=" + rgb(t.getItem(0).getBackground()) + " darkPref=" + Display.isSystemDarkTheme() + " dispDark=" + t.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND)); }
    if (k == 8) { t.getItem(0).setForeground(t.getDisplay().getSystemColor(SWT.COLOR_RED)); t.update(); }
    if (k == 9) { t.getItem(0).setForeground(null); t.update(); }
    if (k == 4) { t.getItem(0).setForeground(t.getDisplay().getSystemColor(SWT.COLOR_WHITE)); t.redraw(); }
    if (k == 5) { log("PEXP5 selection=" + t.getSelectionCount() + " focus=" + t.isFocusControl()); t.deselectAll(); t.redraw(); }
    if (k == 1) { t.setForeground(t.getDisplay().getSystemColor(SWT.COLOR_WHITE)); t.redraw(); }
    if (k == 2) { t.redraw(); t.update(); }
  }
  static void tabs(String label, Control root) {
    if (root.isDisposed() || !root.isVisible()) return;
    if (root instanceof CTabFolder t) {
      StringBuilder sb = new StringBuilder("TABFOLDER " + label + " fg=" + rgb(t.getForeground()) + " bg=" + rgb(t.getBackground()) + " selBg=" + rgb(t.getSelectionBackground()) + " selFg=" + rgb(t.getSelectionForeground()) + " items=");
      for (CTabItem i : t.getItems()) sb.append(i.getText()).append("[fg=").append(i.getForeground() == null ? "-" : rgb(i.getForeground())).append("] ");
      log(sb.toString());
    }
    if (root instanceof Tree tr) { StringBuilder sb = new StringBuilder("TREE " + label + " fg=" + rgb(tr.getForeground()) + " bg=" + rgb(tr.getBackground()) + " items="); int n = 0; for (TreeItem ti : tr.getItems()) { if (n++ > 3) break; sb.append(ti.getText()).append("[fg=").append(rgb(ti.getForeground())).append(" bg=").append(rgb(ti.getBackground())).append("] "); } log(sb.toString()); }
    if (root instanceof Composite p) for (Control x : p.getChildren()) tabs(label, x);
  }

  static void screen(String name, Shell s) {
    s.setVisible(true); s.setMinimized(false); s.forceActive();
    org.eclipse.swt.internal.win32.OS.SetWindowPos(s.handle, org.eclipse.swt.internal.win32.OS.HWND_TOPMOST, 0, 0, 0, 0, org.eclipse.swt.internal.win32.OS.SWP_NOSIZE | org.eclipse.swt.internal.win32.OS.SWP_NOMOVE);
    Display d = s.getDisplay();
    d.timerExec(900, () -> {
      Rectangle b = s.getBounds(); Image im = new Image(d, b.width, b.height); GC gc = new GC(d);
      try { GC ig = new GC(im); boolean ok = org.eclipse.swt.internal.win32.OS.PrintWindow(s.handle, ig.handle, 2); ig.dispose(); if (!ok) gc.copyArea(im, b.x, b.y); ImageLoader l = new ImageLoader(); l.data = new ImageData[] { im.getImageData() };
        l.save(out().resolve(System.getProperty("probe.tag") + "-" + name + ".png").toString(), SWT.IMAGE_PNG); log("SCREEN " + name + " " + b); }
      finally { gc.dispose(); im.dispose(); }
    });
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
      Tree tree = new Tree(parent, SWT.BORDER | SWT.V_SCROLL); tree.setLayoutData(new GridData(250, 90)); for (int k = 0; k < 40; k++) new TreeItem(tree, 0).setText("Probe tree " + k);
      Table table = new Table(parent, SWT.BORDER | SWT.V_SCROLL); table.setLayoutData(new GridData(250, 90)); for (int k = 0; k < 40; k++) new TableItem(table, 0).setText("Probe table " + k);
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
