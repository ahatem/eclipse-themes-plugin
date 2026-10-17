package smoke;

import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import org.eclipse.equinox.app.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.application.*;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.*;
import org.eclipse.jdt.internal.ui.text.java.hover.SourceViewerInformationControl;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.text.XMLConfiguration;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.AntSourceViewerConfiguration;

/** R4_33 consumer checks promoted from the archived PM6 experiment.
 * Internal consumer access is intentionally test-only and version-bounded.
 */
public class EditorConsumers implements IApplication {
  static Throwable failure;
  static org.osgi.framework.Bundle product;
  static void check(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS "+message);}
  static Object field(Object object,String name)throws Exception{
    for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException e){}
    throw new NoSuchFieldException(name);
  }
  static Object theme(String properties)throws Exception{
    Class<?> parser=product.loadClass("com.github.eclipsethemes.core.parser.XmlThemeParser");
    return parser.getMethod("parse",java.io.InputStream.class,java.io.File.class).invoke(parser.getConstructor().newInstance(),
      new java.io.ByteArrayInputStream(("<colorTheme name='Smoke'>"+properties+"</colorTheme>").getBytes(java.nio.charset.StandardCharsets.UTF_8)),null);
  }
  static void apply(String adapter,Object theme,String node)throws Exception{
    Class<?> c=product.loadClass("com.github.eclipsethemes.eclipse.adapters.editor."+adapter);
    c.getMethod("apply",theme.getClass(),IEclipsePreferences.class).invoke(c.getConstructor().newInstance(),theme,InstanceScope.INSTANCE.getNode(node));
  }
  static void pump(Display display){while(display.readAndDispatch()){} }
  public Object start(IApplicationContext context)throws Exception{
    RuntimeState.verify();
    Display display=PlatformUI.createDisplay();
    try{
      PlatformUI.createAndRunWorkbench(display,new WorkbenchAdvisor(){
        public String getInitialWindowPerspectiveId(){return "org.eclipse.ui.resourcePerspective";}
        public boolean openWindows(){return true;}
        public void initialize(IWorkbenchConfigurer c){super.initialize(c);c.setExitOnLastWindowClose(false);c.setSaveAndRestore(false);}
        public void postStartup(){display.timerExec(250,()->{try{probe(display);}catch(Throwable e){failure=e;e.printStackTrace();}finally{display.timerExec(250,()->PlatformUI.getWorkbench().close());}});}
      });
    }finally{display.dispose();System.out.println("R1 DISPLAY DISPOSED");}
    return failure==null ? 0 : 1;
  }
  public void stop(){}
  static void probe(Display display)throws Exception{
    java.util.List<IStatus> errors=Collections.synchronizedList(new ArrayList<>());
    try (Cleanup cleanup = new Cleanup()) {
    product=Platform.getBundle("com.github.eclipsethemes");check(product!=null,"packaged product available");
    System.out.println("RUNTIME "+Platform.getBundle("org.eclipse.platform").getVersion()+" JDT="+Platform.getBundle("org.eclipse.jdt.ui").getVersion()+" Java="+System.getProperty("java.version"));
    ILogListener errorListener=(status,plugin)->{if(status.matches(IStatus.ERROR)||status.getException()!=null)errors.add(status);};
    Platform.addLogListener(errorListener);
    cleanup.add(() -> Platform.removeLogListener(errorListener));
    apply("PluginEditorThemeAdapter",theme("<foreground color='#dedfe0'/>"),"org.eclipse.pde.ui");
    check(InstanceScope.INSTANCE.getNode("org.eclipse.pde.ui").get("editor.color.xml_comment",null).equals("222,223,224"),"default PDE accessor works before probe obtains the plugin store");
    Shell parent=new Shell(display);
    cleanup.add(() -> { if (!parent.isDisposed()) parent.dispose(); }); // Unopened: no visible workbench or dialogs.
    var jdtStore=JavaPlugin.getDefault().getPreferenceStore();
    var editorStore=EditorsPlugin.getDefault().getPreferenceStore();
    var pdeStore=PDEPlugin.getDefault().getPreferenceStore();
    var antStore=AntUIPlugin.getDefault().getPreferenceStore();
    Map<String,List<String>> events=new LinkedHashMap<>();
    for(var entry:Map.of("jdt",jdtStore,"editor",editorStore,"pde",pdeStore,"ant",antStore).entrySet()){
      List<String> list=new ArrayList<>();events.put(entry.getKey(),list);
      IPropertyChangeListener listener=e->list.add(e.getProperty()+"="+e.getNewValue()+" ["+(e.getNewValue()==null?"null":e.getNewValue().getClass().getName())+"]");
      entry.getValue().addPropertyChangeListener(listener);
      cleanup.add(() -> entry.getValue().removePropertyChangeListener(listener));
    }
    String common="<background color='#101112'/><foreground color='#dedfe0'/><selectionBackground color='#334455'/>";
    String optional="<inheritedMethod color='#abc123' bold='true'/><inheritedField color='#abc123' italic='true'/><typeArgument color='#abc123' underline='true'/><deprecated color='#abc123' strikethrough='true'/>";
    Object rich=theme(common+"<debugCurrentInstructionPointer color='#123456'/><debugCallStack color='#654321'/><sourceHoverBackground color='#234567'/>"
      +"<comment color='#987654' italic='true'/><xmlComment color='#456789' bold='true'/>"+optional);
    Object sparse=theme(common);
    IEclipsePreferences editor=InstanceScope.INSTANCE.getNode("org.eclipse.ui.editors");
    editor.putBoolean("currentIPIndication",true);editor.putBoolean("secondaryIPIndication",false);
    editor.putBoolean("currentIPHighlight",true);editor.putBoolean("secondaryIPHighlight",true);
    AnnotationModel model=new AnnotationModel();
    SourceViewer debugViewer=new SourceViewer(parent,null,SWT.NONE);
    debugViewer.setDocument(new Document("one\ntwo\n"),model);
    model.addAnnotation(new Annotation("org.eclipse.debug.ui.currentIP",false,"current"),new Position(0,3));
    model.addAnnotation(new Annotation("org.eclipse.debug.ui.secondaryIP",false,"secondary"),new Position(4,3));
    var decorations=new SourceViewerDecorationSupport(debugViewer,null,new DefaultMarkerAnnotationAccess(),EditorsPlugin.getDefault().getSharedTextColors());
    var lookup=EditorsPlugin.getDefault().getAnnotationPreferenceLookup();
    decorations.setAnnotationPreference(lookup.getAnnotationPreference("org.eclipse.debug.ui.currentIP"));
    decorations.setAnnotationPreference(lookup.getAnnotationPreference("org.eclipse.debug.ui.secondaryIP"));
    cleanup.add(decorations::dispose);
    decorations.install(editorStore);
    apply("TextEditorThemeAdapter",rich,"org.eclipse.ui.editors");pump(display);
    var painter=field(decorations,"fAnnotationPainter");
    Map<?,?> colors=(Map<?,?>)field(painter,"fAnnotationType2Color");
    check(((org.eclipse.swt.graphics.Color)colors.get("org.eclipse.debug.ui.currentIP")).getRGB().equals(new RGB(18,52,86)),"debug current painter listener consumes currentIPColor");
    check(((org.eclipse.swt.graphics.Color)colors.get("org.eclipse.debug.ui.secondaryIP")).getRGB().equals(new RGB(101,67,33)),"debug secondary painter listener consumes secondaryIPColor");
    check(editor.get("currentIPIndication",null).equals("true")&&editor.get("secondaryIPIndication",null).equals("false"),"debug indication switches preserved");

    var javaViewer=new JavaSourceViewer(parent,null,null,false,SWT.NONE,JavaPlugin.getDefault().getCombinedPreferenceStore());
    javaViewer.setDocument(new Document("class Smoke {}"));
    var semantics=new SemanticHighlightingManager();
    cleanup.add(semantics::uninstall);
    semantics.install(javaViewer,JavaPlugin.getDefault().getJavaTextTools().getColorManager(),jdtStore,new SemanticHighlightingManager.HighlightedRange[0][]);
    check(jdtStore.getDefaultBoolean("semanticHighlighting.deprecatedMember.enabled"),"deprecated-member default is enabled");
    for(Object value:List.of(rich,sparse,sparse,rich)){
      boolean present=value==rich;
      apply("JavaEditorThemeAdapter",value,"org.eclipse.jdt.ui");pump(display);
      Object[] roles=(Object[])field(semantics,"fSemanticHighlightings");
      Object[] highlights=(Object[])field(semantics,"fHighlightings");
      for(String key:List.of("inheritedMethodInvocation","inheritedField","typeArgument","deprecatedMember")){
        boolean found=false;
        for(int i=0;i<roles.length;i++){
          Method roleMethod=roles[i].getClass().getMethod("getPreferenceKey"); roleMethod.setAccessible(true); String role=(String)roleMethod.invoke(roles[i]);
          if(role.equals(key)){found=true;Method enabledMethod=highlights[i].getClass().getMethod("isEnabled"); enabledMethod.setAccessible(true); check((boolean)enabledMethod.invoke(highlights[i])==present,"semantic listener "+key+" enabled="+present);}
        }
        check(found,"semantic role found "+key);
        IEclipsePreferences prefs=InstanceScope.INSTANCE.getNode("org.eclipse.jdt.ui");
        check(prefs.get("semanticHighlighting."+key+".enabled",null).equals(Boolean.toString(present)),"stored optional state "+key);
        if(!present)for(String flag:List.of("color","bold","italic","underline","strikethrough"))check(prefs.get("semanticHighlighting."+key+"."+flag,null)==null,"sparse override absent "+key+"."+flag);
      }
      var hover=new SourceViewerInformationControl(parent,false,SWT.NONE,null);
      try {
      var text=(org.eclipse.swt.custom.StyledText)field(hover,"fText");
      if(present)check(text.getBackground().getRGB().equals(new RGB(35,69,103)),"new source hover consumes explicit background");
      else check(jdtStore.getBoolean("sourceHoverBackgroundColor.SystemDefault")==jdtStore.getDefaultBoolean("sourceHoverBackgroundColor.SystemDefault"),"omitted hover exposes system-default preference");
      System.out.println("HOVER present="+present+" actual="+text.getBackground().getRGB());
      } finally { hover.dispose(); }
    }

    SourceViewer pdeViewer=new SourceViewer(parent,null,SWT.NONE),antViewer=new SourceViewer(parent,null,SWT.NONE);
    pdeViewer.setDocument(new Document("<!-- PDE -->"));antViewer.setDocument(new Document("<!-- Ant -->"));
    var pdeColors=new org.eclipse.pde.internal.ui.editor.text.ColorManager();
    cleanup.add(pdeColors::dispose);
    var pde=new XMLConfiguration(pdeColors);pde.getPresentationReconciler(pdeViewer);
    var ant=new AntSourceViewerConfiguration();ant.getPresentationReconciler(antViewer);
    IPropertyChangeListener pdeListener=pde::adaptToPreferenceChange, antListener=ant::adaptToPreferenceChange;
    pdeStore.addPropertyChangeListener(pdeListener); cleanup.add(() -> pdeStore.removePropertyChangeListener(pdeListener));
    antStore.addPropertyChangeListener(antListener); cleanup.add(() -> antStore.removePropertyChangeListener(antListener));
    String fault=System.getProperty("r1.probeFault", "None");
    check(!fault.equals("Assertion"), "assertion failure control after viewer allocation");
    if(fault.equals("Listener")) Platform.getLog(product).error("Injected listener failure", new IllegalStateException("listener failure control"));
    if(fault.equals("Timeout")) Thread.sleep(60000);
    var pdePrefs=InstanceScope.INSTANCE.getNode("org.eclipse.pde.ui");
    var antPrefs=InstanceScope.INSTANCE.getNode("org.eclipse.ant.ui");
    pdeStore.setDefault("editor.color.xml_comment_bold",true);
    pdeStore.setDefault("editor.color.xml_comment_italic",true);
    Object generic=theme(common+"<comment color='#789abc' italic='true'/>");
    Object rich2=theme(common+"<comment color='#123987' bold='true'/><xmlComment color='#abc456' italic='true'/>");
    for(Object value:List.of(rich,generic,sparse,rich2,rich2,rich)){
      apply("PluginEditorThemeAdapter",value,"org.eclipse.pde.ui");
      apply("AntEditorThemeAdapter",value,"org.eclipse.ant.ui");pump(display);
      TextAttribute pdeAttr=(TextAttribute)field(pde,"fXMLCommentAttr");
      TextAttribute antAttr=(TextAttribute)field(ant,"xmlCommentAttribute");
      RGB expected=value==rich?new RGB(69,103,137):value==generic?new RGB(120,154,188):value==rich2?new RGB(171,196,86):new RGB(222,223,224);
      int expectedStyle=value==rich?SWT.BOLD:value==generic||value==rich2?SWT.ITALIC:SWT.NORMAL;
      System.out.println("PDE actual="+pdeAttr.getForeground()+" style="+pdeAttr.getStyle()+" ANT actual="+antAttr.getForeground()+" style="+antAttr.getStyle());
      check(pdeAttr.getForeground()!=null&&pdeAttr.getForeground().getRGB().equals(expected),"PDE comment listener consumes XML role");
      check(antAttr.getForeground()!=null&&antAttr.getForeground().getRGB().equals(expected),"Ant comment listener consumes XML role");
      check(((pdeAttr.getStyle()&SWT.BOLD)!=0)==(value==rich),"PDE comment bold listener transition");
      check(((antAttr.getStyle()&SWT.BOLD)!=0)==(value==rich),"Ant comment bold listener transition");
      check(pdeAttr.getStyle()==expectedStyle&&antAttr.getStyle()==expectedStyle,"PDE/Ant complete supported style and fallback transition");
      check(pdePrefs.get("editor.color.xml_comment_bold",null).equals(Boolean.toString(value==rich)),"default-valued PDE bold remains explicit");
      check(pdePrefs.get("editor.color.xml_comment_italic",null).equals(Boolean.toString(value==generic||value==rich2)),"default-valued PDE italic remains explicit");
    }
    check(events.get("pde").stream().anyMatch(e->e.contains("editor.color.xml_comment_bold=true [java.lang.Boolean]")),"actual PDE store receives typed true");
    check(events.get("pde").stream().anyMatch(e->e.contains("editor.color.xml_comment_bold=false [java.lang.Boolean]")),"actual PDE store receives typed false");
    check(events.get("pde").stream().noneMatch(e->e.contains("_underline=")||e.contains("_strikethrough=")),"PDE emits no unsupported style notifications");
    check(events.get("ant").stream().noneMatch(e->e.contains("_underline=")||e.contains("_strikethrough=")),"Ant emits no unsupported style notifications");
    checkServiceIsolation(rich);
    new TreeMap<>(events).forEach((id, trace) -> trace.forEach(event -> System.out.println("EVENT " + id + " " + event)));
    }
    check(errors.isEmpty(),"zero runtime errors/listener exceptions through service checks and cleanup (no log suppression)");
    if (!System.getProperty("r1.probeFault", "None").equals("MissingCompletion")) System.out.println("R1 CONSUMERS COMPLETE");
  }

  static java.util.Map<String,String> snapshot(IEclipsePreferences prefs)throws Exception{
    var result=new java.util.TreeMap<String,String>();for(var key:prefs.keys())result.put(key,prefs.get(key,null));return result;
  }
  @SuppressWarnings("unchecked") static void checkServiceIsolation(Object theme)throws Exception{
    List<String> failures=new ArrayList<>();
    ILog log=(ILog)Proxy.newProxyInstance(ILog.class.getClassLoader(),new Class<?>[]{ILog.class},(p,m,a)->{if(m.getName().equals("error"))failures.add(a[0]+" "+(a.length>1?a[1]:""));return null;});
    var registryClass=product.loadClass("com.github.eclipsethemes.eclipse.AdapterRegistry");
    var registry=registryClass.getConstructor(ILog.class).newInstance(log);registryClass.getMethod("discover").invoke(registry);
    var adapters=(Map<Object,String>)field(registry,"adapters");
    check(adapters.containsValue("org.eclipse.jdt.ui"),"available JDT adapter discovered");
    Class<?> pdeClass=product.loadClass("com.github.eclipsethemes.eclipse.adapters.editor.PluginEditorThemeAdapter");
    var ctor=pdeClass.getDeclaredConstructor(java.util.function.Supplier.class);ctor.setAccessible(true);
    Object incompatible=ctor.newInstance((java.util.function.Supplier<IPreferenceStore>)()->{throw new NoClassDefFoundError("simulated incompatible PDE");});
    adapters.entrySet().removeIf(e->"org.eclipse.pde.ui".equals(e.getValue()));adapters.put(incompatible,"org.eclipse.pde.ui");
    var prefs=InstanceScope.INSTANCE.getNode("org.eclipse.pde.ui");var before=snapshot(prefs);
    var serviceClass=product.loadClass("com.github.eclipsethemes.eclipse.ThemeService");
    var service=serviceClass.getConstructor(registryClass,ILog.class).newInstance(registry,log);
    var text=InstanceScope.INSTANCE.getNode("org.eclipse.ui.editors");
    text.put("AbstractTextEditor.Color.SelectionBackground","1,2,3");
    check("1,2,3".equals(text.get("AbstractTextEditor.Color.SelectionBackground",null)),"service continuation starts from text selection sentinel 1,2,3");
    serviceClass.getMethod("applyTheme",IWorkbench.class,theme.getClass()).invoke(service,null,theme);
    check(before.equals(snapshot(prefs)),"incompatible PDE accessor causes no PDE mutation");
    check(failures.size()==1&&failures.get(0).contains("PluginEditorThemeAdapter"),"existing service reports incompatible PDE and continues");
    check("51,68,85".equals(text.get("AbstractTextEditor.Color.SelectionBackground",null)),"service changes text selection from sentinel 1,2,3 to 51,68,85 beside incompatible PDE");
  }
}
